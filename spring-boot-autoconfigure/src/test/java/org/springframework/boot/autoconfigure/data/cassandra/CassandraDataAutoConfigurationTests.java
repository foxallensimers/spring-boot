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

package org.springframework.boot.autoconfigure.data.cassandra;

import java.util.Set;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.DriverException;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.city.City;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.mapping.CassandraMappingContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CassandraDataAutoConfiguration}
 *
 * @author Eddú Meléndez
 * @author Mark Paluch
 */
public class CassandraDataAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void templateExists() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestExcludeConfiguration.class, TestConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				CassandraAutoConfiguration.class, CassandraDataAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(CassandraTemplate.class).length)
				.isEqualTo(1);
	}

	@Test
	public void hasDefaultSchemaActionSet() {

		if (isCassandraAvailable()) {

			this.context = new AnnotationConfigApplicationContext();
			String cityPackage = City.class.getPackage().getName();
			AutoConfigurationPackages.register(this.context, cityPackage);
			this.context.register(CassandraAutoConfiguration.class,
					CassandraDataAutoConfiguration.class);
			this.context.refresh();

			CassandraSessionFactoryBean bean = this.context
					.getBean(CassandraSessionFactoryBean.class);
			assertThat(bean.getSchemaAction()).isEqualTo(SchemaAction.NONE);
		}
	}

	@Test
	public void hasRecreateSchemaActionSet() {

		if (isCassandraAvailable()) {
			createTestKeyspaceIfNotExists();

			this.context = new AnnotationConfigApplicationContext();
			String cityPackage = City.class.getPackage().getName();
			AutoConfigurationPackages.register(this.context, cityPackage);

			EnvironmentTestUtils.addEnvironment(this.context,
					"spring.data.cassandra.schemaAction:RECREATE_DROP_UNUSED", "spring.data.cassandra.keyspaceName:boot_test");

			this.context.register(CassandraAutoConfiguration.class,
					CassandraDataAutoConfiguration.class);
			this.context.refresh();

			CassandraSessionFactoryBean bean = this.context
					.getBean(CassandraSessionFactoryBean.class);
			assertThat(bean.getSchemaAction()).isEqualTo(SchemaAction.RECREATE_DROP_UNUSED);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void entityScanShouldSetInitialEntitySet() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, EntityScanConfig.class,
				PropertyPlaceholderAutoConfiguration.class,
				CassandraAutoConfiguration.class, CassandraDataAutoConfiguration.class);
		this.context.refresh();
		CassandraMappingContext mappingContext = this.context
				.getBean(CassandraMappingContext.class);
		Set<Class<?>> initialEntitySet = (Set<Class<?>>) ReflectionTestUtils
				.getField(mappingContext, "initialEntitySet");
		assertThat(initialEntitySet).containsOnly(City.class);
	}

	/**
	 * @return {@literal true} if Cassandra is available
	 */
	private static boolean isCassandraAvailable() {

		Cluster cluster = newCluster();
		try {
			cluster.connect().close();
			return true;
		}
		catch (DriverException exception) {
			return false;
		}
		finally {
			cluster.closeAsync();
		}
	}

	private static void createTestKeyspaceIfNotExists() {

		Cluster cluster = newCluster();
		try {
			Session session = cluster.connect();
			session.execute("CREATE KEYSPACE IF NOT EXISTS boot_test"
					+ "  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };");
			session.close();
		}
		finally {
			cluster.closeAsync();
		}
	}

	private static Cluster newCluster() {
		return Cluster.builder().addContactPoint("localhost").build();
	}

	@Configuration
	@ComponentScan(excludeFilters = @ComponentScan.Filter(classes = {
			Session.class }, type = FilterType.ASSIGNABLE_TYPE))
	static class TestExcludeConfiguration {

	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public Session getObject() {
			return mock(Session.class);
		}

	}

	@Configuration
	@EntityScan("org.springframework.boot.autoconfigure.data.cassandra.city")
	static class EntityScanConfig {

	}

}
