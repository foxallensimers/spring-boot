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

package org.springframework.boot.autoconfigure.jdbc;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Test;
import org.springframework.boot.TestUtils;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.util.ClassUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link DataSourceAutoConfiguration}.
 * 
 * @author Dave Syer
 */
public class DataSourceAutoConfigurationTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testDefaultDataSourceExists() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
	}

	@Test
	public void testEmbeddedTypeDefaultsUsername() throws Exception {
		TestUtils.addEnviroment(this.context,
				"spring.datasource.driverClassName:org.hsqldb.jdbcDriver",
				"spring.datasource.url:jdbc:hsqldb:mem:testdb");
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource bean = this.context.getBean(DataSource.class);
		assertNotNull(bean);
		org.apache.tomcat.jdbc.pool.DataSource pool = (org.apache.tomcat.jdbc.pool.DataSource) bean;
		assertEquals("org.hsqldb.jdbcDriver", pool.getDriverClassName());
		assertEquals("sa", pool.getUsername());
	}

	@Test
	public void testExplicitDriverClassClearsUserName() throws Exception {
		TestUtils.addEnviroment(this.context,
				"spring.datasource.driverClassName:com.mysql.jdbc.Driver",
				"spring.datasource.url:jdbc:mysql://localhost");
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource bean = this.context.getBean(DataSource.class);
		assertNotNull(bean);
		org.apache.tomcat.jdbc.pool.DataSource pool = (org.apache.tomcat.jdbc.pool.DataSource) bean;
		assertEquals("com.mysql.jdbc.Driver", pool.getDriverClassName());
		assertNull(pool.getUsername());
	}

	@Test
	public void testDefaultDataSourceCanBeOverridden() throws Exception {
		this.context.register(TestDataSourceConfiguration.class,
				DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertTrue("DataSource is wrong type: " + dataSource,
				dataSource instanceof BasicDataSource);
	}

	@Test
	public void testJdbcTemplateExists() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		JdbcTemplate jdbcTemplate = this.context.getBean(JdbcTemplate.class);
		assertNotNull(jdbcTemplate);
		assertNotNull(jdbcTemplate.getDataSource());
	}

	@Test
	public void testJdbcTemplateExistsWithCustomDataSource() throws Exception {
		this.context.register(TestDataSourceConfiguration.class,
				DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		JdbcTemplate jdbcTemplate = this.context.getBean(JdbcTemplate.class);
		assertNotNull(jdbcTemplate);
		assertTrue(jdbcTemplate.getDataSource() instanceof BasicDataSource);
	}

	@Test
	public void testNamedParameterJdbcTemplateExists() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(NamedParameterJdbcOperations.class));
	}

	@Test
	public void testDataSourceInitialized() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertTrue(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource);
		assertNotNull(dataSource);
		JdbcOperations template = new JdbcTemplate(dataSource);
		assertEquals(new Integer(0),
				template.queryForObject("SELECT COUNT(*) from BAR", Integer.class));
	}

	@Test
	public void testDataSourceInitializedWithExplicitScript() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(DataSourceAutoConfiguration.CONFIGURATION_PREFIX + ".schema",
				ClassUtils.addResourcePathToPackagePath(getClass(), "schema.sql"));
		this.context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("test", map));
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertTrue(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource);
		assertNotNull(dataSource);
		JdbcOperations template = new JdbcTemplate(dataSource);
		assertEquals(new Integer(0),
				template.queryForObject("SELECT COUNT(*) from FOO", Integer.class));
	}

	@Test
	public void testDataSourceInitializedWithMultipleScripts() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(DataSourceAutoConfiguration.CONFIGURATION_PREFIX + ".schema",
				ClassUtils.addResourcePathToPackagePath(getClass(), "schema.sql")
						+ ","
						+ ClassUtils.addResourcePathToPackagePath(getClass(),
								"another.sql"));
		this.context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("test", map));
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertTrue(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource);
		assertNotNull(dataSource);
		JdbcOperations template = new JdbcTemplate(dataSource);
		assertEquals(new Integer(0),
				template.queryForObject("SELECT COUNT(*) from FOO", Integer.class));
		assertEquals(new Integer(0),
				template.queryForObject("SELECT COUNT(*) from SPAM", Integer.class));
	}

	@Configuration
	static class TestDataSourceConfiguration {

		private BasicDataSource pool;

		@Bean
		public DataSource dataSource() {
			this.pool = new BasicDataSource();
			this.pool.setDriverClassName("org.hsqldb.jdbcDriver");
			this.pool.setUrl("jdbc:hsqldb:target/overridedb");
			this.pool.setUsername("sa");
			return this.pool;
		}

	}

}
