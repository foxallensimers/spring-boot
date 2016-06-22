/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.loader.jar;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.loader.TestJarCreator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JarURLConnection}.
 *
 * @author Andy Wilkinson
 */
public class JarURLConnectionTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder(new File("target"));

	private File rootJarFile;

	private JarFile jarFile;

	@Before
	public void setup() throws Exception {
		this.rootJarFile = this.temporaryFolder.newFile();
		TestJarCreator.createTestJar(this.rootJarFile);
		this.jarFile = new JarFile(this.rootJarFile);
	}

	@Test
	public void connectionToRootUsingAbsoluteUrl() throws Exception {
		URL absoluteUrl = new URL("jar:file:" + getAbsolutePath() + "!/");
		assertThat(new JarURLConnection(absoluteUrl, this.jarFile).getContent())
				.isSameAs(this.jarFile);
	}

	@Test
	public void connectionToRootUsingRelativeUrl() throws Exception {
		URL relativeUrl = new URL("jar:file:" + getRelativePath() + "!/");
		assertThat(new JarURLConnection(relativeUrl, this.jarFile).getContent())
				.isSameAs(this.jarFile);
	}

	@Test
	public void connectionToEntryUsingAbsoluteUrl() throws Exception {
		URL absoluteUrl = new URL("jar:file:" + getAbsolutePath() + "!/1.dat");
		assertThat(new JarURLConnection(absoluteUrl, this.jarFile).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 1 }));
	}

	@Test
	public void connectionToEntryUsingRelativeUrl() throws Exception {
		URL relativeUrl = new URL("jar:file:" + getRelativePath() + "!/1.dat");
		assertThat(new JarURLConnection(relativeUrl, this.jarFile).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 1 }));
	}

	@Test
	public void connectionToEntryUsingAbsoluteUrlWithFileColonSlashSlashPrefix()
			throws Exception {
		URL absoluteUrl = new URL("jar:file:/" + getAbsolutePath() + "!/1.dat");
		assertThat(new JarURLConnection(absoluteUrl, this.jarFile).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 1 }));
	}

	@Test
	public void connectionToEntryUsingAbsoluteUrlForNestedEntry() throws Exception {
		URL absoluteUrl = new URL(
				"jar:file:" + getAbsolutePath() + "!/nested.jar!/3.dat");
		assertThat(new JarURLConnection(absoluteUrl, this.jarFile).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
	}

	@Test
	public void connectionToEntryUsingRelativeUrlForNestedEntry() throws Exception {
		URL relativeUrl = new URL(
				"jar:file:" + getRelativePath() + "!/nested.jar!/3.dat");
		assertThat(new JarURLConnection(relativeUrl, this.jarFile).getInputStream())
				.hasSameContentAs(new ByteArrayInputStream(new byte[] { 3 }));
	}

	private String getAbsolutePath() {
		return this.rootJarFile.getAbsolutePath().replace('\\', '/');
	}

	private String getRelativePath() {
		return this.rootJarFile.getPath().replace('\\', '/');
	}

}
