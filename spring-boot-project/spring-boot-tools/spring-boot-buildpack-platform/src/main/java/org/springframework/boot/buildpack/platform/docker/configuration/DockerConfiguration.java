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

package org.springframework.boot.buildpack.platform.docker.configuration;

import org.springframework.util.Assert;

/**
 * Docker configuration options.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 * @since 2.4.0
 */
public final class DockerConfiguration {

	private final DockerRegistryAuthentication authentication;

	private DockerConfiguration(DockerRegistryAuthentication authentication) {
		this.authentication = authentication;
	}

	public DockerRegistryAuthentication getRegistryAuthentication() {
		return this.authentication;
	}

	public static DockerConfiguration withDefaults() {
		return new DockerConfiguration(null);
	}

	public static DockerConfiguration withRegistryTokenAuthentication(String token) {
		Assert.notNull(token, "Token must not be null");
		return new DockerConfiguration(new DockerRegistryTokenAuthentication(token));
	}

	public static DockerConfiguration withRegistryUserAuthentication(String username, String password, String url,
			String email) {
		Assert.notNull(username, "Username must not be null");
		Assert.notNull(password, "Password must not be null");
		return new DockerConfiguration(new DockerRegistryUserAuthentication(username, password, url, email));
	}

}
