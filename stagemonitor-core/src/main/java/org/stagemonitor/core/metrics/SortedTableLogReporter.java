package org.stagemonitor.core.metrics;

import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.metrics.metrics2.Metric2Registry;
import org.stagemonitor.core.metrics.metrics2.MetricName;
import org.stagemonitor.core.metrics.metrics2.ScheduledMetrics2Reporter;

public class SortedTableLogReporter extends ScheduledMetrics2Reporter {
	/**
	 * Returns a new {@link SortedTableLogReporter.Builder} for {@link SortedTableLogReporter}.
	 *
	 * @param registry the registry to report
	 * @return a {@link SortedTableLogReporter.Builder} instance for a {@link SortedTableLogReporter}
	 */
	public static Builder forRegistry(Metric2Registry registry) {
		return new Builder(registry);
	}

	/**
	 * A builder for {@link SortedTableLogReporter} instances. Defaults to using the
	 * default locale and time zone, writing to {@code System.out}, converting
	 * rates to events/second, converting durations to milliseconds, and not
	 * filtering metrics.
	 */
	public static class Builder extends ScheduledMetrics2Reporter.Builder<SortedTableLogReporter, Builder> {
		private Logger log = LoggerFactory.getLogger("metrics");
		private Locale locale = Locale.getDefault();

		private Builder(Metric2Registry registry) {
			super(registry, "stagemonitor-log-reporter");
		}

		/**
		 * Log to the given {@link Logger}.
		 *
		 * @param log a {@link Logger} instance.
		 * @return {@code this}
		 */
		public Builder log(Logger log) {
			this.log = log;
			return this;
		}

		/**
		 * Format numbers for the given {@link java.util.Locale}.
		 *
		 * @param locale a {@link java.util.Locale}
		 * @return {@code this}
		 */
		public Builder formattedFor(Locale locale) {
			this.locale = locale;
			return this;
		}

		public SortedTableLogReporter build() {
			return new SortedTableLogReporter(this);
		}
	}

	private static final int CONSOLE_WIDTH = 80;

	private final Locale locale;
	private final Logger log;

	private SortedTableLogReporter(Builder builder) {
		super(builder);
		this.log = builder.log;
		this.locale = builder.locale;
	}

	@Override
	public void reportMetrics(Map<MetricName, Gauge> gauges,
							  Map<MetricName, Counter> counters,
							  Map<MetricName, Histogram> histograms,
							  Map<MetricName, Meter> meters,
							  Map<MetricName, Timer> timers) {

		final Timer.Context time = Stagemonitor.getMetric2Registry().timer(name("reporting_time").tag("reporter", "log").build()).time();

		StringBuilder sb = new StringBuilder(1000);
		printWithBanner("Metrics", '=', sb);
		sb.append('\n');

		try {
			logGauges(gauges, sb);
			logCounters(counters, sb);
			logHistograms(histograms, sb);
			logMeters(meters, sb);
			logTimers(timers, sb);
			sb.append('\n');
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			log.info(sb.toString());
			time.stop();
		}
	}

	private void logGauges(Map<MetricName, Gauge> gauges, StringBuilder sb) {
		if (!gauges.isEmpty()) {
			printWithBanner("-- Gauges", '-', sb);
			int maxLength = getMaxLengthOfKeys(gauges);
			sb.append(String.format("%-" + maxLength + "s | value\n", "name"));
			final Map<MetricName, Gauge> sortedGauges = sortByValue(gauges, new Comparator<Gauge>() {
				@Override
				public int compare(Gauge o1, Gauge o2) {
					Object value2 = o2.getValue();
					if (value2 == null) {
						value2 = "";
					}
					Object value1 = o1.getValue();
					if (value1 == null) {
						value1 = "";
					}
					return value2.toString().compareTo(value1.toString());
				}
			});
			for (Map.Entry<MetricName, Gauge> entry : sortedGauges.entrySet()) {
				printGauge(entry.getKey().getInfluxDbLineProtocolString(), entry.getValue(), maxLength, sb);
			}
			sb.append('\n');
		}
	}

	private void logCounters(Map<MetricName, Counter> counters, StringBuilder sb) {
		if (!counters.isEmpty()) {
			printWithBanner("-- Counters", '-', sb);
			int maxLength = getMaxLengthOfKeys(counters);
			sb.append(String.format("%-" + maxLength + "s | count\n", "name"));
			Map<MetricName, Counter> sortedCounters = sortByValue(counters, new Comparator<Counter>() {
				@Override
				public int compare(Counter o1, Counter o2) {
					return ((Long) o2.getCount()).compareTo(o1.getCount());
				}
			});
			for (Map.Entry<MetricName, Counter> entry : sortedCounters.entrySet()) {
				printCounter(entry.getKey().getInfluxDbLineProtocolString(), entry.getValue(), maxLength, sb);
			}
			sb.append('\n');
		}
	}

	private void logHistograms(Map<MetricName, Histogram> histograms, StringBuilder sb) {
		if (!histograms.isEmpty()) {
			printWithBanner("-- Histograms", '-', sb);
			int maxLength = getMaxLengthOfKeys(histograms);
			sb.append(String.format("%-" + maxLength + "s | count     | mean      | min       | max       | stddev    | p50       | p75       | p95       | p98       | p99       | p999      |\n", "name"));
			Map<MetricName, Histogram> sortedHistograms = sortByValue(histograms, new Comparator<Histogram>() {
				@Override
				public int compare(Histogram o1, Histogram o2) {
					return Double.compare(o2.getSnapshot().getMean(), o1.getSnapshot().getMean());
				}
			});
			for (Map.Entry<MetricName, Histogram> entry : sortedHistograms.entrySet()) {
				printHistogram(entry.getKey().getInfluxDbLineProtocolString(), entry.getValue(), maxLength, sb);
			}
			sb.append('\n');
		}
	}

	private void logMeters(Map<MetricName, Meter> meters, StringBuilder sb) {
		if (!meters.isEmpty()) {
			printWithBanner("-- Meters", '-', sb);
			int maxLength = getMaxLengthOfKeys(meters);
			sb.append(String.format("%-" + maxLength + "s | count     | mean_rate | m1_rate   | m5_rate   | m15_rate  | rate_unit     | duration_unit\n", "name"));
			Map<MetricName, Meter> sortedMeters = sortByValue(meters, new Comparator<Meter>() {
				@Override
				public int compare(Meter o1, Meter o2) {
					return ((Long) o2.getCount()).compareTo(o1.getCount());
				}
			});
			for (Map.Entry<MetricName, Meter> entry : sortedMeters.entrySet()) {
				printMeter(entry.getKey().getInfluxDbLineProtocolString(), entry.getValue(), maxLength, sb);
			}
			sb.append('\n');
		}
	}

	private void logTimers(Map<MetricName, Timer> timers, StringBuilder sb) {
		if (!timers.isEmpty()) {
			printWithBanner("-- Timers", '-', sb);
			int maxLength = getMaxLengthOfKeys(timers);
			sb.append(String.format("%-" + maxLength + "s | count     | mean      | min       | max       | stddev    | p50       | p75       | p95       | p98       | p99       | p999      | mean_rate | m1_rate   | m5_rate   | m15_rate  | rate_unit     | duration_unit\n", "name"));
			Map<MetricName, Timer> sortedTimers = sortByValue(timers, new Comparator<Timer>() {
				public int compare(Timer o1, Timer o2) {
					return Double.compare(o2.getSnapshot().getMean(), o1.getSnapshot().getMean());
				}
			});
			for (Map.Entry<MetricName, Timer> entry : sortedTimers.entrySet()) {
				printTimer(entry.getKey().getInfluxDbLineProtocolString(), entry.getValue(), maxLength, sb);
			}
			sb.append('\n');
		}
	}

	private static <K, V> Map<K, V> sortByValue(Map<K, V> map, final Comparator<V> valueComparator) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return valueComparator.compare(o1.getValue(), o2.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	private static int getMaxLengthOfKeys(Map<MetricName, ?> map) {
		int maxLength = -1;
		for (MetricName n : map.keySet()) {
			if (n.getInfluxDbLineProtocolString().length() > maxLength) {
				maxLength = n.getInfluxDbLineProtocolString().length();
			}
		}
		return maxLength;
	}

	private void printGauge(String name, Gauge gauge, int maxNameLength, StringBuilder sb) {
		sb.append(String.format("%" + maxNameLength + "s | ", name));
		sb.append(gauge.getValue()).append('\n');

	}

	private void printCounter(String name, Counter counter, int maxNameLength, StringBuilder sb) {
		sb.append(String.format("%" + maxNameLength + "s | ", name));
		sb.append(counter.getCount()).append('\n');
	}

	private void printMeter(String name, Meter meter, int maxNameLength, StringBuilder sb) {
		sb.append(String.format("%" + maxNameLength + "s | ", name));
		sb.append(formatCount(meter.getCount()));
		printMetered(meter, sb);
		sb.append('\n');
	}

	private void printMetered(Metered metered, StringBuilder sb) {
		printDouble(convertRate(metered.getMeanRate()), sb);
		printDouble(convertRate(metered.getOneMinuteRate()), sb);
		printDouble(convertRate(metered.getFiveMinuteRate()), sb);
		printDouble(convertRate(metered.getFifteenMinuteRate()), sb);
		sb.append(String.format("%-13s | ", getRateUnit()));
		sb.append(getDurationUnit());
	}


	private void printHistogram(String name, Histogram histogram, int maxNameLength, StringBuilder sb) {
		sb.append(String.format("%" + maxNameLength + "s | ", name));
		sb.append(formatCount(histogram.getCount()));
		printSnapshot(histogram.getSnapshot(), sb);
		sb.append('\n');
	}

	private void printSnapshot(Snapshot snapshot, StringBuilder sb) {
		printDouble(convertDuration(snapshot.getMean()), sb);
		printDouble(convertDuration(snapshot.getMin()), sb);
		printDouble(convertDuration(snapshot.getMax()), sb);
		printDouble(convertDuration(snapshot.getStdDev()), sb);
		printDouble(convertDuration(snapshot.getMedian()), sb);
		printDouble(convertDuration(snapshot.get75thPercentile()), sb);
		printDouble(convertDuration(snapshot.get95thPercentile()), sb);
		printDouble(convertDuration(snapshot.get98thPercentile()), sb);
		printDouble(convertDuration(snapshot.get99thPercentile()), sb);
		printDouble(convertDuration(snapshot.get999thPercentile()), sb);
	}

	private void printTimer(String name, Timer timer, int maxNameLength, StringBuilder sb) {
		final Snapshot snapshot = timer.getSnapshot();
		sb.append(String.format("%" + maxNameLength + "s | ", name));
		sb.append(formatCount(timer.getCount()));
		printSnapshot(snapshot, sb);
		printMetered(timer, sb);
		sb.append('\n');
	}

	private String formatCount(long count) {
		return String.format(locale, "%,9d | ", count);
	}

	public void printDouble(double d, StringBuilder sb) {
		sb.append(String.format(locale, "%,9.2f | ", d));
	}

	private void printWithBanner(String s, char c, StringBuilder sb) {
		sb.append(s);
		sb.append(' ');
		for (int i = 0; i < (CONSOLE_WIDTH - s.length() - 1); i++) {
			sb.append(c);
		}
		sb.append('\n');
	}

}
