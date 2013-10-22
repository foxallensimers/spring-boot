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

package org.springframework.boot.cli.compiler;

import groovy.grape.Grape;
import groovy.grape.GrapeEngine;

import java.lang.reflect.Field;

/**
 * @author Andy Wilkinson
 */
public class GrapeEngineInstaller {

	private final GrapeEngine grapeEngine;

	public GrapeEngineInstaller(GrapeEngine grapeEngine) {
		this.grapeEngine = grapeEngine;
	}

	public void install() {
		synchronized (Grape.class) {
			try {
				Field instanceField = Grape.class.getDeclaredField("instance");
				instanceField.setAccessible(true);

				GrapeEngine existingGrapeEngine = (GrapeEngine) instanceField.get(null);

				if (existingGrapeEngine == null) {
					instanceField.set(null, this.grapeEngine);
				}
				else if (!existingGrapeEngine.getClass().equals(
						this.grapeEngine.getClass())) {
					throw new IllegalStateException(
							"Another GrapeEngine of a different type has already been initialized");
				}
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to install GrapeEngine", ex);
			}
		}
	}
}
