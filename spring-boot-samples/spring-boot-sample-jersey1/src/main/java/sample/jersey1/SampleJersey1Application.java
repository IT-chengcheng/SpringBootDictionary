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

package sample.jersey1;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.sun.jersey.spi.container.servlet.ServletContainer;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Path("/")
public class SampleJersey1Application {

	@GET
	@Produces("text/plain")
	public String hello() {
		return "Hello World";
	}

	@Bean
	// Not needed if Spring Web MVC is also present on classpath
	public TomcatServletWebServerFactory webServerFactory() {
		return new TomcatServletWebServerFactory();
	}

	@Bean
	public FilterRegistrationBean<ServletContainer> jersey() {
		FilterRegistrationBean<ServletContainer> bean = new FilterRegistrationBean<>();
		bean.setFilter(new ServletContainer());
		bean.addInitParameter("com.sun.jersey.config.property.packages", "com.sun.jersey;sample.jersey1");
		return bean;
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(SampleJersey1Application.class).web(WebApplicationType.SERVLET).run(args);
	}

}
