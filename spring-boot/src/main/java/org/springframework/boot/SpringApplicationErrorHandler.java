/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * Strategy interface that can be used on a {@link SpringApplicationInitializer} to
 * capture errors in a {@link SpringApplication} after it fails to start up.
 * 
 * @author Dave Syer
 * @see SpringApplicationInitializer
 */
public interface SpringApplicationErrorHandler {

	/**
	 * Handle an application startup error.
	 * @param application the spring application.
	 * @param applicationContext the spring context (if one was created, may be
	 * {@code null})
	 * @param args the run arguments
	 * @param exception an exception thrown during startup (or null if none)
	 */
	void handleError(SpringApplication application,
			ConfigurableApplicationContext applicationContext, String[] args,
			Throwable exception);

}
