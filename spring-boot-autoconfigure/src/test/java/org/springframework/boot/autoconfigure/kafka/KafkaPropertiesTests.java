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

package org.springframework.boot.autoconfigure.kafka;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer.AckMode;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Gary Russell
 * @since 1.5
 */
public class KafkaPropertiesTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void load() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(KafkaAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(context,
				"spring.kafka.bootstrap-servers=foo:1234",
				"spring.kafka.clientId=cid",
				"spring.kafka.ssl.key-password=p1",
				"spring.kafka.ssl.keystore-location=classpath:ksLoc",
				"spring.kafka.ssl.keystore-password=p2",
				"spring.kafka.ssl.truststore-location=classpath:tsLoc",
				"spring.kafka.ssl.truststore-password=p3",

				"spring.kafka.consumer.auto-commit-interval-ms=123",
				"spring.kafka.consumer.auto-offset-reset=earliest",
				"spring.kafka.consumer.client-id=ccid", // test override common
				"spring.kafka.consumer.enable-auto-commit=false",
				"spring.kafka.consumer.fetch-max-wait-ms=456",
				"spring.kafka.consumer.fetch-min-bytes=789",
				"spring.kafka.consumer.group-id=bar",
				"spring.kafka.consumer.heartbeat-interval-ms=234",
				"spring.kafka.consumer.key-deserializer = org.apache.kafka.common.serialization.LongDeserializer",
				"spring.kafka.consumer.value-deserializer = org.apache.kafka.common.serialization.IntegerDeserializer",

				"spring.kafka.producer.acks=all",
				"spring.kafka.producer.batch-size=20",
				"spring.kafka.producer.bootstrap-servers=bar:1234", // test override common
				"spring.kafka.producer.buffer-memory=12345",
				"spring.kafka.producer.compression-type=gzip",
				"spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.LongSerializer",
				"spring.kafka.producer.retries=2",
				"spring.kafka.producer.ssl.key-password=p4",
				"spring.kafka.producer.ssl.keystore-location=classpath:ksLocP",
				"spring.kafka.producer.ssl.keystore-password=p5",
				"spring.kafka.producer.ssl.truststore-location=classpath:tsLocP",
				"spring.kafka.producer.ssl.truststore-password=p6",
				"spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.IntegerSerializer",

				"spring.kafka.template.default-topic=testTopic",

				"spring.kafka.listener.ack-mode=MANUAL",
				"spring.kafka.listener.ack-count=123",
				"spring.kafka.listener.ack-time=456",
				"spring.kafka.listener.concurrency=3",
				"spring.kafka.listener.poll-timeout=2000"
				);
		this.context.refresh();
	}

	@Test
	public void testConsumerProps() {
		DefaultKafkaConsumerFactory<?, ?> consumerFactory = this.context.getBean(DefaultKafkaConsumerFactory.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> consumerProps = (Map<String, Object>) new DirectFieldAccessor(consumerFactory)
				.getPropertyValue("configs");
		// common
		assertThat(consumerProps.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG))
			.isEqualTo(Arrays.asList(new String[] { "foo:1234" }));
		assertThat(consumerProps.get(SslConfigs.SSL_KEY_PASSWORD_CONFIG)).isEqualTo("p1");
		assertThat((String) consumerProps.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG))
			.endsWith(File.separator + "ksLoc");
		assertThat(consumerProps.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG)).isEqualTo("p2");
		assertThat((String) consumerProps.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
			.endsWith(File.separator + "tsLoc");
		assertThat(consumerProps.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG)).isEqualTo("p3");
		// consumer
		assertThat(consumerProps.get(ConsumerConfig.CLIENT_ID_CONFIG)).isEqualTo("ccid"); // override
		assertThat(consumerProps.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG)).isEqualTo(Boolean.FALSE);
		assertThat(consumerProps.get(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG)).isEqualTo(123L);
		assertThat(consumerProps.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)).isEqualTo("earliest");
		assertThat(consumerProps.get(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG)).isEqualTo(456);
		assertThat(consumerProps.get(ConsumerConfig.FETCH_MIN_BYTES_CONFIG)).isEqualTo(789);
		assertThat(consumerProps.get(ConsumerConfig.GROUP_ID_CONFIG)).isEqualTo("bar");
		assertThat(consumerProps.get(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG)).isEqualTo(234);
		assertThat(consumerProps.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG)).isEqualTo(LongDeserializer.class);
		assertThat(consumerProps.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG))
			.isEqualTo(IntegerDeserializer.class);
	}

	@Test
	public void testProducerProps() {
		DefaultKafkaProducerFactory<?, ?> producerFactory = this.context.getBean(DefaultKafkaProducerFactory.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> producerProps = (Map<String, Object>) new DirectFieldAccessor(producerFactory)
				.getPropertyValue("configs");
		// common
		assertThat(producerProps.get(ProducerConfig.CLIENT_ID_CONFIG)).isEqualTo("cid");
		// producer
		assertThat(producerProps.get(ProducerConfig.ACKS_CONFIG)).isEqualTo("all");
		assertThat(producerProps.get(ProducerConfig.BATCH_SIZE_CONFIG)).isEqualTo(20);
		assertThat(producerProps.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))
			.isEqualTo(Arrays.asList(new String[] { "bar:1234" })); // override
		assertThat(producerProps.get(ProducerConfig.BUFFER_MEMORY_CONFIG)).isEqualTo(12345L);
		assertThat(producerProps.get(ProducerConfig.COMPRESSION_TYPE_CONFIG)).isEqualTo("gzip");
		assertThat(producerProps.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)).isEqualTo(LongSerializer.class);
		assertThat(producerProps.get(SslConfigs.SSL_KEY_PASSWORD_CONFIG)).isEqualTo("p4");
		assertThat((String) producerProps.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG))
			.endsWith(File.separator + "ksLocP");
		assertThat(producerProps.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG)).isEqualTo("p5");
		assertThat((String) producerProps.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
			.endsWith(File.separator + "tsLocP");
		assertThat(producerProps.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG)).isEqualTo("p6");
		assertThat(producerProps.get(ProducerConfig.RETRIES_CONFIG)).isEqualTo(2);
		assertThat(producerProps.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)).isEqualTo(IntegerSerializer.class);
	}

	@Test
	public void testInjected() {
		DefaultKafkaProducerFactory<?, ?> producerFactory = this.context.getBean(DefaultKafkaProducerFactory.class);
		DefaultKafkaConsumerFactory<?, ?> consumerFactory = this.context.getBean(DefaultKafkaConsumerFactory.class);
		KafkaTemplate<?, ?> kafkaTemplate = this.context.getBean(KafkaTemplate.class);
		KafkaListenerContainerFactory<?> kafkaListenerContainerFactory = this.context
				.getBean(KafkaListenerContainerFactory.class);
		assertThat(new DirectFieldAccessor(kafkaTemplate).getPropertyValue("producerFactory"))
			.isEqualTo(producerFactory);
		assertThat(kafkaTemplate.getDefaultTopic()).isEqualTo("testTopic");
		DirectFieldAccessor factoryAccessor = new DirectFieldAccessor(kafkaListenerContainerFactory);
		assertThat(factoryAccessor.getPropertyValue("consumerFactory")).isEqualTo(consumerFactory);
		assertThat(factoryAccessor.getPropertyValue("containerProperties.ackMode")).isEqualTo(AckMode.MANUAL);
		assertThat(factoryAccessor.getPropertyValue("containerProperties.ackCount")).isEqualTo(123);
		assertThat(factoryAccessor.getPropertyValue("containerProperties.ackTime")).isEqualTo(456L);
		assertThat(factoryAccessor.getPropertyValue("concurrency")).isEqualTo(3);
		assertThat(factoryAccessor.getPropertyValue("containerProperties.pollTimeout")).isEqualTo(2000L);
	}

}
