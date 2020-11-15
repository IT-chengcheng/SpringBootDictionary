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

package org.springframework.boot.autoconfigure.mongo;

import java.util.concurrent.TimeUnit;

import com.mongodb.ReadPreference;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.connection.AsynchronousSocketChannelStreamFactoryFactory;
import com.mongodb.connection.SocketSettings;
import com.mongodb.connection.StreamFactory;
import com.mongodb.connection.StreamFactoryFactory;
import com.mongodb.connection.netty.NettyStreamFactoryFactory;
import com.mongodb.reactivestreams.client.MongoClient;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MongoReactiveAutoConfiguration}.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
public class MongoReactiveAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MongoReactiveAutoConfiguration.class));

	@Test
	public void clientExists() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(MongoClient.class));
	}

	@Test
	public void optionsAdded() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.host:localhost")
				.withUserConfiguration(OptionsConfig.class)
				.run((context) -> assertThat(context.getBean(MongoClient.class).getSettings().getSocketSettings()
						.getReadTimeout(TimeUnit.SECONDS)).isEqualTo(300));
	}

	@Test
	public void optionsAddedButNoHost() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.uri:mongodb://localhost/test")
				.withUserConfiguration(OptionsConfig.class)
				.run((context) -> assertThat(context.getBean(MongoClient.class).getSettings().getReadPreference())
						.isEqualTo(ReadPreference.nearest()));
	}

	@Test
	public void optionsSslConfig() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.uri:mongodb://localhost/test")
				.withUserConfiguration(SslOptionsConfig.class).run((context) -> {
					assertThat(context).hasSingleBean(MongoClient.class);
					MongoClient mongo = context.getBean(MongoClient.class);
					MongoClientSettings settings = mongo.getSettings();
					assertThat(settings.getApplicationName()).isEqualTo("test-config");
					assertThat(settings.getStreamFactoryFactory()).isSameAs(context.getBean("myStreamFactoryFactory"));
				});
	}

	@Test
	public void nettyStreamFactoryFactoryIsConfiguredAutomatically() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(MongoClient.class);
			assertThat(context.getBean(MongoClient.class).getSettings().getStreamFactoryFactory())
					.isInstanceOf(NettyStreamFactoryFactory.class);
		});
	}

	@Test
	public void customizerOverridesAutoConfig() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.uri:mongodb://localhost/test?appname=auto-config")
				.withUserConfiguration(SimpleCustomizerConfig.class).run((context) -> {
					assertThat(context).hasSingleBean(MongoClient.class);
					MongoClient client = context.getBean(MongoClient.class);
					assertThat(client.getSettings().getApplicationName()).isEqualTo("overridden-name");
					assertThat(client.getSettings().getStreamFactoryFactory())
							.isEqualTo(SimpleCustomizerConfig.streamFactoryFactory);
				});
	}

	@Configuration
	static class OptionsConfig {

		@Bean
		public MongoClientSettings mongoClientSettings() {
			return MongoClientSettings.builder().readPreference(ReadPreference.nearest())
					.socketSettings(SocketSettings.builder().readTimeout(300, TimeUnit.SECONDS).build()).build();
		}

	}

	@Configuration
	static class SslOptionsConfig {

		@Bean
		public MongoClientSettings mongoClientSettings() {
			return MongoClientSettings.builder().applicationName("test-config")
					.streamFactoryFactory(myStreamFactoryFactory()).build();
		}

		@Bean
		public StreamFactoryFactory myStreamFactoryFactory() {
			StreamFactoryFactory streamFactoryFactory = mock(StreamFactoryFactory.class);
			given(streamFactoryFactory.create(any(), any())).willReturn(mock(StreamFactory.class));
			return streamFactoryFactory;
		}

	}

	@Configuration
	static class SimpleCustomizerConfig {

		private static final StreamFactoryFactory streamFactoryFactory = new AsynchronousSocketChannelStreamFactoryFactory.Builder()
				.build();

		@Bean
		public MongoClientSettingsBuilderCustomizer customizer() {
			return (clientSettingsBuilder) -> clientSettingsBuilder.applicationName("overridden-name")
					.streamFactoryFactory(streamFactoryFactory);
		}

	}

}
