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

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.SpringNamingStrategy;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.util.StringUtils;

/**
 * External configuration properties for a JPA EntityManagerFactory created by Spring.
 * 
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.jpa")
public class JpaProperties {

	private static final Log logger = LogFactory.getLog(JpaProperties.class);

	private Map<String, String> properties = new HashMap<String, String>();

	private String databasePlatform;

	private Database database = Database.DEFAULT;

	private boolean generateDdl = false;

	private boolean showSql = false;

	private Hibernate hibernate = new Hibernate();

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public String getDatabasePlatform() {
		return this.databasePlatform;
	}

	public void setDatabasePlatform(String databasePlatform) {
		this.databasePlatform = databasePlatform;
	}

	public Database getDatabase() {
		return this.database;
	}

	public void setDatabase(Database database) {
		this.database = database;
	}

	public boolean isGenerateDdl() {
		return this.generateDdl;
	}

	public void setGenerateDdl(boolean generateDdl) {
		this.generateDdl = generateDdl;
	}

	public boolean isShowSql() {
		return this.showSql;
	}

	public void setShowSql(boolean showSql) {
		this.showSql = showSql;
	}

	public Hibernate getHibernate() {
		return this.hibernate;
	}

	public void setHibernate(Hibernate hibernate) {
		this.hibernate = hibernate;
	}

	/**
	 * Get configuration properties for the initialization of the main Hibernate
	 * EntityManagerFactory.
	 * 
	 * @param dataSource the DataSource in case it is needed to determine the properties
	 * @return some Hibernate properties for configuration
	 */
	public Map<String, String> getHibernateProperties(DataSource dataSource) {
		return this.hibernate.getAdditionalProperties(this.properties, dataSource);
	}

	public static class Hibernate {

		private Class<?> namingStrategy;

		private static Class<?> DEFAULT_NAMING_STRATEGY = SpringNamingStrategy.class;

		private String ddlAuto;

		public Class<?> getNamingStrategy() {
			return this.namingStrategy;
		}

		public void setNamingStrategy(Class<?> namingStrategy) {
			this.namingStrategy = namingStrategy;
		}

		@Deprecated
		public void setNamingstrategy(Class<?> namingStrategy) {
			logger.warn("The property spring.jpa.namingstrategy has been renamed, "
					+ "please update your configuration to use namingStrategy or naming-strategy or naming_strategy");
			this.setNamingStrategy(namingStrategy);
		}

		public String getDdlAuto() {
			return this.ddlAuto;
		}

		private String getActualDdlAuto(Map<String, String> existing,
				DataSource dataSource) {
			String ddlAuto = this.ddlAuto != null ? this.ddlAuto
					: getDefaultDdlAuto(dataSource);
			if (!isAlreadyProvided(existing, "hbm2ddl.auto") && !"none".equals(ddlAuto)) {
				return ddlAuto;
			}
			if (isAlreadyProvided(existing, "hbm2ddl.auto")) {
				return existing.get("hibernate.hbm2ddl.auto");
			}
			return "none";
		}

		public void setDdlAuto(String ddlAuto) {
			this.ddlAuto = ddlAuto;
		}

		private Map<String, String> getAdditionalProperties(Map<String, String> existing,
				DataSource dataSource) {
			Map<String, String> result = new HashMap<String, String>();
			if (!isAlreadyProvided(existing, "ejb.naming_strategy")
					&& this.namingStrategy != null) {
				result.put("hibernate.ejb.naming_strategy", this.namingStrategy.getName());
			}
			else if (this.namingStrategy == null) {
				result.put("hibernate.ejb.naming_strategy",
						DEFAULT_NAMING_STRATEGY.getName());
			}
			String ddlAuto = getActualDdlAuto(existing, dataSource);
			if (StringUtils.hasText(ddlAuto) && !"none".equals(ddlAuto)) {
				result.put("hibernate.hbm2ddl.auto", ddlAuto);
			}
			else {
				result.remove("hibernate.hbm2ddl.auto");
			}
			return result;
		}

		private boolean isAlreadyProvided(Map<String, String> existing, String key) {
			return existing.containsKey("hibernate." + key);
		}

		private String getDefaultDdlAuto(DataSource dataSource) {
			if (EmbeddedDatabaseConnection.isEmbedded(dataSource)) {
				return "create-drop";
			}
			return "none";
		}

	}

}
