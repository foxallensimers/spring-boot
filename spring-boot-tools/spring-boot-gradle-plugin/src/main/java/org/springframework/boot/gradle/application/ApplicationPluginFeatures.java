/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.gradle.application;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.concurrent.Callable;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.distribution.Distribution;
import org.gradle.api.distribution.DistributionContainer;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.ApplicationPluginConvention;
import org.gradle.jvm.application.scripts.TemplateBasedScriptGenerator;

import org.springframework.boot.gradle.PluginFeatures;

/**
 * Features that are configured when the application plugin is applied.
 *
 * @author Andy Wilkinson
 */
public class ApplicationPluginFeatures implements PluginFeatures {

	@Override
	public void apply(Project project) {
		project.getPlugins().withType(ApplicationPlugin.class,
				(plugin) -> configureDistribution(project));
	}

	public void configureDistribution(Project project) {
		ApplicationPluginConvention applicationConvention = project.getConvention()
				.getPlugin(ApplicationPluginConvention.class);
		DistributionContainer distributions = project.getExtensions()
				.getByType(DistributionContainer.class);
		Distribution distribution = distributions.create("boot");
		CreateBootStartScripts bootStartScripts = project.getTasks()
				.create("bootStartScripts", CreateBootStartScripts.class);
		((TemplateBasedScriptGenerator) bootStartScripts.getUnixStartScriptGenerator())
				.setTemplate(project.getResources().getText()
						.fromString(loadResource("/unixStartScript.txt")));
		((TemplateBasedScriptGenerator) bootStartScripts.getWindowsStartScriptGenerator())
				.setTemplate(project.getResources().getText()
						.fromString(loadResource("/windowsStartScript.txt")));
		project.getConfigurations().all((configuration) -> {
			if ("bootArchives".equals(configuration.getName())) {
				distribution.getContents().with(project.copySpec().into("lib")
						.from((Callable<FileCollection>) () -> {
					return configuration.getArtifacts().getFiles();
				}));
				bootStartScripts.setClasspath(configuration.getArtifacts().getFiles());
			}
		});
		bootStartScripts.getConventionMapping().map("outputDir",
				() -> new File(project.getBuildDir(), "bootScripts"));
		bootStartScripts.getConventionMapping().map("applicationName",
				() -> applicationConvention.getApplicationName());
		CopySpec binCopySpec = project.copySpec().into("bin").from(bootStartScripts);
		binCopySpec.setFileMode(0755);
		distribution.getContents().with(binCopySpec);
	}

	private String loadResource(String name) {
		InputStreamReader reader = new InputStreamReader(
				getClass().getResourceAsStream(name));
		char[] buffer = new char[4096];
		int read = 0;
		StringWriter writer = new StringWriter();
		try {
			while ((read = reader.read(buffer)) > 0) {
				writer.write(buffer, 0, read);
			}
			return writer.toString();
		}
		catch (IOException ex) {
			throw new GradleException("Failed to read '" + name + "'", ex);
		}
		finally {
			try {
				reader.close();
			}
			catch (IOException ex) {
				// Continue
			}
		}
	}

}
