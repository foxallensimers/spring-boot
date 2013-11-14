/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded.jetty;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.util.Assert;

/**
 * {@link EmbeddedServletContainer} that can be used to control an embedded Jetty server.
 * Usually this class should be created using the
 * {@link JettyEmbeddedServletContainerFactory} and not directly.
 * 
 * @author Phillip Webb
 * @see JettyEmbeddedServletContainerFactory
 */
public class JettyEmbeddedServletContainer implements EmbeddedServletContainer {

	private final Server server;
	private boolean autoStart;

	/**
	 * Create a new {@link JettyEmbeddedServletContainer} instance.
	 * @param server the underlying Jetty server
	 */
	public JettyEmbeddedServletContainer(Server server) {
		this(server, true);
	}

	/**
	 * Create a new {@link JettyEmbeddedServletContainer} instance.
	 * @param server the underlying Jetty server
	 */
	public JettyEmbeddedServletContainer(Server server, boolean autoStart) {
		this.autoStart = autoStart;
		Assert.notNull(server, "Jetty Server must not be null");
		this.server = server;
		initialize();
	}

	private synchronized void initialize() {
		try {
			this.server.start();
			// Start the server so the ServletContext is available, but stop the
			// connectors to prevent requests from being handled before the Spring context
			// is ready:
			Connector[] connectors = this.server.getConnectors();
			for (Connector connector : connectors) {
				connector.stop();
			}
		}
		catch (Exception ex) {
			throw new EmbeddedServletContainerException(
					"Unable to start embedded Jetty servlet container", ex);
		}
	}

	@Override
	public void start() throws EmbeddedServletContainerException {
		if (!this.autoStart) {
			return;
		}
		try {
			this.server.start();
			Connector[] connectors = this.server.getConnectors();
			for (Connector connector : connectors) {
				connector.start();
			}
		}
		catch (Exception ex) {
			throw new EmbeddedServletContainerException(
					"Unable to start embedded Jetty servlet container", ex);
		}
	}

	@Override
	public synchronized void stop() {
		try {
			this.server.stop();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		catch (Exception ex) {
			throw new EmbeddedServletContainerException(
					"Unable to stop embedded Jetty servlet container", ex);
		}
	}

	/**
	 * Returns access to the underlying Jetty Server.
	 */
	public Server getServer() {
		return this.server;
	}
}
