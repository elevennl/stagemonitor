package org.stagemonitor.web.monitor.filter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.stagemonitor.core.StagemonitorSPI;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.requestmonitor.RequestMonitor;
import org.stagemonitor.web.monitor.HttpRequestTrace;

/**
 * An SPI to inject content into all text/html documents
 * <p/>
 * To register a implementation, create the file
 * src/main/resources/META-INF/services/org.stagemonitor.web.monitor.filter.HtmlInjector
 * and insert the canonical class name of the implementation.
 */
public abstract class HtmlInjector implements StagemonitorSPI {

	/**
	 * Initialisation method that is called just after the implementation is instantiated
	 *
	 * @param initArguments
	 */
	public void init(InitArguments initArguments) {
	}

	/**
	 * Implementations can return html snippets that are injected just before the closing body tag.
	 * <p/>
	 * <b>Note:</b> {@link org.stagemonitor.requestmonitor.RequestMonitor.RequestInformation#getRequestTrace()} may be null
	 *
	 * @param injectArguments
	 * @return the code to inject into html documents just before the closing body tag
	 */
	public abstract void injectHtml(InjectArguments injectArguments);

	/**
	 * Returns <code>true</code>, if this {@link HtmlInjector} should be applied, <code>false</code> otherwise
	 *
	 * @return <code>true</code>, if this {@link HtmlInjector} should be applied, <code>false</code> otherwise
	 * @param isActiveArguments
	 */
	public abstract boolean isActive(IsActiveArguments isActiveArguments);

	public static class InitArguments {
		private final Configuration configuration;
		private final ServletContext servletContext;

		/**
		 * @param configuration the configuration
		 * @param servletContext the current servlet context
		 */
		public InitArguments(Configuration configuration, ServletContext servletContext) {
			this.configuration = configuration;
			this.servletContext = servletContext;
		}

		public Configuration getConfiguration() {
			return configuration;
		}

		public ServletContext getServletContext() {
			return servletContext;
		}
	}

	public static class InjectArguments {
		private final RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation;
		private String contentToInjectBeforeClosingBody;

		/**
		 * @param requestInformation information about the current request
		 */
		public InjectArguments(RequestMonitor.RequestInformation<HttpRequestTrace> requestInformation) {
			this.requestInformation = requestInformation;
		}

		public RequestMonitor.RequestInformation<HttpRequestTrace> getRequestInformation() {
			return requestInformation;
		}

		public void setContentToInjectBeforeClosingBody(String contentToInject) {
			this.contentToInjectBeforeClosingBody = contentToInject;
		}

		public String getContentToInjectBeforeClosingBody() {
			return contentToInjectBeforeClosingBody;
		}
	}

	public static class IsActiveArguments {
		private final HttpServletRequest httpServletRequest;

		/**
		 */
		IsActiveArguments(HttpServletRequest httpServletRequest) {
			this.httpServletRequest = httpServletRequest;
		}

		public HttpServletRequest getHttpServletRequest() {
			return httpServletRequest;
		}
	}
}
