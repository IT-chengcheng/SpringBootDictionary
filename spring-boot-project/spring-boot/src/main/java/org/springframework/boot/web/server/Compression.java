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

package org.springframework.boot.web.server;

/**
 * Simple server-independent abstraction for compression configuration.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class Compression {

	private boolean enabled = false;

	private String[] mimeTypes = new String[] { "text/html", "text/xml", "text/plain", "text/css", "text/javascript",
			"application/javascript", "application/json", "application/xml" };

	private String[] excludedUserAgents = null;

	private int minResponseSize = 2048;

	/**
	 * Return whether response compression is enabled.
	 * @return {@code true} if response compression is enabled
	 */
	public boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Return the MIME types that should be compressed.
	 * @return the MIME types that should be compressed
	 */
	public String[] getMimeTypes() {
		return this.mimeTypes;
	}

	public void setMimeTypes(String[] mimeTypes) {
		this.mimeTypes = mimeTypes;
	}

	/**
	 * Return the user agents for which responses should not be compressed.
	 * @return the user agents for which responses should not be compressed.
	 */
	public String[] getExcludedUserAgents() {
		return this.excludedUserAgents;
	}

	public void setExcludedUserAgents(String[] excludedUserAgents) {
		this.excludedUserAgents = excludedUserAgents;
	}

	/**
	 * Return the minimum "Content-Length" value that is required for compression to be
	 * performed.
	 * @return the minimum content size in bytes that is required for compression
	 */
	public int getMinResponseSize() {
		return this.minResponseSize;
	}

	public void setMinResponseSize(int minSize) {
		this.minResponseSize = minSize;
	}

}
