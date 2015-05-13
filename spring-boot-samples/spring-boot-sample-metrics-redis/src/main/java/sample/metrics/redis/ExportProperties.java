/*
 * Copyright 2012-2015 the original author or authors.
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

package sample.metrics.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties("redis.metrics.export")
class ExportProperties {

	private String prefix = "spring.metrics";
	private String key = "keys.spring.metrics";

	public String getPrefix() {
		return this.prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getAggregatePrefix() {
		String[] tokens = StringUtils.delimitedListToStringArray(this.prefix, ".");
		if (tokens.length > 1) {
			if (StringUtils.hasText(tokens[1])) {
				// If the prefix has 2 or more non-trivial parts, use the first 1
				return tokens[0];
			}
		}
		return this.prefix;
	}

}