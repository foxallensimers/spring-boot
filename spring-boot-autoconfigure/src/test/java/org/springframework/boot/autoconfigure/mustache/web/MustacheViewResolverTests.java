/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.mustache.web;

import java.io.InputStream;
import java.util.Locale;

import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.View;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link MustacheViewResolver}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class MustacheViewResolverTests {

	private MustacheViewResolver resolver = new MustacheViewResolver();

	@Before
	public void init() {
		this.resolver.setApplicationContext(new StaticWebApplicationContext());
		this.resolver.setServletContext(new MockServletContext());
		this.resolver.setPrefix("classpath:/mustache-templates/");
		this.resolver.setSuffix(".html");
	}

	@Test
	public void resolveNonExistent() throws Exception {
		assertNull(this.resolver.resolveViewName("bar", null));
	}

	@Test
	public void resolveNullLocale() throws Exception {
		assertNotNull(this.resolver.resolveViewName("foo", null));
	}

	@Test
	public void resolveDefaultLocale() throws Exception {
		assertNotNull(this.resolver.resolveViewName("foo", Locale.US));
	}

	@Test
	public void resolveDoubleLocale() throws Exception {
		assertNotNull(this.resolver.resolveViewName("foo", Locale.CANADA_FRENCH));
	}

	@Test
	public void resolveTripleLocale() throws Exception {
		assertNotNull(this.resolver.resolveViewName("foo", new Locale("en", "GB", "cy")));
	}

	@Test
	public void resolveSpecificLocale() throws Exception {
		assertNotNull(this.resolver.resolveViewName("foo", new Locale("de")));
	}

	@Test
	public void setsContentType() throws Exception {
		this.resolver.setContentType("application/octet-stream");
		View view = this.resolver.resolveViewName("foo", null);
		assertThat(view.getContentType(), equalTo("application/octet-stream"));

	}

	@Test
	public void templateResourceInputStreamIsClosed() throws Exception {
		final Resource resource = mock(Resource.class);
		given(resource.exists()).willReturn(true);
		InputStream inputStream = new ByteArrayInputStream(new byte[0]);
		InputStream spyInputStream = spy(inputStream);
		given(resource.getInputStream()).willReturn(spyInputStream);
		this.resolver = new MustacheViewResolver();
		this.resolver.setApplicationContext(new StaticWebApplicationContext() {

			@Override
			public Resource getResource(String location) {
				return resource;
			}

		});
		this.resolver.loadView("foo", null);
		verify(spyInputStream).close();
	}

}
