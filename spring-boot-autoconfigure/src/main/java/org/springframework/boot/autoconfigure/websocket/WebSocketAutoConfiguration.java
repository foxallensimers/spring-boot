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

package org.springframework.boot.autoconfigure.websocket;

import javax.servlet.Servlet;

import org.apache.catalina.Context;
import org.apache.catalina.deploy.ApplicationListener;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.web.NonEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;

/**
 * Auto configuration for websocket server in embedded Tomcat. If
 * <code>spring-websocket</code> is detected on the classpath then we add a listener that
 * installs the Tomcat Websocket initializer. In a non-embedded container it should
 * already be there.
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass(name = "org.apache.tomcat.websocket.server.WsSci", value = {
		Servlet.class, Tomcat.class, WebSocketHandler.class })
@AutoConfigureBefore(EmbeddedServletContainerAutoConfiguration.class)
public class WebSocketAutoConfiguration {

	private static Log logger = LogFactory.getLog(WebSocketAutoConfiguration.class);

	private static final ApplicationListener WS_APPLICATION_LISTENER = new ApplicationListener(
			"org.apache.tomcat.websocket.server.WsContextListener", false);

	@Bean
	@ConditionalOnMissingBean(name = "websocketContainerCustomizer")
	public EmbeddedServletContainerCustomizer websocketContainerCustomizer() {

		EmbeddedServletContainerCustomizer customizer = new EmbeddedServletContainerCustomizer() {

			@Override
			public void customize(ConfigurableEmbeddedServletContainer container) {
				if (container instanceof NonEmbeddedServletContainerFactory) {
					logger.info("NonEmbeddedServletContainerFactory detected. Websockets support should be native so this normally is not a problem.");
					return;
				}
				if (!(container instanceof TomcatEmbeddedServletContainerFactory)) {
					throw new IllegalStateException(
							"Websockets are currently only supported in Tomcat (found "
									+ container.getClass() + "). ");
				}
				((TomcatEmbeddedServletContainerFactory) container)
						.addContextCustomizers(new TomcatContextCustomizer() {
							@Override
							public void customize(Context context) {
								context.addApplicationListener(WS_APPLICATION_LISTENER);
							}
						});
			}

		};

		return customizer;

	}
}
