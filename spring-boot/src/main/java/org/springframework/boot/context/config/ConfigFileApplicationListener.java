/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.context.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.logging.Log;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.EnumerableCompositePropertySource;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.PropertySourcesLoader;
import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnvironmentPostProcessor} that configures the context environment by loading
 * properties from well known file locations. By default properties will be loaded from
 * 'application.properties' and/or 'application.yml' files in the following locations:
 * <ul>
 * <li>classpath:</li>
 * <li>file:./</li>
 * <li>classpath:config/</li>
 * <li>file:./config/:</li>
 * </ul>
 * <p>
 * Alternative search locations and names can be specified using
 * {@link #setSearchLocations(String)} and {@link #setSearchNames(String)}.
 * <p>
 * Additional files will also be loaded based on active profiles. For example if a 'web'
 * profile is active 'application-web.properties' and 'application-web.yml' will be
 * considered.
 * <p>
 * The 'spring.config.name' property can be used to specify an alternative name to load
 * and the 'spring.config.location' property can be used to specify alternative search
 * locations or specific files.
 * <p>
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Madhura Bhave
 */
public class ConfigFileApplicationListener implements EnvironmentPostProcessor,
		ApplicationListener<ApplicationEvent>, Ordered {

	private static final String DEFAULT_PROPERTIES = "defaultProperties";

	// Note the order is from least to most specific (last one wins)
	private static final String DEFAULT_SEARCH_LOCATIONS = "classpath:/,classpath:/config/,file:./,file:./config/";

	private static final String DEFAULT_NAMES = "application";

	/**
	 * The "active profiles" property name.
	 */
	public static final String ACTIVE_PROFILES_PROPERTY = "spring.profiles.active";

	/**
	 * The "includes profiles" property name.
	 */
	public static final String INCLUDE_PROFILES_PROPERTY = "spring.profiles.include";

	/**
	 * The "config name" property name.
	 */
	public static final String CONFIG_NAME_PROPERTY = "spring.config.name";

	/**
	 * The "config location" property name.
	 */
	public static final String CONFIG_LOCATION_PROPERTY = "spring.config.location";

	/**
	 * The default order for the processor.
	 */
	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	/**
	 * Name of the application configuration {@link PropertySource}.
	 */
	public static final String APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME = "applicationConfigurationProperties";

	private final DeferredLog logger = new DeferredLog();

	private String searchLocations;

	private String names;

	private int order = DEFAULT_ORDER;

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationEnvironmentPreparedEvent) {
			onApplicationEnvironmentPreparedEvent(
					(ApplicationEnvironmentPreparedEvent) event);
		}
		if (event instanceof ApplicationPreparedEvent) {
			onApplicationPreparedEvent(event);
		}
	}

	private void onApplicationEnvironmentPreparedEvent(
			ApplicationEnvironmentPreparedEvent event) {
		List<EnvironmentPostProcessor> postProcessors = loadPostProcessors();
		postProcessors.add(this);
		AnnotationAwareOrderComparator.sort(postProcessors);
		for (EnvironmentPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessEnvironment(event.getEnvironment(),
					event.getSpringApplication());
		}
	}

	List<EnvironmentPostProcessor> loadPostProcessors() {
		return SpringFactoriesLoader.loadFactories(EnvironmentPostProcessor.class,
				getClass().getClassLoader());
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
			SpringApplication application) {
		addPropertySources(environment, application.getResourceLoader());
	}

	private void onApplicationPreparedEvent(ApplicationEvent event) {
		this.logger.replayTo(ConfigFileApplicationListener.class);
		addPostProcessors(((ApplicationPreparedEvent) event).getApplicationContext());
	}

	/**
	 * Add config file property sources to the specified environment.
	 * @param environment the environment to add source to
	 * @param resourceLoader the resource loader
	 * @see #addPostProcessors(ConfigurableApplicationContext)
	 */
	protected void addPropertySources(ConfigurableEnvironment environment,
			ResourceLoader resourceLoader) {
		RandomValuePropertySource.addToEnvironment(environment);
		new Loader(environment, resourceLoader).load();
	}

	/**
	 * Add appropriate post-processors to post-configure the property-sources.
	 * @param context the context to configure
	 */
	protected void addPostProcessors(ConfigurableApplicationContext context) {
		context.addBeanFactoryPostProcessor(
				new PropertySourceOrderingPostProcessor(context));
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the search locations that will be considered as a comma-separated list. Each
	 * search location should be a directory path (ending in "/") and it will be prefixed
	 * by the file names constructed from {@link #setSearchNames(String) search names} and
	 * profiles (if any) plus file extensions supported by the properties loaders.
	 * Locations are considered in the order specified, with later items taking precedence
	 * (like a map merge).
	 * @param locations the search locations
	 */
	public void setSearchLocations(String locations) {
		Assert.hasLength(locations, "Locations must not be empty");
		this.searchLocations = locations;
	}

	/**
	 * Sets the names of the files that should be loaded (excluding file extension) as a
	 * comma-separated list.
	 * @param names the names to load
	 */
	public void setSearchNames(String names) {
		Assert.hasLength(names, "Names must not be empty");
		this.names = names;
	}

	/**
	 * {@link BeanFactoryPostProcessor} to re-order our property sources below any
	 * {@code @PropertySource} items added by the {@link ConfigurationClassPostProcessor}.
	 */
	private class PropertySourceOrderingPostProcessor
			implements BeanFactoryPostProcessor, Ordered {

		private ConfigurableApplicationContext context;

		PropertySourceOrderingPostProcessor(ConfigurableApplicationContext context) {
			this.context = context;
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
			reorderSources(this.context.getEnvironment());
		}

		private void reorderSources(ConfigurableEnvironment environment) {
			LoadedPropertySources.finishAndRelocate(environment.getPropertySources());
			PropertySource<?> defaultProperties = environment.getPropertySources()
					.remove(DEFAULT_PROPERTIES);
			if (defaultProperties != null) {
				environment.getPropertySources().addLast(defaultProperties);
			}
		}

	}

	/**
	 * Loads candidate property sources and configures the active profiles.
	 */
	private class Loader {

		private final Log logger = ConfigFileApplicationListener.this.logger;

		private final ConfigurableEnvironment environment;

		private final ResourceLoader resourceLoader;

		private PropertySourcesLoader propertiesLoader;

		private Queue<Profile> profiles;

		private List<Profile> processedProfiles;

		private boolean activatedProfiles;

		Loader(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {
			this.environment = environment;
			this.resourceLoader = resourceLoader == null ? new DefaultResourceLoader()
					: resourceLoader;
		}

		public void load() {
			this.propertiesLoader = new PropertySourcesLoader();
			this.activatedProfiles = false;
			this.profiles = Collections.asLifoQueue(new LinkedList<Profile>());
			this.processedProfiles = new LinkedList<>();

			// Pre-existing active profiles set via Environment.setActiveProfiles()
			// are additional profiles and config files are allowed to add more if
			// they want to, so don't call addActiveProfiles() here.
			Set<Profile> initialActiveProfiles = initializeActiveProfiles();
			this.profiles.addAll(getUnprocessedActiveProfiles(initialActiveProfiles));
			if (this.profiles.isEmpty()) {
				for (String defaultProfileName : this.environment.getDefaultProfiles()) {
					Profile defaultProfile = new Profile(defaultProfileName, true);
					if (!this.profiles.contains(defaultProfile)) {
						this.profiles.add(defaultProfile);
					}
				}
			}

			// The default profile for these purposes is represented as null. We add it
			// last so that it is first out of the queue (active profiles will then
			// override any settings in the defaults when the list is reversed later).
			this.profiles.add(null);

			while (!this.profiles.isEmpty()) {
				Profile profile = this.profiles.poll();
				for (String location : getSearchLocations()) {
					if (!location.endsWith("/")) {
						// location is a filename already, so don't search for more
						// filenames
						load(location, null, profile);
					}
					else {
						for (String name : getSearchNames()) {
							load(location, name, profile);
						}
					}
				}
				this.processedProfiles.add(profile);
			}

			addConfigurationProperties(this.propertiesLoader.getPropertySources());
		}

		private Set<Profile> initializeActiveProfiles() {
			if (!this.environment.containsProperty(ACTIVE_PROFILES_PROPERTY)
					&& !this.environment.containsProperty(INCLUDE_PROFILES_PROPERTY)) {
				return Collections.emptySet();
			}
			// Any pre-existing active profiles set via property sources (e.g. System
			// properties) take precedence over those added in config files.
			Set<Profile> active = getProfiles(this.environment, "spring.profiles.active");
			Set<Profile> activeProfiles = new LinkedHashSet<>(active);
			Set<Profile> include = getProfiles(this.environment,
					"spring.profiles.include");
			activeProfiles.addAll(include);
			maybeActivateProfiles(activeProfiles);
			return activeProfiles;
		}

		/**
		 * Return the active profiles that have not been processed yet. If a profile is
		 * enabled via both {@link #ACTIVE_PROFILES_PROPERTY} and
		 * {@link ConfigurableEnvironment#addActiveProfile(String)} it needs to be
		 * filtered so that the {@link #ACTIVE_PROFILES_PROPERTY} value takes precedence.
		 * <p>
		 * Concretely, if the "cloud" profile is enabled via the environment, it will take
		 * less precedence that any profile set via the {@link #ACTIVE_PROFILES_PROPERTY}.
		 * @param initialActiveProfiles the profiles that have been enabled via
		 * {@link #ACTIVE_PROFILES_PROPERTY}
		 * @return the unprocessed active profiles from the environment to enable
		 */
		private List<Profile> getUnprocessedActiveProfiles(
				Set<Profile> initialActiveProfiles) {
			List<Profile> unprocessedActiveProfiles = new ArrayList<>();
			for (String profileName : this.environment.getActiveProfiles()) {
				Profile profile = new Profile(profileName);
				if (!initialActiveProfiles.contains(profile)) {
					unprocessedActiveProfiles.add(profile);
				}
			}
			// Reverse them so the order is the same as from getProfilesForValue()
			// (last one wins when properties are eventually resolved)
			Collections.reverse(unprocessedActiveProfiles);
			return unprocessedActiveProfiles;
		}

		private void load(String location, String name, Profile profile) {
			String group = "profile=" + (profile == null ? "" : profile);
			if (!StringUtils.hasText(name)) {
				// Try to load directly from the location
				loadIntoGroup(group, location, profile);
			}
			else {
				// Search for a file with the given name
				for (String ext : this.propertiesLoader.getAllFileExtensions()) {
					if (profile != null) {
						// Try the profile-specific file
						loadIntoGroup(group, location + name + "-" + profile + "." + ext,
								null);
						for (Profile processedProfile : this.processedProfiles) {
							if (processedProfile != null) {
								loadIntoGroup(group, location + name + "-"
										+ processedProfile + "." + ext, profile);
							}
						}
						// Sometimes people put "spring.profiles: dev" in
						// application-dev.yml (gh-340). Arguably we should try and error
						// out on that, but we can be kind and load it anyway.
						loadIntoGroup(group, location + name + "-" + profile + "." + ext,
								profile);
					}
					// Also try the profile-specific section (if any) of the normal file
					loadIntoGroup(group, location + name + "." + ext, profile);
				}
			}
		}

		private PropertySource<?> loadIntoGroup(String identifier, String location,
				Profile profile) {
			try {
				return doLoadIntoGroup(identifier, location, profile);
			}
			catch (Exception ex) {
				throw new IllegalStateException(
						"Failed to load property source from location '" + location + "'",
						ex);
			}
		}

		private PropertySource<?> doLoadIntoGroup(String identifier, String location,
				Profile profile) throws IOException {
			Resource resource = this.resourceLoader.getResource(location);
			PropertySource<?> propertySource = null;
			StringBuilder msg = new StringBuilder();
			if (resource != null && resource.exists()) {
				String name = "applicationConfig: [" + location + "]";
				String group = "applicationConfig: [" + identifier + "]";
				propertySource = this.propertiesLoader.load(resource, group, name,
						(profile == null ? null : profile.getName()));
				if (propertySource != null) {
					msg.append("Loaded ");
					handleProfileProperties(propertySource);
				}
				else {
					msg.append("Skipped (empty) ");
				}
			}
			else {
				msg.append("Skipped ");
			}
			msg.append("config file ");
			msg.append(getResourceDescription(location, resource));
			if (profile != null) {
				msg.append(" for profile ").append(profile);
			}
			if (resource == null || !resource.exists()) {
				msg.append(" resource not found");
				this.logger.trace(msg);
			}
			else {
				this.logger.debug(msg);
			}
			return propertySource;
		}

		private String getResourceDescription(String location, Resource resource) {
			String resourceDescription = "'" + location + "'";
			if (resource != null) {
				try {
					resourceDescription = String.format("'%s' (%s)",
							resource.getURI().toASCIIString(), location);
				}
				catch (IOException ex) {
					// Use the location as the description
				}
			}
			return resourceDescription;
		}

		private void handleProfileProperties(PropertySource<?> propertySource) {
			MutablePropertySources propertySources = new MutablePropertySources();
			propertySources.addFirst(propertySource);
			Set<Profile> active = getProfiles(propertySources, "spring.profiles.active");
			Set<Profile> include = getProfiles(propertySources,
					"spring.profiles.include");
			maybeActivateProfiles(active);
			addProfiles(include);
		}

		private Set<Profile> getProfiles(ConfigurableEnvironment environment,
				String name) {
			return getProfiles(environment.getPropertySources(), name);
		}

		private Set<Profile> getProfiles(PropertySources sources, String name) {
			Binder binder = new Binder(ConfigurationPropertySources.get(sources),
					new PropertySourcesPlaceholdersResolver(this.environment));
			return binder.bind(name, String[].class).map(this::asProfileSet)
					.orElse(Collections.emptySet());
		}

		private Set<Profile> asProfileSet(String[] profileNames) {
			List<Profile> profiles = new ArrayList<>();
			for (String profileName : profileNames) {
				profiles.add(new Profile(profileName));
			}
			Collections.reverse(profiles);
			return new LinkedHashSet<>(profiles);
		}

		private void maybeActivateProfiles(Set<Profile> profiles) {
			if (this.activatedProfiles) {
				if (!profiles.isEmpty()) {
					this.logger.debug("Profiles already activated, '" + profiles
							+ "' will not be applied");
				}
				return;
			}
			if (!profiles.isEmpty()) {
				addProfiles(profiles);
				this.logger.debug("Activated profiles "
						+ StringUtils.collectionToCommaDelimitedString(profiles));
				this.activatedProfiles = true;
				removeUnprocessedDefaultProfiles();
			}
		}

		private void removeUnprocessedDefaultProfiles() {
			for (Iterator<Profile> iterator = this.profiles.iterator(); iterator
					.hasNext();) {
				if (iterator.next().isDefaultProfile()) {
					iterator.remove();
				}
			}
		}

		private void addProfiles(Set<Profile> profiles) {
			for (Profile profile : profiles) {
				this.profiles.add(profile);
				if (!environmentHasActiveProfile(profile.getName())) {
					// If it's already accepted we assume the order was set
					// intentionally
					prependProfile(this.environment, profile);
				}
			}
		}

		private boolean environmentHasActiveProfile(String profile) {
			for (String activeProfile : this.environment.getActiveProfiles()) {
				if (activeProfile.equals(profile)) {
					return true;
				}
			}
			return false;
		}

		private void prependProfile(ConfigurableEnvironment environment,
				Profile profile) {
			Set<String> profiles = new LinkedHashSet<>();
			environment.getActiveProfiles(); // ensure they are initialized
			// But this one should go first (last wins in a property key clash)
			profiles.add(profile.getName());
			profiles.addAll(Arrays.asList(environment.getActiveProfiles()));
			environment.setActiveProfiles(profiles.toArray(new String[profiles.size()]));
		}

		private Set<String> getSearchLocations() {
			Set<String> locations = new LinkedHashSet<>();
			// User-configured settings take precedence, so we do them first
			if (this.environment.containsProperty(CONFIG_LOCATION_PROPERTY)) {
				for (String path : asResolvedSet(
						this.environment.getProperty(CONFIG_LOCATION_PROPERTY), null)) {
					if (!path.contains("$")) {
						path = StringUtils.cleanPath(path);
						if (!ResourceUtils.isUrl(path)) {
							path = ResourceUtils.FILE_URL_PREFIX + path;
						}
					}
					locations.add(path);
				}
			}
			locations.addAll(
					asResolvedSet(ConfigFileApplicationListener.this.searchLocations,
							DEFAULT_SEARCH_LOCATIONS));
			return locations;
		}

		private Set<String> getSearchNames() {
			if (this.environment.containsProperty(CONFIG_NAME_PROPERTY)) {
				return asResolvedSet(this.environment.getProperty(CONFIG_NAME_PROPERTY),
						null);
			}
			return asResolvedSet(ConfigFileApplicationListener.this.names, DEFAULT_NAMES);
		}

		private Set<String> asResolvedSet(String value, String fallback) {
			List<String> list = Arrays.asList(StringUtils.trimArrayElements(
					StringUtils.commaDelimitedListToStringArray(value != null
							? this.environment.resolvePlaceholders(value) : fallback)));
			Collections.reverse(list);
			return new LinkedHashSet<>(list);
		}

		private void addConfigurationProperties(MutablePropertySources sources) {
			List<PropertySource<?>> reorderedSources = new ArrayList<>();
			for (PropertySource<?> item : sources) {
				reorderedSources.add(item);
			}
			addConfigurationProperties(new LoadedPropertySources(reorderedSources));
		}

		private void addConfigurationProperties(LoadedPropertySources loadedSources) {
			MutablePropertySources existingSources = this.environment
					.getPropertySources();
			if (existingSources.contains(DEFAULT_PROPERTIES)) {
				existingSources.addBefore(DEFAULT_PROPERTIES, loadedSources);
			}
			else {
				existingSources.addLast(loadedSources);
			}
		}

	}

	private static class Profile {

		private final String name;

		private final boolean defaultProfile;

		Profile(String name) {
			this(name, false);
		}

		Profile(String name, boolean defaultProfile) {
			Assert.notNull(name, "Name must not be null");
			this.name = name;
			this.defaultProfile = defaultProfile;
		}

		public String getName() {
			return this.name;
		}

		public boolean isDefaultProfile() {
			return this.defaultProfile;
		}

		@Override
		public String toString() {
			return this.name;
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || obj.getClass() != getClass()) {
				return false;
			}
			return ((Profile) obj).name.equals(this.name);
		}

	}

	/**
	 * Holds the configuration {@link PropertySource}s as they are loaded can relocate
	 * them once configuration classes have been processed.
	 */
	static class LoadedPropertySources
			extends EnumerablePropertySource<Collection<PropertySource<?>>> {

		private final Collection<PropertySource<?>> sources;

		private final String[] names;

		LoadedPropertySources(Collection<PropertySource<?>> sources) {
			super(APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME, sources);
			this.sources = sources;
			List<String> names = new ArrayList<>();
			for (PropertySource<?> source : sources) {
				if (source instanceof EnumerablePropertySource) {
					names.addAll(Arrays.asList(
							((EnumerablePropertySource<?>) source).getPropertyNames()));
				}
			}
			this.names = names.toArray(new String[names.size()]);
		}

		@Override
		public Object getProperty(String name) {
			for (PropertySource<?> propertySource : this.sources) {
				Object value = propertySource.getProperty(name);
				if (value != null) {
					return value;
				}
			}
			return null;
		}

		public static void finishAndRelocate(MutablePropertySources propertySources) {
			String name = APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME;
			LoadedPropertySources removed = (LoadedPropertySources) propertySources
					.get(name);
			if (removed != null) {
				for (PropertySource<?> propertySource : removed.sources) {
					if (propertySource instanceof EnumerableCompositePropertySource) {
						EnumerableCompositePropertySource composite = (EnumerableCompositePropertySource) propertySource;
						for (PropertySource<?> nested : composite.getSource()) {
							propertySources.addAfter(name, nested);
							name = nested.getName();
						}
					}
					else {
						propertySources.addAfter(name, propertySource);
					}
				}
				propertySources.remove(APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME);
			}
		}

		@Override
		public String[] getPropertyNames() {
			return this.names;
		}

	}

}
