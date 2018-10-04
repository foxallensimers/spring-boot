/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.testkit;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.jetbrains.kotlin.cli.common.PropertiesKt;
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunner;
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin;
import org.jetbrains.kotlin.gradle.plugin.KotlinPlugin;
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.xml.sax.InputSource;

import org.springframework.asm.ClassVisitor;
import org.springframework.boot.loader.tools.LaunchScript;
import org.springframework.util.FileCopyUtils;

/**
 * A {@link TestRule} for running a Gradle build using {@link GradleRunner}.
 *
 * @author Andy Wilkinson
 */
public class GradleBuild implements TestRule {

	private static final Pattern GRADLE_VERSION_PATTERN = Pattern
			.compile("\\[Gradle .+\\]");

	private final TemporaryFolder temp = new TemporaryFolder();

	private File projectDir;

	private String script;

	private String gradleVersion;

	@Override
	public Statement apply(Statement base, Description description) {
		URL scriptUrl = findDefaultScript(description);
		if (scriptUrl != null) {
			script(scriptUrl.getFile());
		}
		return this.temp.apply(new Statement() {

			@Override
			public void evaluate() throws Throwable {
				before();
				try {
					base.evaluate();
				}
				finally {
					after();
				}
			}

		}, description);
	}

	private URL findDefaultScript(Description description) {
		URL scriptUrl = getScriptForTestMethod(description);
		if (scriptUrl != null) {
			return scriptUrl;
		}
		return getScriptForTestClass(description.getTestClass());
	}

	private URL getScriptForTestMethod(Description description) {
		String name = description.getTestClass().getSimpleName() + "-"
				+ removeGradleVersion(description.getMethodName()) + ".gradle";
		return description.getTestClass().getResource(name);
	}

	private String removeGradleVersion(String methodName) {
		return GRADLE_VERSION_PATTERN.matcher(methodName).replaceAll("").trim();
	}

	private URL getScriptForTestClass(Class<?> testClass) {
		return testClass.getResource(testClass.getSimpleName() + ".gradle");
	}

	private void before() throws IOException {
		this.projectDir = this.temp.newFolder();
	}

	private void after() {
		GradleBuild.this.script = null;
	}

	private List<File> pluginClasspath() {
		return Arrays.asList(new File("bin"), new File("build/classes/java/main"),
				new File("build/resources/main"),
				new File(pathOfJarContaining(LaunchScript.class)),
				new File(pathOfJarContaining(ClassVisitor.class)),
				new File(pathOfJarContaining(DependencyManagementPlugin.class)),
				new File(pathOfJarContaining(KotlinPluginWrapper.class)),
				new File(pathOfJarContaining(PropertiesKt.class)),
				new File(pathOfJarContaining(KotlinCompilerRunner.class)),
				new File(pathOfJarContaining(KotlinPlugin.class)),
				new File(pathOfJarContaining(KotlinGradleSubplugin.class)),
				new File(pathOfJarContaining(ArchiveEntry.class)));
	}

	private String pathOfJarContaining(Class<?> type) {
		return type.getProtectionDomain().getCodeSource().getLocation().getPath();
	}

	public GradleBuild script(String script) {
		this.script = script;
		return this;
	}

	public BuildResult build(String... arguments) {
		try {
			return prepareRunner(arguments).build();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public BuildResult buildAndFail(String... arguments) {
		try {
			return prepareRunner(arguments).buildAndFail();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public GradleRunner prepareRunner(String... arguments) throws IOException {
		String scriptContent = FileCopyUtils.copyToString(new FileReader(this.script))
				.replace("{version}", getBootVersion());
		FileCopyUtils.copy(scriptContent,
				new FileWriter(new File(this.projectDir, "build.gradle")));
		GradleRunner gradleRunner = GradleRunner.create().withProjectDir(this.projectDir)
				.withDebug(true).withPluginClasspath(pluginClasspath());

		if (this.gradleVersion != null) {
			gradleRunner.withGradleVersion(this.gradleVersion);
		}
		List<String> allArguments = new ArrayList<>();
		allArguments.add("-PbootVersion=" + getBootVersion());
		allArguments.add("--stacktrace");
		allArguments.addAll(Arrays.asList(arguments));
		return gradleRunner.withArguments(allArguments);
	}

	public File getProjectDir() {
		return this.projectDir;
	}

	public void setProjectDir(File projectDir) {
		this.projectDir = projectDir;
	}

	public GradleBuild gradleVersion(String version) {
		this.gradleVersion = version;
		return this;
	}

	public String getGradleVersion() {
		return this.gradleVersion;
	}

	private static String getBootVersion() {
		return evaluateExpression(
				"/*[local-name()='project']/*[local-name()='parent']/*[local-name()='version']"
						+ "/text()");
	}

	private static String evaluateExpression(String expression) {
		try {
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xpath = xPathFactory.newXPath();
			XPathExpression expr = xpath.compile(expression);
			String version = expr
					.evaluate(new InputSource(new FileReader(".flattened-pom.xml")));
			return version;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to evaluate expression", ex);
		}
	}

}
