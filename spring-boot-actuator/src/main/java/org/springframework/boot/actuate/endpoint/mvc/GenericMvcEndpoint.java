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

package org.springframework.boot.actuate.endpoint.mvc;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Dave Syer
 */
@FrameworkEndpoint
public class GenericMvcEndpoint implements MvcEndpoint {

	private Endpoint<?> delegate;

	public GenericMvcEndpoint(Endpoint<?> delegate) {
		this.delegate = delegate;
	}

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public Object invoke() {
		return this.delegate.invoke();
	}

	@Override
	public String getPath() {
		return this.delegate.getPath();
	}

}
