/*
 * Copyright 2013-2016 the original author or authors.
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
package org.springframework.hateoas.hal;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.hal.DefaultCurieProvider.Curie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Unit tests for {@link DefaultCurieProvider}.
 * 
 * @author Oliver Gierke
 */
public class DefaultCurieProviderUnitTest {

	private static final UriTemplate URI_TEMPLATE = new UriTemplate("http://localhost:8080/rels/{rel}");

	CurieProvider provider = new DefaultCurieProvider("acme", URI_TEMPLATE);

	@Test(expected = IllegalArgumentException.class)
	public void preventsNullCurieName() {
		new DefaultCurieProvider(null, URI_TEMPLATE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void preventsEmptyCurieName() {
		new DefaultCurieProvider("", URI_TEMPLATE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void preventsNullUriTemplateName() {
		new DefaultCurieProvider("acme", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void preventsUriTemplateWithoutVariable() {
		new DefaultCurieProvider("acme", new UriTemplate("http://localhost:8080/rels"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void preventsUriTemplateWithMoreThanOneVariable() {
		new DefaultCurieProvider("acme", new UriTemplate("http://localhost:8080/rels/{rel}/{another}"));
	}

	@Test
	public void doesNotPrefixIanaRels() {
		assertThat(provider.getNamespacedRelFrom(new Link("http://amazon.com")), is("self"));
	}

	@Test
	public void prefixesNormalRels() {
		assertThat(provider.getNamespacedRelFrom(new Link("http://amazon.com", "book")), is("acme:book"));
	}

	@Test
	public void doesNotPrefixQualifiedRels() {
		assertThat(provider.getNamespacedRelFrom(new Link("http://amazon.com", "custom:rel")), is("custom:rel"));
	}

	/**
	 * @see #229
	 */
	@Test
	public void doesNotPrefixIanaRelsForRelAsString() {
		assertThat(provider.getNamespacedRelFor("self"), is("self"));
	}

	/**
	 * @see #229
	 */
	@Test
	public void prefixesNormalRelsForRelAsString() {
		assertThat(provider.getNamespacedRelFor("book"), is("acme:book"));
	}

	/**
	 * @see #229
	 */
	@Test
	public void doesNotPrefixQualifiedRelsForRelAsString() {
		assertThat(provider.getNamespacedRelFor("custom:rel"), is("custom:rel"));
	}

	/**
	 * @see #363
	 */
	@Test
	public void configuresMultipleCuriesWithoutDefaultCorrectly() {

		DefaultCurieProvider provider = new DefaultCurieProvider(getCuries());

		assertThat(provider.getCurieInformation(new Links()), hasSize(2));
		assertThat(provider.getNamespacedRelFor("some"), is("some"));
	}

	/**
	 * @see #363
	 */
	@Test
	public void configuresMultipleCuriesWithDefaultCorrectly() {

		DefaultCurieProvider provider = new DefaultCurieProvider(getCuries(), "foo");

		assertThat(provider.getCurieInformation(new Links()), hasSize(2));
		assertThat(provider.getNamespacedRelFor("some"), is("foo:some"));
	}

	/**
	 * #421
	 */
	@Test
	public void expandsNonAbsoluteUriWithApplicationUri() {

		DefaultCurieProvider provider = new DefaultCurieProvider("name", new UriTemplate("/docs/{rel}"));

		MockHttpServletRequest request = new MockHttpServletRequest();
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);

		Links links = new Links(new Link("http://localhost", "name:foo"));

		Collection<? extends Object> curies = provider.getCurieInformation(links);
		assertThat(curies, hasSize(1));

		Object curie = curies.iterator().next();
		assertThat(curie, is(instanceOf(Curie.class)));

		assertThat(((Curie) curie).getHref(), startsWith("http://localhost"));
	}

	private static Map<String, UriTemplate> getCuries() {

		Map<String, UriTemplate> curies = new HashMap<String, UriTemplate>(2);
		curies.put("foo", new UriTemplate("/foo/{rel}"));
		curies.put("bar", new UriTemplate("/bar/{rel}"));

		return curies;
	}
}
