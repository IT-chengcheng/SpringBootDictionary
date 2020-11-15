/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint.web;

import org.springframework.boot.actuate.endpoint.EndpointId;

/**
 * Strategy interface used to provide a mapping between an endpoint ID and the root path
 * where it will be exposed.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
@FunctionalInterface
public interface PathMapper {

	/**
	 * Resolve the root path for the endpoint with the specified {@code endpointId}.
	 * @param endpointId the id of an endpoint
	 * @return the path of the endpoint
	 * @since 2.0.6
	 */
	default String getRootPath(EndpointId endpointId) {
		return getRootPath((endpointId != null) ? endpointId.toString() : null);
	}

	/**
	 * Resolve the root path for the endpoint with the specified {@code endpointId}.
	 * @param endpointId the id of an endpoint
	 * @return the path of the endpoint
	 * @deprecated since 2.0.6 in favor of {@link #getRootPath(EndpointId)}
	 */
	@Deprecated
	String getRootPath(String endpointId);

	/**
	 * Returns an {@link PathMapper} that uses the endpoint ID as the path.
	 * @return an {@link PathMapper} that uses the lowercase endpoint ID as the path
	 */
	static PathMapper useEndpointId() {
		return new PathMapper() {

			@Override
			@Deprecated
			public String getRootPath(String endpointId) {
				return getRootPath(EndpointId.of(endpointId));
			}

			@Override
			public String getRootPath(EndpointId endpointId) {
				return endpointId.toString();
			}

		};

	}

}
