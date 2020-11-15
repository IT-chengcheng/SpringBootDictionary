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

package org.springframework.boot.web.embedded.netty;

import reactor.ipc.netty.http.server.HttpServerOptions;

/**
 * Callback interface that can be used to customize a Reactor Netty server builder.
 *
 * @author Brian Clozel
 * @see NettyReactiveWebServerFactory
 * @since 2.0.0
 */
@FunctionalInterface
public interface NettyServerCustomizer {

	/**
	 * Customize the Netty web server.
	 * @param builder the server options builder to customize
	 */
	void customize(HttpServerOptions.Builder builder);

}
