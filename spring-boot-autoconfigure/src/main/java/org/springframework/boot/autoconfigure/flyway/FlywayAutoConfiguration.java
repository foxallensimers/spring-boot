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

package org.springframework.boot.autoconfigure.flyway;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static java.util.Arrays.asList;
import static org.springframework.beans.factory.BeanFactoryUtils.beanNamesForTypeIncludingAncestors;
import static org.springframework.beans.factory.BeanFactoryUtils.transformedBeanName;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Flyway database migrations.
 * 
 * @author Dave Syer
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnBean(DataSource.class)
@ConditionalOnExpression("${flyway.enabled:true}")
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class FlywayAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(Flyway.class)
	@EnableConfigurationProperties(FlywayProperties.class)
	@Import(FlywayJpaDependencyConfiguration.class)
	public static class FlywayConfiguration {

		@Autowired
		private FlywayProperties properties = new FlywayProperties();

		@Autowired
		private ResourceLoader resourceLoader = new DefaultResourceLoader();

		@Autowired(required = false)
		private DataSource dataSource;

		@Autowired(required = false)
		@FlywayDataSource
		private DataSource flywayDataSource;

		@PostConstruct
		public void checkLocationExists() {
			if (this.properties.isCheckLocation()) {

				Assert.state(!this.properties.getLocations().isEmpty(),
						"Migration script locations not configured");
				boolean exists = false;
				for (String location : this.properties.getLocations()) {
					Resource resource = this.resourceLoader.getResource(location);
					exists = (!exists && resource.exists());
				}
				Assert.state(exists, "Cannot find migrations location in: "
						+ this.properties.getLocations()
						+ " (please add migrations or check your Flyway configuration)");
			}
		}

		@Bean(initMethod = "migrate")
		@ConfigurationProperties(prefix = "flyway")
		public Flyway flyway() {
			Flyway flyway = new Flyway();
			if (this.properties.isCreateDataSource()) {
				flyway.setDataSource(this.properties.getUrl(), this.properties.getUser(),
						this.properties.getPassword(), this.properties.getInitSqls()
								.toArray(new String[0]));
			}
			else if (this.flywayDataSource != null) {
				flyway.setDataSource(this.flywayDataSource);
			}
			else {
				flyway.setDataSource(this.dataSource);
			}
			return flyway;
		}

	}

	@Configuration
	@ConditionalOnClass(LocalContainerEntityManagerFactoryBean.class)
	@ConditionalOnBean(AbstractEntityManagerFactoryBean.class)
	protected static class FlywayJpaDependencyConfiguration implements
			BeanFactoryPostProcessor {

		public static final String FLYWAY_JPA_BEAN_NAME = "flyway";

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {

			for (String beanName : getEntityManagerFactoryBeanNames(beanFactory)) {
				BeanDefinition definition = getBeanDefinition(beanName, beanFactory);
				definition.setDependsOn(StringUtils.addStringToArray(
						definition.getDependsOn(), FLYWAY_JPA_BEAN_NAME));
			}
		}

		private static BeanDefinition getBeanDefinition(String beanName,
				ConfigurableListableBeanFactory beanFactory) {
			try {
				return beanFactory.getBeanDefinition(beanName);
			}
			catch (NoSuchBeanDefinitionException e) {

				BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();

				if (parentBeanFactory instanceof ConfigurableListableBeanFactory) {
					return getBeanDefinition(beanName,
							(ConfigurableListableBeanFactory) parentBeanFactory);
				}

				throw e;
			}
		}

		private static Iterable<String> getEntityManagerFactoryBeanNames(
				ListableBeanFactory beanFactory) {

			Set<String> names = new HashSet<String>();
			names.addAll(asList(beanNamesForTypeIncludingAncestors(beanFactory,
					EntityManagerFactory.class, true, false)));

			for (String factoryBeanName : beanNamesForTypeIncludingAncestors(beanFactory,
					AbstractEntityManagerFactoryBean.class, true, false)) {
				names.add(transformedBeanName(factoryBeanName));
			}

			return names;
		}

	}

}
