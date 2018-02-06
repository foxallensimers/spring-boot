/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.UUID;

import javax.sql.DataSource;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricsAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class MetricsAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.with(MetricsRun.simple());

	@Test
	public void autoConfiguredDataSourceIsInstrumented() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.datasource.generate-unique-name=true")
				.run((context) -> {
					context.getBean(DataSource.class).getConnection().getMetaData();
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					registry.get("data.source.max.connections").tags("name", "dataSource")
							.meter();
				});
	}

	@Test
	public void autoConfiguredDataSourceWithCustomMetricName() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.datasource.generate-unique-name=true",
						"management.metrics.jdbc.metric-name=custom.name")
				.run((context) -> {
					context.getBean(DataSource.class).getConnection().getMetaData();
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					registry.get("custom.name.max.connections").tags("name", "dataSource")
							.meter();
				});
	}

	@Test
	public void dataSourceInstrumentationCanBeDisabled() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.datasource.generate-unique-name=true",
						"management.metrics.jdbc.instrument=false")
				.run((context) -> {
					context.getBean(DataSource.class).getConnection().getMetaData();
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("data.source.max.connections")
							.tags("name", "dataSource").meter()).isNull();
				});
	}

	@Test
	public void allDataSourcesCanBeInstrumented() {
		this.contextRunner.withUserConfiguration(TwoDataSourcesConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.run((context) -> {
					context.getBean("firstDataSource", DataSource.class).getConnection()
							.getMetaData();
					context.getBean("secondOne", DataSource.class).getConnection()
							.getMetaData();
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					registry.get("data.source.max.connections").tags("name", "first")
							.meter();
					registry.get("data.source.max.connections").tags("name", "secondOne")
							.meter();
				});
	}

	@Test
	public void propertyBasedMeterFilter() {
		this.contextRunner.withPropertyValues("management.metrics.enable.my.org=false")
				.run(context -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					registry.timer("my.org.timer");
					assertThat(registry.find("my.org.timer").timer()).isNull();
				});
	}

	@Configuration
	static class TwoDataSourcesConfiguration {

		@Bean
		public DataSource firstDataSource() {
			return createDataSource();
		}

		@Bean
		public DataSource secondOne() {
			return createDataSource();
		}

		private DataSource createDataSource() {
			String url = "jdbc:hsqldb:mem:test-" + UUID.randomUUID();
			return DataSourceBuilder.create().url(url).build();
		}

	}

}
