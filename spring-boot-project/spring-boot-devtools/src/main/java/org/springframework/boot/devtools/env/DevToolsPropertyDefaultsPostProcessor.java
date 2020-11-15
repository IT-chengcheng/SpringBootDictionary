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

package org.springframework.boot.devtools.env;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.devtools.restart.Restarter;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * {@link EnvironmentPostProcessor} to add properties that make sense when working at
 * development time.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 1.3.0
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class DevToolsPropertyDefaultsPostProcessor implements EnvironmentPostProcessor {

	private static final Map<String, Object> PROPERTIES;

	static {
		Map<String, Object> devToolsProperties = new HashMap<>();
		devToolsProperties.put("spring.thymeleaf.cache", "false");
		devToolsProperties.put("spring.freemarker.cache", "false");
		devToolsProperties.put("spring.groovy.template.cache", "false");
		devToolsProperties.put("spring.mustache.cache", "false");
		devToolsProperties.put("server.servlet.session.persistent", "true");
		devToolsProperties.put("spring.h2.console.enabled", "true");
		devToolsProperties.put("spring.resources.cache.period", "0");
		devToolsProperties.put("spring.resources.chain.cache", "false");
		devToolsProperties.put("spring.template.provider.cache", "false");
		devToolsProperties.put("spring.mvc.log-resolved-exception", "true");
		devToolsProperties.put("server.servlet.jsp.init-parameters.development", "true");
		devToolsProperties.put("spring.reactor.stacktrace-mode.enabled", "true");
		PROPERTIES = Collections.unmodifiableMap(devToolsProperties);
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (isLocalApplication(environment) && canAddProperties(environment)) {
			PropertySource<?> propertySource = new MapPropertySource("refresh", PROPERTIES);
			environment.getPropertySources().addLast(propertySource);
		}
	}

	private boolean isLocalApplication(ConfigurableEnvironment environment) {
		return environment.getPropertySources().get("remoteUrl") == null;
	}

	private boolean canAddProperties(Environment environment) {
		return isRestarterInitialized() || isRemoteRestartEnabled(environment);
	}

	private boolean isRestarterInitialized() {
		try {
			Restarter restarter = Restarter.getInstance();
			return (restarter != null && restarter.getInitialUrls() != null);
		}
		catch (Exception ex) {
			return false;
		}
	}

	private boolean isRemoteRestartEnabled(Environment environment) {
		return environment.containsProperty("spring.devtools.remote.secret");
	}

}
