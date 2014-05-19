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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.hibernate.jpa.boot.spi.Bootstrap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryBuilder.EntityManagerFactoryBeanCallback;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.HibernateEntityManagerCondition;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.ClassUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Hibernate JPA.
 * 
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnClass({ LocalContainerEntityManagerFactoryBean.class,
		EnableTransactionManagement.class, EntityManager.class })
@Conditional(HibernateEntityManagerCondition.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class HibernateJpaAutoConfiguration extends JpaBaseConfiguration {

	@Autowired
	private JpaProperties properties;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ConfigurableApplicationContext applicationContext;

	@Override
	protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
		return new HibernateJpaVendorAdapter();
	}

	@Override
	protected Map<String, Object> getVendorProperties() {
		return this.properties.getInitialHibernateProperties(this.dataSource);
	}

	@Override
	protected EntityManagerFactoryBeanCallback getVendorCallback() {
		final Map<String, Object> map = this.properties
				.getHibernateProperties(this.dataSource);
		return new EntityManagerFactoryBeanCallback() {
			@Override
			public void execute(
					LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
				HibernateJpaAutoConfiguration.this.applicationContext
						.addApplicationListener(new DeferredSchemaAction(
								entityManagerFactoryBean, map));
			}
		};
	}

	private static class DeferredSchemaAction implements
			ApplicationListener<ContextRefreshedEvent> {

		private Map<String, Object> map;
		private LocalContainerEntityManagerFactoryBean factory;

		public DeferredSchemaAction(LocalContainerEntityManagerFactoryBean factory,
				Map<String, Object> map) {
			this.factory = factory;
			this.map = map;
		}

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			String ddlAuto = (String) this.map.get("hibernate.hbm2ddl.auto");
			if (ddlAuto == null || "none".equals(ddlAuto)) {
				return;
			}
			Bootstrap.getEntityManagerFactoryBuilder(
					this.factory.getPersistenceUnitInfo(), this.map).generateSchema();
		}
	}

	static class HibernateEntityManagerCondition extends SpringBootCondition {

		private static String[] CLASS_NAMES = {
				"org.hibernate.ejb.HibernateEntityManager",
				"org.hibernate.jpa.HibernateEntityManager" };

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			for (String className : CLASS_NAMES) {
				if (ClassUtils.isPresent(className, context.getClassLoader())) {
					return ConditionOutcome.match("found HibernateEntityManager class");
				}
			}
			return ConditionOutcome.noMatch("did not find HibernateEntityManager class");
		}
	}

}
