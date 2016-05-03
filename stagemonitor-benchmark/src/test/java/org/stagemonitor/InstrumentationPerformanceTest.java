package org.stagemonitor;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isBootstrapClassLoader;
import static net.bytebuddy.matcher.ElementMatchers.nameContains;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.stagemonitor.core.instrument.ClassLoaderNameMatcher.classLoaderWithName;
import static org.stagemonitor.core.instrument.ClassLoaderNameMatcher.isReflectionClassLoader;
import static org.stagemonitor.core.instrument.TimedElementMatcherDecorator.timed;
import static org.stagemonitor.core.metrics.metrics2.MetricName.name;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.pool.TypePool;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.LoggerFactory;
import org.stagemonitor.core.Stagemonitor;
import org.stagemonitor.core.instrument.TimedElementMatcherDecorator;
import org.stagemonitor.core.metrics.SortedTableLogReporter;
import org.stagemonitor.core.metrics.metrics2.MetricName;

public class InstrumentationPerformanceTest  {

	private static Node node;

	private InstrumentationPerformanceTest() {
	}

	public static void main(String[] args) throws Exception {
//		installNoOpAgent();
		final Timer.Context timer = Stagemonitor.getMetric2Registry().timer(name("startElasticsearch").build()).time();
		startElasticsearch();
		Stagemonitor.init();
		timer.stop();
		printResults();
		node.close();
	}

	private static void installNoOpAgent() {
		ByteBuddyAgent.install();
		new AgentBuilder.Default(new ByteBuddy().with(TypeValidation.of(false)))
				.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
				.with(new AgentBuilder.BinaryLocator.WithTypePoolCache.Simple(TypePool.Default.ReaderMode.FAST, new ConcurrentHashMap<Object, TypePool.CacheProvider>()))
				.ignore(any(), timed("classloader", "bootstrap", isBootstrapClassLoader()))
				.or(any(), timed("classloader", "reflection", isReflectionClassLoader()))
				.or(any(), timed("classloader", "groovy-call-site", classLoaderWithName("org.codehaus.groovy.runtime.callsite.CallSiteClassLoader")))
				.or(timed("type", "global-exclude", nameStartsWith("java")
						.or(nameStartsWith("com.sun."))
						.or(nameStartsWith("sun."))
						.or(nameStartsWith("jdk."))
						.or(nameStartsWith("org.aspectj."))
						.or(nameStartsWith("org.groovy."))
						.or(nameStartsWith("com.p6spy."))
						.or(nameStartsWith("net.bytebuddy."))
						.or(nameStartsWith("org.slf4j.").and(not(nameStartsWith("org.slf4j.impl."))))
						.or(nameContains("javassist"))
						.or(nameContains(".asm."))
						.or(nameStartsWith("org.stagemonitor")
								.and(not(nameContains("Test").or(nameContains("benchmark")))))
				))
				.disableClassFormatChanges()
				.type(nameStartsWith("org.elasticsearch"))
				.transform(AgentBuilder.Transformer.NoOp.INSTANCE)
				.installOnByteBuddyAgent();
	}

	private static void startElasticsearch() {
		try {
			FileUtils.deleteDirectory(new File("build/elasticsearch"));
		} catch (IOException e) {
			// ignore
		}
		final NodeBuilder nodeBuilder = NodeBuilder.nodeBuilder().local(true);
		nodeBuilder.settings()
				.put("name", "junit-es-node")
				.put("node.http.enabled", "false")
				.put("path.home", "build/elasticsearch")
				.put("index.store.fs.memory.enabled", "true")
				.put("index.number_of_shards", "1")
				.put("index.number_of_replicas", "0");

		node = nodeBuilder.node();
		node.client().admin().cluster().prepareHealth().setWaitForGreenStatus().get();
	}

	public static void printResults() throws Exception {
		SortedTableLogReporter reporter = SortedTableLogReporter
				.forRegistry(Stagemonitor.getMetric2Registry())
				.log(LoggerFactory.getLogger(InstrumentationPerformanceTest.class))
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS)
				.formattedFor(Locale.US)
				.build();
		reporter.reportMetrics(new HashMap<MetricName, Gauge>(), new HashMap<MetricName, Counter>(),
				new HashMap<MetricName, Histogram>(), new HashMap<MetricName, Meter>(),
				Stagemonitor.getMetric2Registry().getTimers());

		TimedElementMatcherDecorator.logMetrics();
		Stagemonitor.reset();
	}

}
