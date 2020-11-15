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

package org.springframework.boot.autoconfigure.kafka;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer.AckMode;
import org.springframework.kafka.security.jaas.KafkaJaasLoginModuleInitializer;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.kafka.transaction.KafkaTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link KafkaAutoConfiguration}.
 *
 * @author Gary Russell
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Nakul Mishra
 */
public class KafkaAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class));

	@Test
	public void consumerProperties() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class).withPropertyValues(
				"spring.kafka.bootstrap-servers=foo:1234", "spring.kafka.properties.foo=bar",
				"spring.kafka.properties.baz=qux", "spring.kafka.properties.foo.bar.baz=qux.fiz.buz",
				"spring.kafka.ssl.key-password=p1", "spring.kafka.ssl.keystore-location=classpath:ksLoc",
				"spring.kafka.ssl.keystore-password=p2", "spring.kafka.ssl.keystore-type=PKCS12",
				"spring.kafka.ssl.truststore-location=classpath:tsLoc", "spring.kafka.ssl.truststore-password=p3",
				"spring.kafka.ssl.truststore-type=PKCS12", "spring.kafka.ssl.protocol=TLSv1.2",
				"spring.kafka.consumer.auto-commit-interval=123", "spring.kafka.consumer.max-poll-records=42",
				"spring.kafka.consumer.auto-offset-reset=earliest", "spring.kafka.consumer.client-id=ccid", // test
																											// override
																											// common
				"spring.kafka.consumer.enable-auto-commit=false", "spring.kafka.consumer.fetch-max-wait=456",
				"spring.kafka.consumer.properties.fiz.buz=fix.fox", "spring.kafka.consumer.fetch-min-size=789",
				"spring.kafka.consumer.group-id=bar", "spring.kafka.consumer.heartbeat-interval=234",
				"spring.kafka.consumer.key-deserializer = org.apache.kafka.common.serialization.LongDeserializer",
				"spring.kafka.consumer.value-deserializer = org.apache.kafka.common.serialization.IntegerDeserializer")
				.run((context) -> {
					DefaultKafkaConsumerFactory<?, ?> consumerFactory = context
							.getBean(DefaultKafkaConsumerFactory.class);
					Map<String, Object> configs = consumerFactory.getConfigurationProperties();
					// common
					assertThat(configs.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG))
							.isEqualTo(Collections.singletonList("foo:1234"));
					assertThat(configs.get(SslConfigs.SSL_KEY_PASSWORD_CONFIG)).isEqualTo("p1");
					assertThat((String) configs.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG))
							.endsWith(File.separator + "ksLoc");
					assertThat(configs.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG)).isEqualTo("p2");
					assertThat(configs.get(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
					assertThat((String) configs.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
							.endsWith(File.separator + "tsLoc");
					assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG)).isEqualTo("p3");
					assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
					assertThat(configs.get(SslConfigs.SSL_PROTOCOL_CONFIG)).isEqualTo("TLSv1.2");
					// consumer
					assertThat(configs.get(ConsumerConfig.CLIENT_ID_CONFIG)).isEqualTo("ccid"); // override
					assertThat(configs.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG)).isEqualTo(Boolean.FALSE);
					assertThat(configs.get(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG)).isEqualTo(123);
					assertThat(configs.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)).isEqualTo("earliest");
					assertThat(configs.get(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG)).isEqualTo(456);
					assertThat(configs.get(ConsumerConfig.FETCH_MIN_BYTES_CONFIG)).isEqualTo(789);
					assertThat(configs.get(ConsumerConfig.GROUP_ID_CONFIG)).isEqualTo("bar");
					assertThat(configs.get(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG)).isEqualTo(234);
					assertThat(configs.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG))
							.isEqualTo(LongDeserializer.class);
					assertThat(configs.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG))
							.isEqualTo(IntegerDeserializer.class);
					assertThat(configs.get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG)).isEqualTo(42);
					assertThat(configs.get("foo")).isEqualTo("bar");
					assertThat(configs.get("baz")).isEqualTo("qux");
					assertThat(configs.get("foo.bar.baz")).isEqualTo("qux.fiz.buz");
					assertThat(configs.get("fiz.buz")).isEqualTo("fix.fox");
				});
	}

	@Test
	public void producerProperties() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class).withPropertyValues(
				"spring.kafka.clientId=cid", "spring.kafka.properties.foo.bar.baz=qux.fiz.buz",
				"spring.kafka.producer.acks=all", "spring.kafka.producer.batch-size=20",
				"spring.kafka.producer.bootstrap-servers=bar:1234", // test
				// override
				"spring.kafka.producer.buffer-memory=12345", "spring.kafka.producer.compression-type=gzip",
				"spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.LongSerializer",
				"spring.kafka.producer.retries=2", "spring.kafka.producer.properties.fiz.buz=fix.fox",
				"spring.kafka.producer.ssl.key-password=p4",
				"spring.kafka.producer.ssl.keystore-location=classpath:ksLocP",
				"spring.kafka.producer.ssl.keystore-password=p5", "spring.kafka.producer.ssl.keystore-type=PKCS12",
				"spring.kafka.producer.ssl.truststore-location=classpath:tsLocP",
				"spring.kafka.producer.ssl.truststore-password=p6", "spring.kafka.producer.ssl.truststore-type=PKCS12",
				"spring.kafka.producer.ssl.protocol=TLSv1.2",
				"spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.IntegerSerializer")
				.run((context) -> {
					DefaultKafkaProducerFactory<?, ?> producerFactory = context
							.getBean(DefaultKafkaProducerFactory.class);
					Map<String, Object> configs = producerFactory.getConfigurationProperties();
					// common
					assertThat(configs.get(ProducerConfig.CLIENT_ID_CONFIG)).isEqualTo("cid");
					// producer
					assertThat(configs.get(ProducerConfig.ACKS_CONFIG)).isEqualTo("all");
					assertThat(configs.get(ProducerConfig.BATCH_SIZE_CONFIG)).isEqualTo(20);
					assertThat(configs.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))
							.isEqualTo(Collections.singletonList("bar:1234")); // override
					assertThat(configs.get(ProducerConfig.BUFFER_MEMORY_CONFIG)).isEqualTo(12345L);
					assertThat(configs.get(ProducerConfig.COMPRESSION_TYPE_CONFIG)).isEqualTo("gzip");
					assertThat(configs.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)).isEqualTo(LongSerializer.class);
					assertThat(configs.get(SslConfigs.SSL_KEY_PASSWORD_CONFIG)).isEqualTo("p4");
					assertThat((String) configs.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG))
							.endsWith(File.separator + "ksLocP");
					assertThat(configs.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG)).isEqualTo("p5");
					assertThat(configs.get(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
					assertThat((String) configs.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
							.endsWith(File.separator + "tsLocP");
					assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG)).isEqualTo("p6");
					assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
					assertThat(configs.get(SslConfigs.SSL_PROTOCOL_CONFIG)).isEqualTo("TLSv1.2");
					assertThat(configs.get(ProducerConfig.RETRIES_CONFIG)).isEqualTo(2);
					assertThat(configs.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG))
							.isEqualTo(IntegerSerializer.class);
					assertThat(context.getBeansOfType(KafkaJaasLoginModuleInitializer.class)).isEmpty();
					assertThat(context.getBeansOfType(KafkaTransactionManager.class)).isEmpty();
					assertThat(configs.get("foo.bar.baz")).isEqualTo("qux.fiz.buz");
					assertThat(configs.get("fiz.buz")).isEqualTo("fix.fox");
				});
	}

	@Test
	public void adminProperties() {
		this.contextRunner
				.withPropertyValues("spring.kafka.clientId=cid", "spring.kafka.properties.foo.bar.baz=qux.fiz.buz",
						"spring.kafka.admin.fail-fast=true", "spring.kafka.admin.properties.fiz.buz=fix.fox",
						"spring.kafka.admin.ssl.key-password=p4",
						"spring.kafka.admin.ssl.keystore-location=classpath:ksLocP",
						"spring.kafka.admin.ssl.keystore-password=p5", "spring.kafka.admin.ssl.keystore-type=PKCS12",
						"spring.kafka.admin.ssl.truststore-location=classpath:tsLocP",
						"spring.kafka.admin.ssl.truststore-password=p6",
						"spring.kafka.admin.ssl.truststore-type=PKCS12", "spring.kafka.admin.ssl.protocol=TLSv1.2")
				.run((context) -> {
					KafkaAdmin admin = context.getBean(KafkaAdmin.class);
					Map<String, Object> configs = admin.getConfig();
					// common
					assertThat(configs.get(AdminClientConfig.CLIENT_ID_CONFIG)).isEqualTo("cid");
					// admin
					assertThat(configs.get(SslConfigs.SSL_KEY_PASSWORD_CONFIG)).isEqualTo("p4");
					assertThat((String) configs.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG))
							.endsWith(File.separator + "ksLocP");
					assertThat(configs.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG)).isEqualTo("p5");
					assertThat(configs.get(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
					assertThat((String) configs.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
							.endsWith(File.separator + "tsLocP");
					assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG)).isEqualTo("p6");
					assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG)).isEqualTo("PKCS12");
					assertThat(configs.get(SslConfigs.SSL_PROTOCOL_CONFIG)).isEqualTo("TLSv1.2");
					assertThat(context.getBeansOfType(KafkaJaasLoginModuleInitializer.class)).isEmpty();
					assertThat(configs.get("foo.bar.baz")).isEqualTo("qux.fiz.buz");
					assertThat(configs.get("fiz.buz")).isEqualTo("fix.fox");
					assertThat(KafkaTestUtils.getPropertyValue(admin, "fatalIfBrokerNotAvailable", Boolean.class))
							.isTrue();
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void listenerProperties() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.kafka.template.default-topic=testTopic",
						"spring.kafka.listener.ack-mode=MANUAL", "spring.kafka.listener.client-id=client",
						"spring.kafka.listener.ack-count=123", "spring.kafka.listener.ack-time=456",
						"spring.kafka.listener.concurrency=3", "spring.kafka.listener.poll-timeout=2000",
						"spring.kafka.listener.no-poll-threshold=2.5", "spring.kafka.listener.type=batch",
						"spring.kafka.listener.idle-event-interval=1s", "spring.kafka.listener.monitor-interval=45",
						"spring.kafka.listener.log-container-config=true", "spring.kafka.jaas.enabled=true",
						"spring.kafka.producer.transaction-id-prefix=foo", "spring.kafka.jaas.login-module=foo",
						"spring.kafka.jaas.control-flag=REQUISITE", "spring.kafka.jaas.options.useKeyTab=true")
				.run((context) -> {
					DefaultKafkaProducerFactory<?, ?> producerFactory = context
							.getBean(DefaultKafkaProducerFactory.class);
					DefaultKafkaConsumerFactory<?, ?> consumerFactory = context
							.getBean(DefaultKafkaConsumerFactory.class);
					KafkaTemplate<?, ?> kafkaTemplate = context.getBean(KafkaTemplate.class);
					KafkaListenerContainerFactory<?> kafkaListenerContainerFactory = context
							.getBean(KafkaListenerContainerFactory.class);
					assertThat(kafkaTemplate.getMessageConverter()).isInstanceOf(MessagingMessageConverter.class);
					assertThat(new DirectFieldAccessor(kafkaTemplate).getPropertyValue("producerFactory"))
							.isEqualTo(producerFactory);
					assertThat(kafkaTemplate.getDefaultTopic()).isEqualTo("testTopic");
					DirectFieldAccessor dfa = new DirectFieldAccessor(kafkaListenerContainerFactory);
					assertThat(dfa.getPropertyValue("consumerFactory")).isEqualTo(consumerFactory);
					assertThat(dfa.getPropertyValue("containerProperties.ackMode")).isEqualTo(AckMode.MANUAL);
					assertThat(dfa.getPropertyValue("containerProperties.clientId")).isEqualTo("client");
					assertThat(dfa.getPropertyValue("containerProperties.ackCount")).isEqualTo(123);
					assertThat(dfa.getPropertyValue("containerProperties.ackTime")).isEqualTo(456L);
					assertThat(dfa.getPropertyValue("concurrency")).isEqualTo(3);
					assertThat(dfa.getPropertyValue("containerProperties.pollTimeout")).isEqualTo(2000L);
					assertThat(dfa.getPropertyValue("containerProperties.noPollThreshold")).isEqualTo(2.5f);
					assertThat(dfa.getPropertyValue("containerProperties.idleEventInterval")).isEqualTo(1000L);
					assertThat(dfa.getPropertyValue("containerProperties.monitorInterval")).isEqualTo(45);
					assertThat(dfa.getPropertyValue("containerProperties.logContainerConfig")).isEqualTo(Boolean.TRUE);
					assertThat(dfa.getPropertyValue("batchListener")).isEqualTo(true);
					assertThat(context.getBeansOfType(KafkaJaasLoginModuleInitializer.class)).hasSize(1);
					KafkaJaasLoginModuleInitializer jaas = context.getBean(KafkaJaasLoginModuleInitializer.class);
					dfa = new DirectFieldAccessor(jaas);
					assertThat(dfa.getPropertyValue("loginModule")).isEqualTo("foo");
					assertThat(dfa.getPropertyValue("controlFlag"))
							.isEqualTo(AppConfigurationEntry.LoginModuleControlFlag.REQUISITE);
					assertThat(context.getBeansOfType(KafkaTransactionManager.class)).hasSize(1);
					assertThat(((Map<String, String>) dfa.getPropertyValue("options")))
							.containsExactly(entry("useKeyTab", "true"));
				});
	}

	@Test
	public void testKafkaTemplateRecordMessageConverters() {
		this.contextRunner.withUserConfiguration(MessageConverterConfiguration.class).run((context) -> {
			KafkaTemplate<?, ?> kafkaTemplate = context.getBean(KafkaTemplate.class);
			assertThat(kafkaTemplate.getMessageConverter()).isSameAs(context.getBean("myMessageConverter"));
		});
	}

	@Test
	public void testConcurrentKafkaListenerContainerFactoryWithCustomMessageConverters() {
		this.contextRunner.withUserConfiguration(MessageConverterConfiguration.class).run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory = context
					.getBean(ConcurrentKafkaListenerContainerFactory.class);
			DirectFieldAccessor dfa = new DirectFieldAccessor(kafkaListenerContainerFactory);
			assertThat(dfa.getPropertyValue("messageConverter")).isSameAs(context.getBean("myMessageConverter"));
		});
	}

	@Test
	public void testConcurrentKafkaListenerContainerFactoryWithKafkaTemplate() {
		this.contextRunner.run((context) -> {
			ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory = context
					.getBean(ConcurrentKafkaListenerContainerFactory.class);
			DirectFieldAccessor dfa = new DirectFieldAccessor(kafkaListenerContainerFactory);
			assertThat(dfa.getPropertyValue("replyTemplate")).isSameAs(context.getBean(KafkaTemplate.class));
		});
	}

	@Configuration
	protected static class TestConfiguration {

	}

	@Configuration
	protected static class MessageConverterConfiguration {

		@Bean
		public RecordMessageConverter myMessageConverter() {
			return mock(RecordMessageConverter.class);
		}

	}

}
