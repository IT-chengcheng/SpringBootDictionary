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

package org.springframework.boot.autoconfigure.web.servlet.error;

import java.util.Properties;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NonRecursivePropertyPlaceholderHelper}.
 *
 * @author Phillip Webb
 */
public class NonRecursivePropertyPlaceholderHelperTests {

	private final NonRecursivePropertyPlaceholderHelper helper = new NonRecursivePropertyPlaceholderHelper("${", "}");

	@Test
	public void canResolve() {
		Properties properties = new Properties();
		properties.put("a", "b");
		String result = this.helper.replacePlaceholders("${a}", properties);
		assertThat(result).isEqualTo("b");
	}

	@Test
	public void cannotResolveRecursive() {
		Properties properties = new Properties();
		properties.put("a", "${b}");
		properties.put("b", "c");
		String result = this.helper.replacePlaceholders("${a}", properties);
		assertThat(result).isEqualTo("${b}");
	}

}
