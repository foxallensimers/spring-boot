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

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Event published as when a {@link SpringApplication} is starting up and the
 * {@link ApplicationContext} is about to refresh. The bean definitions will be loaded and
 * the {@link Environment} is ready for use at this stage.
 * 
 * @author Dave Syer
 */
public class SpringApplicationBeforeRefreshEvent extends ApplicationEvent {

	private final String[] args;
	private final ConfigurableApplicationContext context;

	/**
	 * @param springApplication the current application
	 * @param context the ApplicationContext about to be refreshed
	 * @param args the argumemts the application is running with
	 */
	public SpringApplicationBeforeRefreshEvent(SpringApplication springApplication,
			ConfigurableApplicationContext context, String[] args) {
		super(springApplication);
		this.context = context;
		this.args = args;
	}

	/**
	 * @return the springApplication
	 */
	public SpringApplication getSpringApplication() {
		return (SpringApplication) getSource();
	}

	/**
	 * @return the args
	 */
	public String[] getArgs() {
		return this.args;
	}

	/**
	 * @return the context
	 */
	public ConfigurableApplicationContext getApplicationContext() {
		return this.context;
	}

}
