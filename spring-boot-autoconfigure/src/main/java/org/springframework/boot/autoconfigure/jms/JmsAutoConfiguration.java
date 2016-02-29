/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms;

import javax.jms.ConnectionFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring JMS.
 *
 * @author Greg Turnquist
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnClass(JmsTemplate.class)
@ConditionalOnBean(ConnectionFactory.class)
@EnableConfigurationProperties(JmsProperties.class)
@Import(JmsAnnotationDrivenConfiguration.class)
public class JmsAutoConfiguration {

	@Autowired
	private JmsProperties properties;

	@Autowired
	private ConnectionFactory connectionFactory;

	@Autowired
	private ObjectProvider<DestinationResolver> destinationResolver;

	@Autowired
	private ObjectProvider<MessageConverter> messageConverter;

	@Bean
	@ConditionalOnMissingBean
	public JmsTemplate jmsTemplate() {
		JmsTemplate jmsTemplate = new JmsTemplate(this.connectionFactory);
		jmsTemplate.setPubSubDomain(this.properties.isPubSubDomain());
		if (this.destinationResolver != null) {
			jmsTemplate.setDestinationResolver(this.destinationResolver.getIfUnique());
		}
		if (this.messageConverter != null) {
			jmsTemplate.setMessageConverter(this.messageConverter.getIfUnique());
		}
		return jmsTemplate;
	}

	@ConditionalOnClass(JmsMessagingTemplate.class)
	@ConditionalOnMissingBean(JmsMessagingTemplate.class)
	protected static class MessagingTemplateConfiguration {

		@Bean
		public JmsMessagingTemplate jmsMessagingTemplate(JmsTemplate jmsTemplate) {
			return new JmsMessagingTemplate(jmsTemplate);
		}

	}

}
