/**
* Copyright (C) 2026 Red Hat, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.jboss.intersmash.tests.wildfly.distributed.timers.infinispan.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Timer expiration base implementation.
 */
public class TimerExpiration implements Serializable {

	private static final long serialVersionUID = -6807753176066577450L;

	private Long id = null;
	private String executor;
	private String name;
	private String info;
	private Instant timestamp;

	public TimerExpiration() {
		this(null, null, null, null);
	}

	public TimerExpiration(String executor, String name, String info, Instant timestamp) {
		super();
		this.executor = executor;
		this.name = name;
		this.info = info;
		this.timestamp = timestamp;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getExecutor() {
		return executor;
	}

	public void setExecutor(String executor) {
		this.executor = executor;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		TimerExpiration that = (TimerExpiration) o;
		return Objects.equals(id, that.id) &&
				Objects.equals(executor, that.executor) &&
				Objects.equals(name, that.name) &&
				Objects.equals(info, that.info) &&
				Objects.equals(timestamp, that.timestamp);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, executor, name, info, timestamp);
	}

	@Override
	public String toString() {
		return "TimerExpiration{" +
				"id=" + id +
				", executor='" + executor + '\'' +
				", name='" + name + '\'' +
				", info='" + info + '\'' +
				", timestamp=" + timestamp +
				'}';
	}
}
