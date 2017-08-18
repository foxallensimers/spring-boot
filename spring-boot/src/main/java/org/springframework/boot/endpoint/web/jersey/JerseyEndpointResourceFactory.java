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

package org.springframework.boot.endpoint.web.jersey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.Resource.Builder;

import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.OperationInvoker;
import org.springframework.boot.endpoint.ParameterMappingException;
import org.springframework.boot.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.endpoint.web.Link;
import org.springframework.boot.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.endpoint.web.WebEndpointOperation;
import org.springframework.boot.endpoint.web.WebEndpointResponse;
import org.springframework.util.CollectionUtils;

/**
 * A factory for creating Jersey {@link Resource Resources} for web endpoint operations.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class JerseyEndpointResourceFactory {

	private final EndpointLinksResolver endpointLinksResolver = new EndpointLinksResolver();

	/**
	 * Creates {@link Resource Resources} for the operations of the given
	 * {@code webEndpoints}.
	 * @param endpointPath the path beneath which all endpoints should be mapped
	 * @param webEndpoints the web endpoints
	 * @return the resources for the operations
	 */
	public Collection<Resource> createEndpointResources(String endpointPath,
			Collection<EndpointInfo<WebEndpointOperation>> webEndpoints) {
		List<Resource> resources = new ArrayList<>();
		webEndpoints.stream()
				.flatMap((endpointInfo) -> endpointInfo.getOperations().stream())
				.map((operation) -> createResource(endpointPath, operation))
				.forEach(resources::add);
		resources.add(createEndpointLinksResource(endpointPath, webEndpoints));
		return resources;
	}

	private Resource createResource(String endpointPath, WebEndpointOperation operation) {
		OperationRequestPredicate requestPredicate = operation.getRequestPredicate();
		Builder resourceBuilder = Resource.builder()
				.path(endpointPath + "/" + requestPredicate.getPath());
		resourceBuilder.addMethod(requestPredicate.getHttpMethod().name())
				.consumes(toStringArray(requestPredicate.getConsumes()))
				.produces(toStringArray(requestPredicate.getProduces()))
				.handledBy(new EndpointInvokingInflector(operation.getOperationInvoker(),
						!requestPredicate.getConsumes().isEmpty()));
		return resourceBuilder.build();
	}

	private String[] toStringArray(Collection<String> collection) {
		return collection.toArray(new String[collection.size()]);
	}

	private Resource createEndpointLinksResource(String endpointPath,
			Collection<EndpointInfo<WebEndpointOperation>> webEndpoints) {
		Builder resourceBuilder = Resource.builder().path(endpointPath);
		resourceBuilder.addMethod("GET").handledBy(
				new EndpointLinksInflector(webEndpoints, this.endpointLinksResolver));
		return resourceBuilder.build();
	}

	private static final class EndpointInvokingInflector
			implements Inflector<ContainerRequestContext, Object> {

		private final OperationInvoker operationInvoker;

		private final boolean readBody;

		private EndpointInvokingInflector(OperationInvoker operationInvoker,
				boolean readBody) {
			this.operationInvoker = operationInvoker;
			this.readBody = readBody;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Response apply(ContainerRequestContext data) {
			Map<String, Object> arguments = new HashMap<>();
			if (this.readBody) {
				Map<String, Object> body = ((ContainerRequest) data)
						.readEntity(Map.class);
				if (body != null) {
					arguments.putAll(body);
				}
			}
			arguments.putAll(extractPathParameters(data));
			arguments.putAll(extractQueryParameters(data));
			try {
				return convertToJaxRsResponse(this.operationInvoker.invoke(arguments),
						data.getRequest().getMethod());
			}
			catch (ParameterMappingException ex) {
				return Response.status(Status.BAD_REQUEST).build();
			}
		}

		private Map<String, Object> extractPathParameters(
				ContainerRequestContext requestContext) {
			return extract(requestContext.getUriInfo().getPathParameters());
		}

		private Map<String, Object> extractQueryParameters(
				ContainerRequestContext requestContext) {
			return extract(requestContext.getUriInfo().getQueryParameters());
		}

		private Map<String, Object> extract(
				MultivaluedMap<String, String> multivaluedMap) {
			Map<String, Object> result = new HashMap<>();
			multivaluedMap.forEach((name, values) -> {
				if (!CollectionUtils.isEmpty(values)) {
					result.put(name, values.size() == 1 ? values.get(0) : values);
				}
			});
			return result;
		}

		private Response convertToJaxRsResponse(Object response, String httpMethod) {
			if (response == null) {
				return Response.status(HttpMethod.GET.equals(httpMethod)
						? Status.NOT_FOUND : Status.NO_CONTENT).build();
			}
			try {
				if (!(response instanceof WebEndpointResponse)) {
					return Response.status(Status.OK).entity(convertIfNecessary(response))
							.build();
				}
				WebEndpointResponse<?> webEndpointResponse = (WebEndpointResponse<?>) response;
				return Response.status(webEndpointResponse.getStatus())
						.entity(convertIfNecessary(webEndpointResponse.getBody()))
						.build();
			}
			catch (IOException ex) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}

		private Object convertIfNecessary(Object body) throws IOException {
			if (body instanceof org.springframework.core.io.Resource) {
				return ((org.springframework.core.io.Resource) body).getInputStream();
			}
			return body;
		}

	}

	private static final class EndpointLinksInflector
			implements Inflector<ContainerRequestContext, Response> {

		private final Collection<EndpointInfo<WebEndpointOperation>> endpoints;

		private final EndpointLinksResolver linksResolver;

		private EndpointLinksInflector(
				Collection<EndpointInfo<WebEndpointOperation>> endpoints,
				EndpointLinksResolver linksResolver) {
			this.endpoints = endpoints;
			this.linksResolver = linksResolver;
		}

		@Override
		public Response apply(ContainerRequestContext request) {
			Map<String, Link> links = this.linksResolver.resolveLinks(this.endpoints,
					request.getUriInfo().getAbsolutePath().toString());
			return Response.ok(Collections.singletonMap("_links", links)).build();
		}

	}

}
