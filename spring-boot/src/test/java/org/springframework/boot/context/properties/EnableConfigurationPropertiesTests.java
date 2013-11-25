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

package org.springframework.boot.context.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.TestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link EnableConfigurationProperties}.
 * 
 * @author Dave Syer
 */
public class EnableConfigurationPropertiesTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testBasicPropertiesBinding() {
		this.context.register(TestConfiguration.class);
		TestUtils.addEnviroment(this.context, "name:foo");
		this.context.refresh();
		assertEquals(1, this.context.getBeanNamesForType(TestProperties.class).length);
		assertEquals("foo", this.context.getBean(TestProperties.class).name);
	}

	@Test
	public void testStrictPropertiesBinding() {
		this.context.register(StrictTestConfiguration.class);
		TestUtils.addEnviroment(this.context, "name:foo");
		this.context.refresh();
		assertEquals(1,
				this.context.getBeanNamesForType(StrictTestProperties.class).length);
		assertEquals("foo", this.context.getBean(TestProperties.class).name);
	}

	@Test
	public void testIgnoreNestedPropertiesBinding() {
		this.context.register(IgnoreNestedTestConfiguration.class);
		TestUtils.addEnviroment(this.context, "name:foo", "nested.name:bar");
		this.context.refresh();
		assertEquals(1,
				this.context.getBeanNamesForType(IgnoreNestedTestProperties.class).length);
		assertEquals("foo", this.context.getBean(TestProperties.class).name);
	}

	@Test
	public void testNestedPropertiesBinding() {
		this.context.register(NestedConfiguration.class);
		TestUtils.addEnviroment(this.context, "name:foo", "nested.name:bar");
		this.context.refresh();
		assertEquals(1, this.context.getBeanNamesForType(NestedProperties.class).length);
		assertEquals("foo", this.context.getBean(NestedProperties.class).name);
		assertEquals("bar", this.context.getBean(NestedProperties.class).nested.name);
	}

	@Test
	public void testBasicPropertiesBindingWithAnnotationOnBaseClass() {
		this.context.register(DerivedConfiguration.class);
		TestUtils.addEnviroment(this.context, "name:foo");
		this.context.refresh();
		assertEquals(1, this.context.getBeanNamesForType(DerivedProperties.class).length);
		assertEquals("foo", this.context.getBean(BaseProperties.class).name);
	}

	@Test
	public void testArrayPropertiesBinding() {
		this.context.register(TestConfiguration.class);
		TestUtils.addEnviroment(this.context, "name:foo", "array:1,2,3");
		this.context.refresh();
		assertEquals(1, this.context.getBeanNamesForType(TestProperties.class).length);
		assertEquals(3, this.context.getBean(TestProperties.class).getArray().length);
	}

	@Test
	public void testCollectionPropertiesBindingFromYamlArray() {
		this.context.register(TestConfiguration.class);
		TestUtils.addEnviroment(this.context, "name:foo", "list[0]:1", "list[1]:2");
		this.context.refresh();
		assertEquals(2, this.context.getBean(TestProperties.class).getList().size());
	}

	@Test
	public void testPropertiesBindingWithoutAnnotation() {
		this.context.register(MoreConfiguration.class);
		TestUtils.addEnviroment(this.context, "name:foo");
		this.context.refresh();
		assertEquals(1, this.context.getBeanNamesForType(MoreProperties.class).length);
		assertEquals("foo", this.context.getBean(MoreProperties.class).name);
	}

	@Test
	public void testPropertiesBindingWithDefaultsInXml() {
		this.context.register(TestConfiguration.class, DefaultXmlConfiguration.class);
		this.context.refresh();
		String[] beanNames = this.context.getBeanNamesForType(TestProperties.class);
		assertEquals("Wrong beans: " + Arrays.asList(beanNames), 1, beanNames.length);
		assertEquals("bar", this.context.getBean(TestProperties.class).name);
	}

	@Test
	public void testPropertiesBindingWithDefaultsInBeanMethod() {
		this.context.register(DefaultConfiguration.class);
		this.context.refresh();
		String[] beanNames = this.context.getBeanNamesForType(TestProperties.class);
		assertEquals("Wrong beans: " + Arrays.asList(beanNames), 1, beanNames.length);
		assertEquals("bar", this.context.getBean(TestProperties.class).name);
	}

	@Test
	public void testBindingDirectlyToFile() {
		this.context.register(ResourceBindingProperties.class, TestConfiguration.class);
		this.context.refresh();
		assertEquals(1,
				this.context.getBeanNamesForType(ResourceBindingProperties.class).length);
		assertEquals("foo", this.context.getBean(ResourceBindingProperties.class).name);
	}

	@Test
	public void testBindingDirectlyToFileResolvedFromEnvironment() {
		TestUtils.addEnviroment(this.context, "binding.location:classpath:other.yml");
		this.context.register(ResourceBindingProperties.class, TestConfiguration.class);
		this.context.refresh();
		assertEquals(1,
				this.context.getBeanNamesForType(ResourceBindingProperties.class).length);
		assertEquals("other", this.context.getBean(ResourceBindingProperties.class).name);
	}

	@Test
	public void testBindingDirectlyToFileWithDefaultsWhenProfileNotFound() {
		this.context.register(ResourceBindingProperties.class, TestConfiguration.class);
		this.context.getEnvironment().addActiveProfile("nonexistent");
		this.context.refresh();
		assertEquals(1,
				this.context.getBeanNamesForType(ResourceBindingProperties.class).length);
		assertEquals("foo", this.context.getBean(ResourceBindingProperties.class).name);
	}

	@Test
	public void testBindingDirectlyToFileWithExplicitSpringProfile() {
		this.context.register(ResourceBindingProperties.class, TestConfiguration.class);
		this.context.getEnvironment().addActiveProfile("super");
		this.context.refresh();
		assertEquals(1,
				this.context.getBeanNamesForType(ResourceBindingProperties.class).length);
		assertEquals("bar", this.context.getBean(ResourceBindingProperties.class).name);
	}

	@Test
	public void testBindingWithTwoBeans() {
		this.context.register(MoreConfiguration.class, TestConfiguration.class);
		this.context.refresh();
		assertEquals(1, this.context.getBeanNamesForType(TestProperties.class).length);
		assertEquals(1, this.context.getBeanNamesForType(MoreProperties.class).length);
	}

	@Test
	public void testBindingWithParentContext() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.register(TestConfiguration.class);
		parent.refresh();
		TestUtils.addEnviroment(this.context, "name:foo");
		this.context.setParent(parent);
		this.context.register(TestConfiguration.class, TestConsumer.class);
		this.context.refresh();
		assertEquals(1, this.context.getBeanNamesForType(TestProperties.class).length);
		assertEquals(1, parent.getBeanNamesForType(TestProperties.class).length);
		assertEquals("foo", this.context.getBean(TestConsumer.class).getName());
	}

	@Test
	public void testBindingOnlyParentContext() {
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		TestUtils.addEnviroment(parent, "name:foo");
		parent.register(TestConfiguration.class);
		parent.refresh();
		this.context.setParent(parent);
		this.context.register(TestConsumer.class);
		this.context.refresh();
		assertEquals(0, this.context.getBeanNamesForType(TestProperties.class).length);
		assertEquals(1, parent.getBeanNamesForType(TestProperties.class).length);
		assertEquals("foo", this.context.getBean(TestConsumer.class).getName());
	}

	@Configuration
	@EnableConfigurationProperties(TestProperties.class)
	protected static class TestConfiguration {
	}

	@Configuration
	@EnableConfigurationProperties(StrictTestProperties.class)
	protected static class StrictTestConfiguration {
	}

	@Configuration
	@EnableConfigurationProperties(IgnoreNestedTestProperties.class)
	protected static class IgnoreNestedTestConfiguration {
	}

	@Configuration
	@EnableConfigurationProperties(DerivedProperties.class)
	protected static class DerivedConfiguration {
	}

	@Configuration
	@EnableConfigurationProperties(NestedProperties.class)
	protected static class NestedConfiguration {
	}

	@Configuration
	protected static class DefaultConfiguration {
		@Bean
		public TestProperties testProperties() {
			TestProperties test = new TestProperties();
			test.setName("bar");
			return test;
		}
	}

	@Configuration
	@ImportResource("org/springframework/boot/context/properties/testProperties.xml")
	protected static class DefaultXmlConfiguration {
	}

	@Component
	protected static class TestConsumer {
		@Autowired
		private TestProperties properties;

		@PostConstruct
		public void init() {
			assertNotNull(this.properties);
		}

		public String getName() {
			return this.properties.name;
		}
	}

	@Configuration
	@EnableConfigurationProperties(MoreProperties.class)
	protected static class MoreConfiguration {
	}

	@ConfigurationProperties
	protected static class NestedProperties {
		private String name;
		private Nested nested = new Nested();

		public void setName(String name) {
			this.name = name;
		}

		public Nested getNested() {
			return this.nested;
		}

		protected static class Nested {
			private String name;

			public void setName(String name) {
				this.name = name;
			}

		}
	}

	@ConfigurationProperties
	protected static class BaseProperties {
		private String name;

		public void setName(String name) {
			this.name = name;
		}
	}

	protected static class DerivedProperties extends BaseProperties {
	}

	@ConfigurationProperties
	protected static class TestProperties {
		private String name;
		private int[] array;
		private List<Integer> list = new ArrayList<Integer>();

		// No getter - you should be able to bind to a write-only bean

		public void setName(String name) {
			this.name = name;
		}

		public void setArray(int... values) {
			this.array = values;
		}

		public int[] getArray() {
			return this.array;
		}

		public List<Integer> getList() {
			return this.list;
		}
	}

	@ConfigurationProperties(ignoreUnknownFields = false)
	protected static class StrictTestProperties extends TestProperties {

	}

	@ConfigurationProperties(ignoreUnknownFields = false, ignoreNestedProperties = true)
	protected static class IgnoreNestedTestProperties extends TestProperties {

	}

	protected static class MoreProperties {
		private String name;

		public void setName(String name) {
			this.name = name;
		}

		// No getter - you should be able to bind to a write-only bean
	}

	@ConfigurationProperties(path = "${binding.location:classpath:name.yml}")
	protected static class ResourceBindingProperties {
		private String name;

		public void setName(String name) {
			this.name = name;
		}

		// No getter - you should be able to bind to a write-only bean
	}
}
