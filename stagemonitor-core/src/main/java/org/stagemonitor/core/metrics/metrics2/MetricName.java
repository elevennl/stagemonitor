package org.stagemonitor.core.metrics.metrics2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

import org.stagemonitor.core.util.GraphiteSanitizer;

/**
 * Represents a metrics 2.0 name that consists of a name and arbitrary tags (a set of key-value-pairs).
 * </p>
 * This is needed for example for InfluxDB's data model (see https://influxdb.com/docs/v0.9/concepts/schema_and_data_layout.html)
 * </p>
 * See also http://metrics20.org/
 */
public class MetricName {

	private String influxDbLineProtocolString;

	private final String name;

	// The insertion order is important for the correctness of #toGraphiteName
	private final LinkedHashMap<String, String> tags;

	private MetricName(String name, LinkedHashMap<String, String> tags) {
		this.name = name;
		this.tags = tags;
	}

	public MetricName withTag(String key, String value) {
		return withTags(Collections.singletonMap(key, value));
	}

	public MetricName withTags(Map<String, String> prefixTags) {
		return name(name).tags(prefixTags).tags(this.tags).build();
	}

	public static Builder name(String name) {
		return new Builder(name);
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getTags() {
		return Collections.unmodifiableMap(tags);
	}

	/**
	 * Converts a metrics 2.0 name into a graphite compliant name by appending all tag values to the metric name
	 *
	 * @return A graphite compliant name
	 */
	public String toGraphiteName() {
		StringBuilder sb = new StringBuilder(GraphiteSanitizer.sanitizeGraphiteMetricSegment(name));
		for (String value : tags.values()) {
			sb.append('.').append(GraphiteSanitizer.sanitizeGraphiteMetricSegment(value));
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MetricName)) return false;

		MetricName that = (MetricName) o;

		return name.equals(that.name) && tags.equals(that.tags);
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + tags.hashCode();
		return result;
	}

	public boolean matches(MetricName other) {
		if (name.equals(other.name)) {
			return containsAllTags(other.tags);
		} else {
			return false;
		}
	}

	private boolean containsAllTags(Map<String, String> tags) {
		for (Map.Entry<String, String> entry : tags.entrySet()) {
			if (!entry.getValue().equals(this.tags.get(entry.getKey()))) {
				return false;
			}
		}
		return true;
	}

	public static class Builder {

		private final String name;

		private final LinkedHashMap<String, String> tags = new LinkedHashMap<String, String>(8);

		public Builder(String name) {
			this.name = name;
		}

		public Builder tag(String key, Object value) {
			this.tags.put(key, value.toString());
			return this;
		}

		public Builder type(String value) {
			this.tags.put("type", value);
			return this;
		}

		public Builder tier(String value) {
			this.tags.put("tier", value);
			return this;
		}

		public Builder layer(String value) {
			this.tags.put("layer", value);
			return this;
		}

		public Builder unit(String value) {
			this.tags.put("unit", value);
			return this;
		}

		public Builder tags(Map<String, String> tags) {
			this.tags.putAll(tags);
			return this;
		}

		public MetricName build() {
			return new MetricName(name, tags);
		}

	}

	@Override
	public String toString() {
		return "name='" + name + '\'' + ", tags=" + tags;
	}

	public String getInfluxDbLineProtocolString() {
		if (influxDbLineProtocolString == null) {
			final StringBuilder sb = new StringBuilder(name.length() + tags.size() * 16 + tags.size());
			sb.append(escapeForInfluxDB(name));
			appendTags(sb, tags);
			influxDbLineProtocolString = sb.toString();
		}
		return influxDbLineProtocolString;
	}

	public static String getInfluxDbTags(Map<String, String> tags) {
		final StringBuilder sb = new StringBuilder();
		appendTags(sb, tags);
		return sb.toString();
	}

	private static void appendTags(StringBuilder sb, Map<String, String> tags) {
		for (String key : new TreeSet<String>(tags.keySet())) {
			sb.append(',').append(escapeForInfluxDB(key)).append('=').append(escapeForInfluxDB(tags.get(key)));
		}
	}

	private static String escapeForInfluxDB(String s) {
		if (s.indexOf(',') != -1 || s.indexOf(' ') != -1) {
			return s.replace(" ", "\\ ").replace(",", "\\,");
		}
		return s;
	}

}
