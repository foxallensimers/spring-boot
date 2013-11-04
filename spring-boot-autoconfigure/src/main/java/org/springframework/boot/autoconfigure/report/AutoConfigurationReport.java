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

package org.springframework.boot.autoconfigure.report;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.Outcome;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Bean used to gather autoconfiguration decisions, and then generate a collection of info
 * for beans that were created as well as situations where the conditional outcome was
 * negative.
 * 
 * @author Greg Turnquist
 * @author Dave Syer
 */
public class AutoConfigurationReport implements ApplicationContextAware,
		ApplicationListener<ContextRefreshedEvent> {

	private static Log logger = LogFactory.getLog(AutoConfigurationReport.class);

	private Set<CreatedBeanInfo> beansCreated = new LinkedHashSet<CreatedBeanInfo>();
	private Map<String, List<AutoConfigurationDecision>> autoconfigurationDecisions = new LinkedHashMap<String, List<AutoConfigurationDecision>>();
	private Map<String, List<String>> positive = new LinkedHashMap<String, List<String>>();
	private Map<String, List<String>> negative = new LinkedHashMap<String, List<String>>();
	private ApplicationContext context;
	private boolean initialized = false;

	public static void registerDecision(ConditionContext context, String message,
			String classOrMethodName, Outcome outcome) {
		if (context.getBeanFactory().containsBeanDefinition("autoConfigurationReport")) {
			AutoConfigurationReport autoconfigurationReport = context.getBeanFactory()
					.getBean(AutoConfigurationReport.class);
			autoconfigurationReport.registerDecision(message, classOrMethodName, outcome);
		}
	}

	private void registerDecision(String message, String classOrMethodName,
			Outcome outcome) {
		AutoConfigurationDecision decision = new AutoConfigurationDecision(message,
				classOrMethodName, outcome);
		if (!this.autoconfigurationDecisions.containsKey(classOrMethodName)) {
			this.autoconfigurationDecisions.put(classOrMethodName,
					new ArrayList<AutoConfigurationDecision>());
		}
		this.autoconfigurationDecisions.get(classOrMethodName).add(decision);
	}

	public Set<CreatedBeanInfo> getBeansCreated() {
		return this.beansCreated;
	}

	public Map<String, List<String>> getNegativeDecisions() {
		return this.negative;
	}

	public Set<Class<?>> getBeanTypesCreated() {
		Set<Class<?>> beanTypesCreated = new HashSet<Class<?>>();
		for (CreatedBeanInfo bootCreatedBeanInfo : this.getBeansCreated()) {
			beanTypesCreated.add(bootCreatedBeanInfo.getBeanType());
		}
		return beanTypesCreated;
	}

	public Set<String> getBeanNamesCreated() {
		Set<String> beanNamesCreated = new HashSet<String>();
		for (CreatedBeanInfo bootCreatedBeanInfo : this.getBeansCreated()) {
			beanNamesCreated.add(bootCreatedBeanInfo.getName());
		}
		return beanNamesCreated;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.context = applicationContext;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		initialize();
	}

	public void initialize() {
		if (!this.initialized) {
			synchronized (this) {
				if (!this.initialized) {
					this.initialized = true;
					splitDecisionsIntoPositiveAndNegative();
					scanPositiveDecisionsForBeansBootCreated();
					if (this.context.getEnvironment().getProperty("debug", Boolean.class,
							false)) {
						logger.info("Created beans:");
						for (CreatedBeanInfo info : this.beansCreated) {
							logger.info(info);
						}
						logger.info("Negative decisions:");
						for (String key : this.negative.keySet()) {
							logger.info(key + ": " + this.negative.get(key));
						}
					}
				}
			}
		}
	}

	/**
	 * Scan the list of {@link AutoConfigurationDecision}'s, and if all outcomes true,
	 * then put it on the positive list. Otherwise, put it on the negative list.
	 */
	private synchronized void splitDecisionsIntoPositiveAndNegative() {
		for (String key : this.autoconfigurationDecisions.keySet()) {
			boolean match = true;
			for (AutoConfigurationDecision decision : this.autoconfigurationDecisions
					.get(key)) {
				if (!decision.getOutcome().isMatch()) {
					match = false;
				}
			}
			if (match) {
				if (!this.positive.containsKey(key)) {
					this.positive.put(key, new ArrayList<String>());
				}
				for (AutoConfigurationDecision decision : this.autoconfigurationDecisions
						.get(key)) {
					this.positive.get(key).add(decision.getMessage());
				}
			}
			else {
				if (!this.negative.containsKey(key)) {
					this.negative.put(key, new ArrayList<String>());
				}
				for (AutoConfigurationDecision decision : this.autoconfigurationDecisions
						.get(key)) {
					this.negative.get(key).add(decision.getMessage());
				}
			}
		}
	}

	/**
	 * Scan all the decisions based on successful outcome, and try to find the
	 * corresponding beans Boot created.
	 */
	private synchronized void scanPositiveDecisionsForBeansBootCreated() {
		for (String key : this.positive.keySet()) {
			for (AutoConfigurationDecision decision : this.autoconfigurationDecisions
					.get(key)) {
				for (String beanName : this.context.getBeanDefinitionNames()) {
					Object bean = this.context.getBean(beanName);
					if (decision.getMessage().contains(beanName)
							&& decision.getMessage().contains("matched")) {
						boolean anyMethodsAreBeans = false;
						for (Method method : bean.getClass().getMethods()) {
							if (this.context.containsBean(method.getName())) {
								this.beansCreated.add(new CreatedBeanInfo(method
										.getName(), method.getReturnType(), this.positive
										.get(key)));
								anyMethodsAreBeans = true;
							}
						}

						if (!anyMethodsAreBeans) {
							this.beansCreated.add(new CreatedBeanInfo(beanName, bean,
									this.positive.get(key)));
						}
					}
				}
			}
		}
	}

}
