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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;

import org.junit.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnBean}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class ConditionalOnBeanTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void testNameOnBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanNameConfiguration.class)
				.run(this::hasBarBean);
	}

	@Test
	public void testNameAndTypeOnBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanNameAndTypeConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	public void testNameOnBeanConditionReverseOrder() {
		// Ideally this should be true
		this.contextRunner.withUserConfiguration(OnBeanNameConfiguration.class, FooConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	public void testClassOnBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanClassConfiguration.class)
				.run(this::hasBarBean);
	}

	@Test
	public void testClassOnBeanClassNameCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanClassNameConfiguration.class)
				.run(this::hasBarBean);
	}

	@Test
	public void testOnBeanConditionWithXml() {
		this.contextRunner.withUserConfiguration(XmlConfiguration.class, OnBeanNameConfiguration.class)
				.run(this::hasBarBean);
	}

	@Test
	public void testOnBeanConditionWithCombinedXml() {
		// Ideally this should be true
		this.contextRunner.withUserConfiguration(CombinedXmlConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	public void testAnnotationOnBeanCondition() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnAnnotationConfiguration.class)
				.run(this::hasBarBean);
	}

	@Test
	public void testOnMissingBeanType() {
		this.contextRunner.withUserConfiguration(FooConfiguration.class, OnBeanMissingClassConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("bar"));
	}

	@Test
	public void withPropertyPlaceholderClassName() {
		this.contextRunner
				.withUserConfiguration(PropertySourcesPlaceholderConfigurer.class,
						WithPropertyPlaceholderClassName.class, OnBeanClassConfiguration.class)
				.withPropertyValues("mybeanclass=java.lang.String")
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	public void beanProducedByFactoryBeanIsConsideredWhenMatchingOnAnnotation() {
		this.contextRunner
				.withUserConfiguration(FactoryBeanConfiguration.class, OnAnnotationWithFactoryBeanConfiguration.class)
				.run((context) -> {
					assertThat(context).hasBean("bar");
					assertThat(context).hasSingleBean(ExampleBean.class);
				});
	}

	private void hasBarBean(AssertableApplicationContext context) {
		assertThat(context).hasBean("bar");
		assertThat(context.getBean("bar")).isEqualTo("bar");
	}

	@Test
	public void conditionEvaluationConsidersChangeInTypeWhenBeanIsOverridden() {
		this.contextRunner.withUserConfiguration(OriginalDefinition.class, OverridingDefinition.class,
				ConsumingConfiguration.class).run((context) -> {
					assertThat(context).hasBean("testBean");
					assertThat(context).hasSingleBean(Integer.class);
					assertThat(context).doesNotHaveBean(ConsumingConfiguration.class);
				});
	}

	@Configuration
	@ConditionalOnBean(name = "foo")
	protected static class OnBeanNameConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnBean(name = "foo", value = Date.class)
	protected static class OnBeanNameAndTypeConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnBean(annotation = EnableScheduling.class)
	protected static class OnAnnotationConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnBean(String.class)
	protected static class OnBeanClassConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnBean(type = "java.lang.String")
	protected static class OnBeanClassNameConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnBean(type = "some.type.Missing")
	protected static class OnBeanMissingClassConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@EnableScheduling
	protected static class FooConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

	@Configuration
	@ImportResource("org/springframework/boot/autoconfigure/condition/foo.xml")
	protected static class XmlConfiguration {

	}

	@Configuration
	@ImportResource("org/springframework/boot/autoconfigure/condition/foo.xml")
	@Import(OnBeanNameConfiguration.class)
	protected static class CombinedXmlConfiguration {

	}

	@Configuration
	@Import(WithPropertyPlaceholderClassNameRegistrar.class)
	protected static class WithPropertyPlaceholderClassName {

	}

	@Configuration
	static class FactoryBeanConfiguration {

		@Bean
		public ExampleFactoryBean exampleBeanFactoryBean() {
			return new ExampleFactoryBean();
		}

	}

	@Configuration
	@ConditionalOnBean(annotation = TestAnnotation.class)
	static class OnAnnotationWithFactoryBeanConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	protected static class WithPropertyPlaceholderClassNameRegistrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			RootBeanDefinition bd = new RootBeanDefinition();
			bd.setBeanClassName("${mybeanclass}");
			registry.registerBeanDefinition("mybean", bd);
		}

	}

	public static class ExampleFactoryBean implements FactoryBean<ExampleBean> {

		@Override
		public ExampleBean getObject() {
			return new ExampleBean("fromFactory");
		}

		@Override
		public Class<?> getObjectType() {
			return ExampleBean.class;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}

	}

	@TestAnnotation
	public static class ExampleBean {

		private String value;

		public ExampleBean(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public @interface TestAnnotation {

	}

	@Configuration
	public static class OriginalDefinition {

		@Bean
		public String testBean() {
			return "test";
		}

	}

	@Configuration
	@ConditionalOnBean(String.class)
	public static class OverridingDefinition {

		@Bean
		public Integer testBean() {
			return 1;
		}

	}

	@Configuration
	@ConditionalOnBean(String.class)
	public static class ConsumingConfiguration {

		ConsumingConfiguration(String testBean) {
		}

	}

}
