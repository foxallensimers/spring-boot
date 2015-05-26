/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.authserver.SpringSecurityOAuth2AuthorizationServerConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.SpringSecurityOAuth2ClientConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.SpringSecurityOAuth2ResourceServerConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfiguration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Spring Security OAuth2 top level auto-configuration beans
 *
 * @author Greg Turnquist
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ OAuth2AccessToken.class, WebMvcConfigurerAdapter.class })
@ConditionalOnWebApplication
@Import({ SpringSecurityOAuth2AuthorizationServerConfiguration.class,
		SpringSecurityOAuth2MethodSecurityConfiguration.class,
		SpringSecurityOAuth2ResourceServerConfiguration.class,
		SpringSecurityOAuth2ClientConfiguration.class })
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties(ClientCredentialsProperties.class)
public class SpringSecurityOAuth2AutoConfiguration {

	@Configuration
	protected static class ResourceServerOrderProcessor implements BeanPostProcessor {

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof ResourceServerConfiguration) {
				ResourceServerConfiguration configuration = (ResourceServerConfiguration) bean;
				configuration.setOrder(getOrder());
			}
			return bean;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}

		private int getOrder() {
			// Before the authorization server (default 0)
			return -10;
		}

	}

}
