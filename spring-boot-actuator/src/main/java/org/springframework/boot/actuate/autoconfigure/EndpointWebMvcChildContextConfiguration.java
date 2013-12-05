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

package org.springframework.boot.actuate.autoconfigure;

import java.util.Map;

import javax.servlet.Filter;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerAdapter;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.properties.ManagementServerProperties;
import org.springframework.boot.actuate.web.ErrorController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Configuration triggered from {@link EndpointWebMvcAutoConfiguration} when a new
 * {@link EmbeddedServletContainer} running on a different port is required.
 * 
 * @see EndpointWebMvcAutoConfiguration
 */
@Configuration
public class EndpointWebMvcChildContextConfiguration {

	@Configuration
	protected static class ServerCustomization implements
			EmbeddedServletContainerCustomizer {

		@Value("${error.path:/error}")
		private String errorPath = "/error";

		@Autowired
		private ListableBeanFactory beanFactory;

		// This needs to be lazily initialized because EmbeddedServletContainerCustomizer
		// instances get their callback very early in the context lifecycle.
		private ManagementServerProperties managementServerProperties;

		@Override
		public void customize(ConfigurableEmbeddedServletContainerFactory factory) {
			if (this.managementServerProperties == null) {
				this.managementServerProperties = BeanFactoryUtils
						.beanOfTypeIncludingAncestors(this.beanFactory,
								ManagementServerProperties.class);
			}
			factory.setPort(this.managementServerProperties.getPort());
			factory.setAddress(this.managementServerProperties.getAddress());
			factory.setContextPath(this.managementServerProperties.getContextPath());
			factory.addErrorPages(new ErrorPage(this.errorPath));
		}

	}

	@Bean
	public DispatcherServlet dispatcherServlet() {
		DispatcherServlet dispatcherServlet = new DispatcherServlet();

		// Ensure the parent configuration does not leak down to us
		dispatcherServlet.setDetectAllHandlerAdapters(false);
		dispatcherServlet.setDetectAllHandlerExceptionResolvers(false);
		dispatcherServlet.setDetectAllHandlerMappings(false);
		dispatcherServlet.setDetectAllViewResolvers(false);

		return dispatcherServlet;
	}

	@Bean
	public HandlerMapping handlerMapping() {
		return new EndpointHandlerMapping();
	}

	@Bean
	public HandlerAdapter handlerAdapter() {
		return new EndpointHandlerAdapter();
	}

	/*
	 * The error controller is present but not mapped as an endpoint in this context
	 * because of the DispatcherServlet having had it's HandlerMapping explicitly
	 * disabled. So this tiny shim exposes the same feature but only for machine
	 * endpoints.
	 */
	@Bean
	public Endpoint<Map<String, Object>> errorEndpoint(final ErrorController controller) {
		return new AbstractEndpoint<Map<String, Object>>("/error", false, true) {
			@Override
			protected Map<String, Object> doInvoke() {
				RequestAttributes attributes = RequestContextHolder
						.currentRequestAttributes();
				return controller.extract(attributes, false);
			}
		};
	}

	@Configuration
	@ConditionalOnClass({ EnableWebSecurity.class, Filter.class })
	@ConditionalOnBean(name = "springSecurityFilterChain", search = SearchStrategy.PARENTS)
	public static class EndpointWebMvcChildContextSecurityConfiguration {

		@Bean
		public Filter springSecurityFilterChain(HierarchicalBeanFactory beanFactory) {
			BeanFactory parent = beanFactory.getParentBeanFactory();
			return parent.getBean("springSecurityFilterChain", Filter.class);
		}

	}

}
