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

package org.springframework.boot.web.client;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.http.client.config.RequestConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RestTemplateBuilder}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class RestTemplateBuilderTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private RestTemplateBuilder builder = new RestTemplateBuilder();

	@Mock
	private HttpMessageConverter<Object> messageConverter;

	@Mock
	private ClientHttpRequestInterceptor interceptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void createWhenCustomizersAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Customizers must not be null");
		RestTemplateCustomizer[] customizers = null;
		new RestTemplateBuilder(customizers);
	}

	@Test
	public void createWithCustomizersShouldApplyCustomizers() {
		RestTemplateCustomizer customizer = mock(RestTemplateCustomizer.class);
		RestTemplate template = new RestTemplateBuilder(customizer).build();
		verify(customizer).customize(template);
	}

	@Test
	public void buildShouldDetectRequestFactory() {
		RestTemplate restTemplate = this.builder.build();
		assertThat(restTemplate.getRequestFactory()).isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
	}

	@Test
	public void detectRequestFactoryWhenFalseShouldDisableDetection() {
		RestTemplate restTemplate = this.builder.detectRequestFactory(false).build();
		assertThat(restTemplate.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);
	}

	@Test
	public void rootUriShouldApply() {
		RestTemplate restTemplate = this.builder.rootUri("https://example.com").build();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		server.expect(requestTo("https://example.com/hello")).andRespond(withSuccess());
		restTemplate.getForEntity("/hello", String.class);
		server.verify();
	}

	@Test
	public void rootUriShouldApplyAfterUriTemplateHandler() {
		UriTemplateHandler uriTemplateHandler = mock(UriTemplateHandler.class);
		RestTemplate template = this.builder.uriTemplateHandler(uriTemplateHandler).rootUri("https://example.com")
				.build();
		UriTemplateHandler handler = template.getUriTemplateHandler();
		handler.expand("/hello");
		assertThat(handler).isInstanceOf(RootUriTemplateHandler.class);
		verify(uriTemplateHandler).expand("https://example.com/hello");
	}

	@Test
	public void messageConvertersWhenConvertersAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("MessageConverters must not be null");
		this.builder.messageConverters((HttpMessageConverter<?>[]) null);
	}

	@Test
	public void messageConvertersCollectionWhenConvertersAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("MessageConverters must not be null");
		this.builder.messageConverters((Set<HttpMessageConverter<?>>) null);
	}

	@Test
	public void messageConvertersShouldApply() {
		RestTemplate template = this.builder.messageConverters(this.messageConverter).build();
		assertThat(template.getMessageConverters()).containsOnly(this.messageConverter);
	}

	@Test
	public void messageConvertersShouldReplaceExisting() {
		RestTemplate template = this.builder.messageConverters(new ResourceHttpMessageConverter())
				.messageConverters(Collections.singleton(this.messageConverter)).build();
		assertThat(template.getMessageConverters()).containsOnly(this.messageConverter);
	}

	@Test
	public void additionalMessageConvertersWhenConvertersAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("MessageConverters must not be null");
		this.builder.additionalMessageConverters((HttpMessageConverter<?>[]) null);
	}

	@Test
	public void additionalMessageConvertersCollectionWhenConvertersAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("MessageConverters must not be null");
		this.builder.additionalMessageConverters((Set<HttpMessageConverter<?>>) null);
	}

	@Test
	public void additionalMessageConvertersShouldAddToExisting() {
		HttpMessageConverter<?> resourceConverter = new ResourceHttpMessageConverter();
		RestTemplate template = this.builder.messageConverters(resourceConverter)
				.additionalMessageConverters(this.messageConverter).build();
		assertThat(template.getMessageConverters()).containsOnly(resourceConverter, this.messageConverter);
	}

	@Test
	public void defaultMessageConvertersShouldSetDefaultList() {
		RestTemplate template = new RestTemplate(Collections.singletonList(new StringHttpMessageConverter()));
		this.builder.defaultMessageConverters().configure(template);
		assertThat(template.getMessageConverters()).hasSameSizeAs(new RestTemplate().getMessageConverters());
	}

	@Test
	public void defaultMessageConvertersShouldClearExisting() {
		RestTemplate template = new RestTemplate(Collections.singletonList(new StringHttpMessageConverter()));
		this.builder.additionalMessageConverters(this.messageConverter).defaultMessageConverters().configure(template);
		assertThat(template.getMessageConverters()).hasSameSizeAs(new RestTemplate().getMessageConverters());
	}

	@Test
	public void interceptorsWhenInterceptorsAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("interceptors must not be null");
		this.builder.interceptors((ClientHttpRequestInterceptor[]) null);
	}

	@Test
	public void interceptorsCollectionWhenInterceptorsAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("interceptors must not be null");
		this.builder.interceptors((Set<ClientHttpRequestInterceptor>) null);
	}

	@Test
	public void interceptorsShouldApply() {
		RestTemplate template = this.builder.interceptors(this.interceptor).build();
		assertThat(template.getInterceptors()).containsOnly(this.interceptor);
	}

	@Test
	public void interceptorsShouldReplaceExisting() {
		RestTemplate template = this.builder.interceptors(mock(ClientHttpRequestInterceptor.class))
				.interceptors(Collections.singleton(this.interceptor)).build();
		assertThat(template.getInterceptors()).containsOnly(this.interceptor);
	}

	@Test
	public void additionalInterceptorsWhenInterceptorsAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("interceptors must not be null");
		this.builder.additionalInterceptors((ClientHttpRequestInterceptor[]) null);
	}

	@Test
	public void additionalInterceptorsCollectionWhenInterceptorsAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("interceptors must not be null");
		this.builder.additionalInterceptors((Set<ClientHttpRequestInterceptor>) null);
	}

	@Test
	public void additionalInterceptorsShouldAddToExisting() {
		ClientHttpRequestInterceptor interceptor = mock(ClientHttpRequestInterceptor.class);
		RestTemplate template = this.builder.interceptors(interceptor).additionalInterceptors(this.interceptor).build();
		assertThat(template.getInterceptors()).containsOnly(interceptor, this.interceptor);
	}

	@Test
	public void requestFactoryClassWhenFactoryIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("RequestFactory must not be null");
		this.builder.requestFactory((Class<ClientHttpRequestFactory>) null);
	}

	@Test
	public void requestFactoryClassShouldApply() {
		RestTemplate template = this.builder.requestFactory(SimpleClientHttpRequestFactory.class).build();
		assertThat(template.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);
	}

	@Test
	public void requestFactoryPackagePrivateClassShouldApply() {
		RestTemplate template = this.builder.requestFactory(TestClientHttpRequestFactory.class).build();
		assertThat(template.getRequestFactory()).isInstanceOf(TestClientHttpRequestFactory.class);
	}

	@Test
	public void requestFactoryWhenSupplierIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("RequestFactory Supplier must not be null");
		this.builder.requestFactory((Supplier<ClientHttpRequestFactory>) null);
	}

	@Test
	public void requestFactoryShouldApply() {
		ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
		RestTemplate template = this.builder.requestFactory(() -> requestFactory).build();
		assertThat(template.getRequestFactory()).isSameAs(requestFactory);
	}

	@Test
	public void uriTemplateHandlerWhenHandlerIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("UriTemplateHandler must not be null");
		this.builder.uriTemplateHandler(null);
	}

	@Test
	public void uriTemplateHandlerShouldApply() {
		UriTemplateHandler uriTemplateHandler = mock(UriTemplateHandler.class);
		RestTemplate template = this.builder.uriTemplateHandler(uriTemplateHandler).build();
		assertThat(template.getUriTemplateHandler()).isSameAs(uriTemplateHandler);
	}

	@Test
	public void errorHandlerWhenHandlerIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ErrorHandler must not be null");
		this.builder.errorHandler(null);
	}

	@Test
	public void errorHandlerShouldApply() {
		ResponseErrorHandler errorHandler = mock(ResponseErrorHandler.class);
		RestTemplate template = this.builder.errorHandler(errorHandler).build();
		assertThat(template.getErrorHandler()).isSameAs(errorHandler);
	}

	@Test
	public void basicAuthorizationShouldApply() {
		RestTemplate template = this.builder.basicAuthorization("spring", "boot").build();
		ClientHttpRequestInterceptor interceptor = template.getInterceptors().get(0);
		assertThat(interceptor).isInstanceOf(BasicAuthorizationInterceptor.class);
		assertThat(interceptor).extracting("username").containsExactly("spring");
		assertThat(interceptor).extracting("password").containsExactly("boot");
	}

	@Test
	public void customizersWhenCustomizersAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("RestTemplateCustomizers must not be null");
		this.builder.customizers((RestTemplateCustomizer[]) null);
	}

	@Test
	public void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("RestTemplateCustomizers must not be null");
		this.builder.customizers((Set<RestTemplateCustomizer>) null);
	}

	@Test
	public void customizersShouldApply() {
		RestTemplateCustomizer customizer = mock(RestTemplateCustomizer.class);
		RestTemplate template = this.builder.customizers(customizer).build();
		verify(customizer).customize(template);
	}

	@Test
	public void customizersShouldBeAppliedLast() {
		RestTemplate template = spy(new RestTemplate());
		this.builder.additionalCustomizers(
				(restTemplate) -> verify(restTemplate).setRequestFactory(any(ClientHttpRequestFactory.class)));
		this.builder.configure(template);
	}

	@Test
	public void customizersShouldReplaceExisting() {
		RestTemplateCustomizer customizer1 = mock(RestTemplateCustomizer.class);
		RestTemplateCustomizer customizer2 = mock(RestTemplateCustomizer.class);
		RestTemplate template = this.builder.customizers(customizer1).customizers(Collections.singleton(customizer2))
				.build();
		verifyZeroInteractions(customizer1);
		verify(customizer2).customize(template);
	}

	@Test
	public void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("RestTemplateCustomizers must not be null");
		this.builder.additionalCustomizers((RestTemplateCustomizer[]) null);
	}

	@Test
	public void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("RestTemplateCustomizers must not be null");
		this.builder.additionalCustomizers((Set<RestTemplateCustomizer>) null);
	}

	@Test
	public void additionalCustomizersShouldAddToExisting() {
		RestTemplateCustomizer customizer1 = mock(RestTemplateCustomizer.class);
		RestTemplateCustomizer customizer2 = mock(RestTemplateCustomizer.class);
		RestTemplate template = this.builder.customizers(customizer1).additionalCustomizers(customizer2).build();
		verify(customizer1).customize(template);
		verify(customizer2).customize(template);
	}

	@Test
	public void buildShouldReturnRestTemplate() {
		RestTemplate template = this.builder.build();
		assertThat(template.getClass()).isEqualTo(RestTemplate.class);
	}

	@Test
	public void buildClassShouldReturnClassInstance() {
		RestTemplateSubclass template = this.builder.build(RestTemplateSubclass.class);
		assertThat(template.getClass()).isEqualTo(RestTemplateSubclass.class);
	}

	@Test
	public void configureShouldApply() {
		RestTemplate template = new RestTemplate();
		this.builder.configure(template);
		assertThat(template.getRequestFactory()).isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
	}

	@Test
	public void connectTimeoutCanBeConfiguredOnHttpComponentsRequestFactory() {
		ClientHttpRequestFactory requestFactory = this.builder
				.requestFactory(HttpComponentsClientHttpRequestFactory.class).setConnectTimeout(1234).build()
				.getRequestFactory();
		assertThat(((RequestConfig) ReflectionTestUtils.getField(requestFactory, "requestConfig")).getConnectTimeout())
				.isEqualTo(1234);
	}

	@Test
	public void readTimeoutCanBeConfiguredOnHttpComponentsRequestFactory() {
		ClientHttpRequestFactory requestFactory = this.builder
				.requestFactory(HttpComponentsClientHttpRequestFactory.class).setReadTimeout(1234).build()
				.getRequestFactory();
		assertThat(((RequestConfig) ReflectionTestUtils.getField(requestFactory, "requestConfig")).getSocketTimeout())
				.isEqualTo(1234);
	}

	@Test
	public void connectTimeoutCanBeConfiguredOnSimpleRequestFactory() {
		ClientHttpRequestFactory requestFactory = this.builder.requestFactory(SimpleClientHttpRequestFactory.class)
				.setConnectTimeout(1234).build().getRequestFactory();
		assertThat(ReflectionTestUtils.getField(requestFactory, "connectTimeout")).isEqualTo(1234);
	}

	@Test
	public void readTimeoutCanBeConfiguredOnSimpleRequestFactory() {
		ClientHttpRequestFactory requestFactory = this.builder.requestFactory(SimpleClientHttpRequestFactory.class)
				.setReadTimeout(1234).build().getRequestFactory();
		assertThat(ReflectionTestUtils.getField(requestFactory, "readTimeout")).isEqualTo(1234);
	}

	@Test
	public void connectTimeoutCanBeConfiguredOnOkHttp3RequestFactory() {
		ClientHttpRequestFactory requestFactory = this.builder.requestFactory(OkHttp3ClientHttpRequestFactory.class)
				.setConnectTimeout(1234).build().getRequestFactory();
		assertThat(
				ReflectionTestUtils.getField(ReflectionTestUtils.getField(requestFactory, "client"), "connectTimeout"))
						.isEqualTo(1234);
	}

	@Test
	public void readTimeoutCanBeConfiguredOnOkHttp3RequestFactory() {
		ClientHttpRequestFactory requestFactory = this.builder.requestFactory(OkHttp3ClientHttpRequestFactory.class)
				.setReadTimeout(1234).build().getRequestFactory();
		assertThat(ReflectionTestUtils.getField(ReflectionTestUtils.getField(requestFactory, "client"), "readTimeout"))
				.isEqualTo(1234);
	}

	@Test
	public void connectTimeoutCanBeConfiguredOnAWrappedRequestFactory() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		this.builder.requestFactory(() -> new BufferingClientHttpRequestFactory(requestFactory)).setConnectTimeout(1234)
				.build();
		assertThat(ReflectionTestUtils.getField(requestFactory, "connectTimeout")).isEqualTo(1234);
	}

	@Test
	public void readTimeoutCanBeConfiguredOnAWrappedRequestFactory() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		this.builder.requestFactory(() -> new BufferingClientHttpRequestFactory(requestFactory)).setReadTimeout(1234)
				.build();
		assertThat(ReflectionTestUtils.getField(requestFactory, "readTimeout")).isEqualTo(1234);
	}

	@Test
	public void unwrappingDoesNotAffectRequestFactoryThatIsSetOnTheBuiltTemplate() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		RestTemplate template = this.builder.requestFactory(() -> new BufferingClientHttpRequestFactory(requestFactory))
				.build();
		assertThat(template.getRequestFactory()).isInstanceOf(BufferingClientHttpRequestFactory.class);
	}

	public static class RestTemplateSubclass extends RestTemplate {

	}

	static class TestClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

	}

}
