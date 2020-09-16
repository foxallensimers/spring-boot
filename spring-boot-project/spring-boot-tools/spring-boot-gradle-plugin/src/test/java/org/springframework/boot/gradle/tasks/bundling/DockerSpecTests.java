/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.tasks.bundling;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerRegistryAuthentication;
import org.springframework.util.Base64Utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link DockerSpec}.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 */
public class DockerSpecTests {

	@Test
	void asDockerConfigurationWithoutRegistry() {
		DockerSpec dockerSpec = new DockerSpec();
		assertThat(dockerSpec.asDockerConfiguration()).isNull();
	}

	@Test
	void asDockerConfigurationWithEmptyRegistry() {
		DockerSpec dockerSpec = new DockerSpec(new DockerSpec.DockerRegistrySpec());
		assertThat(dockerSpec.asDockerConfiguration()).isNull();
	}

	@Test
	void asDockerConfigurationWithUserAuth() {
		DockerSpec.DockerRegistrySpec dockerRegistry = new DockerSpec.DockerRegistrySpec();
		dockerRegistry.setUsername("user");
		dockerRegistry.setPassword("secret");
		dockerRegistry.setUrl("https://docker.example.com");
		dockerRegistry.setEmail("docker@example.com");
		DockerSpec dockerSpec = new DockerSpec(dockerRegistry);
		DockerConfiguration dockerConfiguration = dockerSpec.asDockerConfiguration();
		DockerRegistryAuthentication registryAuthentication = dockerConfiguration.getRegistryAuthentication();
		assertThat(registryAuthentication).isNotNull();
		assertThat(new String(Base64Utils.decodeFromString(registryAuthentication.createAuthHeader())))
				.contains("\"username\" : \"user\"").contains("\"password\" : \"secret\"")
				.contains("\"email\" : \"docker@example.com\"")
				.contains("\"serveraddress\" : \"https://docker.example.com\"");
	}

	@Test
	void asDockerConfigurationWithIncompleteUserAuthFails() {
		DockerSpec.DockerRegistrySpec dockerRegistry = new DockerSpec.DockerRegistrySpec();
		dockerRegistry.setUsername("user");
		dockerRegistry.setUrl("https://docker.example.com");
		dockerRegistry.setEmail("docker@example.com");
		DockerSpec dockerSpec = new DockerSpec(dockerRegistry);
		assertThatExceptionOfType(GradleException.class).isThrownBy(dockerSpec::asDockerConfiguration)
				.withMessageContaining("Invalid Docker registry configuration");
	}

	@Test
	void asDockerConfigurationWithTokenAuth() {
		DockerSpec.DockerRegistrySpec dockerRegistry = new DockerSpec.DockerRegistrySpec();
		dockerRegistry.setToken("token");
		DockerSpec dockerSpec = new DockerSpec(dockerRegistry);
		DockerConfiguration dockerConfiguration = dockerSpec.asDockerConfiguration();
		DockerRegistryAuthentication registryAuthentication = dockerConfiguration.getRegistryAuthentication();
		assertThat(registryAuthentication).isNotNull();
		assertThat(new String(Base64Utils.decodeFromString(registryAuthentication.createAuthHeader())))
				.contains("\"identitytoken\" : \"token\"");
	}

	@Test
	void asDockerConfigurationWithUserAndTokenAuthFails() {
		DockerSpec.DockerRegistrySpec dockerRegistry = new DockerSpec.DockerRegistrySpec();
		dockerRegistry.setUsername("user");
		dockerRegistry.setPassword("secret");
		dockerRegistry.setToken("token");
		DockerSpec dockerSpec = new DockerSpec(dockerRegistry);
		assertThatExceptionOfType(GradleException.class).isThrownBy(dockerSpec::asDockerConfiguration)
				.withMessageContaining("Invalid Docker registry configuration");
	}

}
