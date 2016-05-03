package org.stagemonitor.requestmonitor.reporter;

public class TestServiceLoaderPostExecutionInterceptor extends PostExecutionRequestTraceReporterInterceptor {
	@Override
	public void interceptReport(PostExecutionInterceptorContext context) {
		context.addProperty("serviceLoaderWorks", true);
	}
}
