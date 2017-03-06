/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.webflux;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.server.session.WebSessionManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HttpHandler}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass({ DispatcherHandler.class, HttpHandler.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnMissingBean(HttpHandler.class)
@AutoConfigureAfter({ WebFluxAnnotationAutoConfiguration.class })
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpHandlerAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(RouterFunction.class)
	public static class AnnotationConfig {

		private ApplicationContext applicationContext;

		public AnnotationConfig(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Bean
		public HttpHandler httpHandler() {
			return WebHttpHandlerBuilder.applicationContext(this.applicationContext)
					.build();
		}

	}

	@Configuration
	@ConditionalOnBean(RouterFunction.class)
	public static class FunctionalConfig {

		private final List<WebFilter> webFilters;

		private final WebSessionManager webSessionManager;

		private final List<HttpMessageReader> messageReaders;

		private final List<HttpMessageWriter> messageWriters;

		private final List<ViewResolver> viewResolvers;

		public FunctionalConfig(ObjectProvider<List<WebFilter>> webFilters,
				ObjectProvider<WebSessionManager> webSessionManager,
				ObjectProvider<List<HttpMessageReader>> messageReaders,
				ObjectProvider<List<HttpMessageWriter>> messageWriters,
				ObjectProvider<List<ViewResolver>> viewResolvers) {
			this.webFilters = webFilters.getIfAvailable();
			if (this.webFilters != null) {
				AnnotationAwareOrderComparator.sort(this.webFilters);
			}
			this.webSessionManager = webSessionManager.getIfAvailable();
			this.messageReaders = messageReaders.getIfAvailable();
			this.messageWriters = messageWriters.getIfAvailable();
			this.viewResolvers = viewResolvers.getIfAvailable();
		}

		@Bean
		public HttpHandler httpHandler(List<RouterFunction> routerFunctions) {
			routerFunctions.sort(new AnnotationAwareOrderComparator());
			RouterFunction routerFunction = routerFunctions.stream()
					.reduce(RouterFunction::and).get();
			HandlerStrategies.Builder strategiesBuilder = HandlerStrategies.builder();
			if (this.messageReaders != null) {
				this.messageReaders.forEach(strategiesBuilder::messageReader);
			}
			if (this.messageWriters != null) {
				this.messageWriters.forEach(strategiesBuilder::messageWriter);
			}
			if (this.viewResolvers != null) {
				this.viewResolvers.forEach(strategiesBuilder::viewResolver);
			}
			WebHandler webHandler = RouterFunctions.toHttpHandler(routerFunction,
					strategiesBuilder.build());
			WebHttpHandlerBuilder builder = WebHttpHandlerBuilder.webHandler(webHandler)
					.sessionManager(this.webSessionManager);
			if (this.webFilters != null) {
				builder.filters(
						this.webFilters.toArray(new WebFilter[this.webFilters.size()]));
			}
			return builder.build();
		}

	}

}
