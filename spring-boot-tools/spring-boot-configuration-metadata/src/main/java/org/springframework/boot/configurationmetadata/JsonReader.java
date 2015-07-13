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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Read standard json metadata format as {@link ConfigurationMetadataRepository}
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
class JsonReader {

	private static final int BUFFER_SIZE = 4096;

	private static final String NEW_LINE = System.getProperty("line.separator");

	public RawConfigurationMetadata read(InputStream in, Charset charset) throws IOException {
		JSONObject json = readJson(in, charset);
		List<ConfigurationMetadataSource> groups = parseAllSources(json);
		List<ConfigurationMetadataItem> items = parseAllItems(json);
		List<ConfigurationMetadataHint> hints = parseAllHints(json);
		return new RawConfigurationMetadata(groups, items, hints);
	}

	private List<ConfigurationMetadataSource> parseAllSources(JSONObject root) {
		List<ConfigurationMetadataSource> result = new ArrayList<ConfigurationMetadataSource>();
		if (!root.has("groups")) {
			return result;
		}
		JSONArray sources = root.getJSONArray("groups");
		for (int i = 0; i < sources.length(); i++) {
			JSONObject source = sources.getJSONObject(i);
			result.add(parseSource(source));
		}
		return result;
	}

	private List<ConfigurationMetadataItem> parseAllItems(JSONObject root) {
		List<ConfigurationMetadataItem> result = new ArrayList<ConfigurationMetadataItem>();
		if (!root.has("properties")) {
			return result;
		}
		JSONArray items = root.getJSONArray("properties");
		for (int i = 0; i < items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			result.add(parseItem(item));
		}
		return result;
	}

	private List<ConfigurationMetadataHint> parseAllHints(JSONObject root) {
		List<ConfigurationMetadataHint> result = new ArrayList<ConfigurationMetadataHint>();
		if (!root.has("hints")) {
			return result;
		}
		JSONArray items = root.getJSONArray("hints");
		for (int i = 0; i < items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			result.add(parseHint(item));
		}
		return result;
	}

	private ConfigurationMetadataSource parseSource(JSONObject json) {
		ConfigurationMetadataSource source = new ConfigurationMetadataSource();
		source.setGroupId(json.getString("name"));
		source.setType(json.optString("type", null));
		String description = json.optString("description", null);
		source.setDescription(description);
		source.setShortDescription(extractShortDescription(description));
		source.setSourceType(json.optString("sourceType", null));
		source.setSourceMethod(json.optString("sourceMethod", null));
		return source;
	}

	private ConfigurationMetadataItem parseItem(JSONObject json) {
		ConfigurationMetadataItem item = new ConfigurationMetadataItem();
		item.setId(json.getString("name"));
		item.setType(json.optString("type", null));
		String description = json.optString("description", null);
		item.setDescription(description);
		item.setShortDescription(extractShortDescription(description));
		item.setDefaultValue(readItemValue(json.opt("defaultValue")));
		item.setDeprecated(json.optBoolean("deprecated", false));
		item.setSourceType(json.optString("sourceType", null));
		item.setSourceMethod(json.optString("sourceMethod", null));
		return item;
	}

	private ConfigurationMetadataHint parseHint(JSONObject json) {
		ConfigurationMetadataHint hint = new ConfigurationMetadataHint();
		hint.setId(json.getString("name"));
		if (json.has("values")) {
			JSONArray values = json.getJSONArray("values");
			for (int i = 0; i < values.length(); i++) {
				JSONObject value = values.getJSONObject(i);
				ValueHint valueHint = new ValueHint();
				valueHint.setValue(readItemValue(value.get("value")));
				String description = value.optString("description", null);
				valueHint.setDescription(description);
				valueHint.setShortDescription(extractShortDescription(description));
				hint.getValueHints().add(valueHint);
			}
		}
		if (json.has("providers")) {
			JSONArray providers = json.getJSONArray("providers");
			for (int i = 0; i < providers.length(); i++) {
				JSONObject provider = providers.getJSONObject(i);
				ValueProvider valueProvider = new ValueProvider();
				valueProvider.setName(provider.getString("name"));
				if (provider.has("parameters")) {
					JSONObject parameters = provider.getJSONObject("parameters");
					Iterator<?> keys = parameters.keys();
					while (keys.hasNext()) {
						String key = (String) keys.next();
						valueProvider.getParameters().put(key, readItemValue(parameters.get(key)));
					}
				}
				hint.getValueProviders().add(valueProvider);
			}
		}
		return hint;
	}

	private Object readItemValue(Object value) {
		if (value instanceof JSONArray) {
			JSONArray array = (JSONArray) value;
			Object[] content = new Object[array.length()];
			for (int i = 0; i < array.length(); i++) {
				content[i] = array.get(i);
			}
			return content;
		}
		return value;
	}

	static String extractShortDescription(String description) {
		if (description == null) {
			return null;
		}
		int dot = description.indexOf(".");
		if (dot != -1) {
			BreakIterator breakIterator = BreakIterator.getSentenceInstance(Locale.US);
			breakIterator.setText(description);
			String text = description.substring(breakIterator.first(), breakIterator.next()).trim();
			return removeSpaceBetweenLine(text);
		}
		else {
			String[] lines = description.split(NEW_LINE);
			return lines[0].trim();
		}
	}

	private static String removeSpaceBetweenLine(String text) {
		String[] lines = text.split(NEW_LINE);
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			sb.append(line.trim()).append(" ");
		}
		return sb.toString().trim();
	}

	private JSONObject readJson(InputStream in, Charset charset) throws IOException {
		try {
			StringBuilder out = new StringBuilder();
			InputStreamReader reader = new InputStreamReader(in, charset);
			char[] buffer = new char[BUFFER_SIZE];
			int bytesRead = -1;
			while ((bytesRead = reader.read(buffer)) != -1) {
				out.append(buffer, 0, bytesRead);
			}
			return new JSONObject(out.toString());
		}
		finally {
			in.close();
		}
	}

}
