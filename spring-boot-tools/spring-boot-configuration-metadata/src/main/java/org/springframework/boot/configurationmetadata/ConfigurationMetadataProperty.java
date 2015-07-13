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

package org.springframework.boot.configurationmetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Define a configuration property. Each property is fully identified by
 * its {@link #getId() id} who is composed of a namespace prefix (the
 * {@link ConfigurationMetadataGroup#getId() group id}), if any and the
 * {@link #getName() name} of the property.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public class ConfigurationMetadataProperty {

	private String id;

	private String name;

	private String type;

	private String description;

	private String shortDescription;

	private Object defaultValue;

	private final List<ValueHint> valueHints = new ArrayList<ValueHint>();

	private final List<ValueProvider> valueProviders = new ArrayList<ValueProvider>();

	private boolean deprecated;

	/**
	 * The full identifier of the property, in lowercase dashed form (e.g. my.group.simple-property)
	 */
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * The name of the property, in lowercase dashed form (e.g. simple-property). If this item
	 * does not belong to any group, the id is returned.
	 */
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * The class name of the data type of the property. For example, {@code java.lang.String}.
	 * <p>For consistency, the type of a primitive is specified using its wrapper counterpart,
	 * i.e. {@code boolean} becomes {@code java.lang.Boolean}. If the type holds generic
	 * information, these are provided as  well, i.e. a {@code HashMap} of String to Integer
	 * would be defined as {@code java.util.HashMap<java.lang.String,java.lang.Integer>}.
	 * <p>Note that this class may be a complex type that gets converted from a String as values
	 * are bound.
	 */
	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * A description of the property, if any. Can be multi-lines.
	 * @see #getShortDescription()
	 */
	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * A single-line, single-sentence description of this property, if any.
	 * @see #getDescription()
	 */
	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	/**
	 * The default value, if any.
	 */
	public Object getDefaultValue() {
		return this.defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * The list of well-defined values, if any. If no extra {@link ValueProvider provider} is
	 * specified, these values  are to be considered a closed-set of the available values
	 * for this item.
	 */
	public List<ValueHint> getValueHints() {
		return valueHints;
	}

	/**
	 * The value providers that are applicable to this item. Only one {@link ValueProvider} is
	 * enabled for an item: the first in the list that is supported should be used.
	 */
	public List<ValueProvider> getValueProviders() {
		return valueProviders;
	}

	/**
	 * Specify if the property is deprecated.
	 */
	public boolean isDeprecated() {
		return this.deprecated;
	}

	public void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
	}

}
