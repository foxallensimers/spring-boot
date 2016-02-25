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

package org.springframework.boot.autoconfigure.jdbc;

import java.sql.SQLException;
import java.util.Random;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link DataSourceInitializer}.
 *
 * @author Dave Syer
 */
public class DataSourceInitializerTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Before
	public void init() {
		EmbeddedDatabaseConnection.override = null;
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:false",
				"spring.datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt());
	}

	@After
	public void restore() {
		EmbeddedDatabaseConnection.override = null;
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultDataSourceDoesNotExists() throws Exception {
		this.context.register(DataSourceInitializer.class,
				PropertyPlaceholderAutoConfiguration.class, DataSourceProperties.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(DataSource.class).length)
				.isEqualTo(0);
	}

	@Test
	public void testTwoDataSources() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"datasource.one.url=jdbc:hsqldb:mem:/one",
				"datasource.one.driverClassName=org.hsqldb.Driver",
				"datasource.two.url=jdbc:hsqldb:mem:/two",
				"datasource.two.driverClassName=org.hsqldb.Driver");
		this.context.register(TwoDataSources.class, DataSourceInitializer.class,
				PropertyPlaceholderAutoConfiguration.class, DataSourceProperties.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(DataSource.class).length)
				.isEqualTo(2);
	}

	@Test
	public void testDataSourceInitialized() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:true");
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource).isTrue();
		assertThat(dataSource).isNotNull();
		JdbcOperations template = new JdbcTemplate(dataSource);
		assertThat(template.queryForObject("SELECT COUNT(*) from BAR", Integer.class))
				.isEqualTo(1);
	}

	@Test
	public void testDataSourceInitializedWithExplicitScript() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:true",
				"spring.datasource.schema:" + ClassUtils
						.addResourcePathToPackagePath(getClass(), "schema.sql"),
				"spring.datasource.data:" + ClassUtils
						.addResourcePathToPackagePath(getClass(), "data.sql"));
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource).isTrue();
		assertThat(dataSource).isNotNull();
		JdbcOperations template = new JdbcTemplate(dataSource);
		assertThat(template.queryForObject("SELECT COUNT(*) from FOO", Integer.class))
				.isEqualTo(1);
	}

	@Test
	public void testDataSourceInitializedWithMultipleScripts() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:true",
				"spring.datasource.schema:"
						+ ClassUtils.addResourcePathToPackagePath(getClass(),
								"schema.sql")
						+ ","
						+ ClassUtils.addResourcePathToPackagePath(getClass(),
								"another.sql"),
				"spring.datasource.data:" + ClassUtils
						.addResourcePathToPackagePath(getClass(), "data.sql"));
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource).isTrue();
		assertThat(dataSource).isNotNull();
		JdbcOperations template = new JdbcTemplate(dataSource);
		assertThat(template.queryForObject("SELECT COUNT(*) from FOO", Integer.class))
				.isEqualTo(1);
		assertThat(template.queryForObject("SELECT COUNT(*) from SPAM", Integer.class))
				.isEqualTo(0);
	}

	@Test
	public void testDataSourceInitializedWithExplicitSqlScriptEncoding()
			throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:true",
				"spring.datasource.sqlScriptEncoding:UTF-8",
				"spring.datasource.schema:" + ClassUtils
						.addResourcePathToPackagePath(getClass(), "encoding-schema.sql"),
				"spring.datasource.data:" + ClassUtils
						.addResourcePathToPackagePath(getClass(), "encoding-data.sql"));
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource).isTrue();
		assertThat(dataSource).isNotNull();
		JdbcOperations template = new JdbcTemplate(dataSource);
		assertThat(template.queryForObject("SELECT COUNT(*) from BAR", Integer.class))
				.isEqualTo(2);
		assertThat(
				template.queryForObject("SELECT name from BAR WHERE id=1", String.class))
						.isEqualTo("bar");
		assertThat(
				template.queryForObject("SELECT name from BAR WHERE id=2", String.class))
						.isEqualTo("ばー");
	}

	@Test
	public void testInitializationDisabled() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();

		DataSource dataSource = this.context.getBean(DataSource.class);

		this.context.publishEvent(new DataSourceInitializedEvent(dataSource));

		assertThat(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource).isTrue();
		assertThat(dataSource).isNotNull();
		JdbcOperations template = new JdbcTemplate(dataSource);

		try {
			template.queryForObject("SELECT COUNT(*) from BAR", Integer.class);
			fail("Query should have failed as BAR table does not exist");
		}
		catch (BadSqlGrammarException ex) {
			SQLException sqlException = ex.getSQLException();
			int expectedCode = -5501; // user lacks privilege or object not found
			assertThat(sqlException.getErrorCode()).isEqualTo(expectedCode);
		}
	}

	@Configuration
	@EnableConfigurationProperties
	protected static class TwoDataSources {

		@Bean
		@Primary
		@ConfigurationProperties(prefix = "datasource.one")
		public DataSource oneDataSource() {
			return DataSourceBuilder.create().build();
		}

		@Bean
		@ConfigurationProperties(prefix = "datasource.two")
		public DataSource twoDataSource() {
			return DataSourceBuilder.create().build();
		}

	}

}
