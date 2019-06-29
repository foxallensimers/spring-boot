/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link PropertySourcesPlaceholderConfigurer}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 1.0.0
 * @deprecated as of 1.5 in favor of
 * {@link org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration}
 */
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Deprecated
@Import(PropertyPlaceholderAutoConfiguration.Selector.class)
public class PropertyPlaceholderAutoConfiguration {

	private static final String[] REPLACEMENT = {
			"org.springframework.boot.autoconfigure.context." + "PropertyPlaceholderAutoConfiguration" };

	static class Selector implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return REPLACEMENT;
		}

	}

}
