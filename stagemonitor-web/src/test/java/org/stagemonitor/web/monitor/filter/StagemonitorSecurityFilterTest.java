package org.stagemonitor.web.monitor.filter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.stagemonitor.core.configuration.Configuration;
import org.stagemonitor.core.configuration.ConfigurationOptionProvider;
import org.stagemonitor.core.configuration.source.ConfigurationSource;
import org.stagemonitor.core.configuration.source.SimpleSource;
import org.stagemonitor.web.WebPlugin;

public class StagemonitorSecurityFilterTest {

	@Test
	public void testStagemonitorSecurityFilter() throws Exception {
		//         url                            password   widget     header      req attr    allowed
		testFilter("/stagemonitor/public",        null,      true,      null,       null,       true);
		testFilter("/stagemonitor/foo",           null,      true,      null,       null,       true);

		testFilter("/stagemonitor/public",        "",        true,      null,       null,       true);
		testFilter("/stagemonitor/foo",           "",        true,      null,       null,       true);

		testFilter("/stagemonitor/public",        "",        false,     null,       null,       true);
		testFilter("/stagemonitor/foo",           "",        false,     null,       null,       true);
		testFilter("/stagemonitor/foo",           "",        false,     "pw",       null,       true);

		testFilter("/stagemonitor/public",        "pw",      true,      null,       null,       true);
		testFilter("/stagemonitor/foo",           "pw",      true,      null,       null,       true);

		testFilter("/stagemonitor/public",        "pw",      false,     null,       null,       true);
		testFilter("/stagemonitor/foo",           "pw",      false,     null,       null,       false);
		testFilter("/stagemonitor/foo",           "pw",      false,     "wp",       null,       false);

		testFilter("/stagemonitor/foo",           "pw",      false,     "pw",       null,       true);
		testFilter("/stagemonitor/foo",           "pw",      false,     null,       true,       true);
		testFilter("/stagemonitor/foo",           "pw",      true,      null,       null,       true);

		testFilter("/stagemonitor/foo",           "pw",      false,     null,       false,      false);
		testFilter("/stagemonitor/foo",           "pw",      false,     "pw",       false,      false);
		testFilter("/stagemonitor/foo",           "pw",      true,      null,       false,      false);
		testFilter("/stagemonitor/foo",           "",        true,      null,       false,      false);
		testFilter("/stagemonitor/foo",           null,      true,      null,       false,      false);

		testFilter("/stagemonitor/public/bar",    "pw",      false,     null,       false,      true);
		testFilter("/stagemonitor/public/bar",    "pw",      false,     "pw",       false,      true);
		testFilter("/stagemonitor/public/bar",    "pw",      true,      null,       false,      true);
		testFilter("/stagemonitor/public/bar",    "",        true,      null,       false,      true);
		testFilter("/stagemonitor/public/bar",    null,      true,      null,       false,      true);
	}

	public void testFilter(String url, String password, boolean widgetEnabled, String enableWidgetHeaderValue,
						   Boolean enableWidgetRequestAttribute, boolean allowed) throws Exception {

		WebPlugin webPlugin = new WebPlugin();
		SimpleSource configurationSource = new SimpleSource();
		if (password != null) {
			configurationSource.add("stagemonitor.password", password);
		}
		configurationSource.add("stagemonitor.web.widget.enabled", Boolean.toString(widgetEnabled));
		Configuration configuration = new Configuration(Arrays.<ConfigurationOptionProvider>asList(webPlugin),
				Arrays.<ConfigurationSource>asList(configurationSource), "stagemonitor.password");

		StagemonitorSecurityFilter stagemonitorSecurityFilter = new StagemonitorSecurityFilter(configuration);
		FilterChain filterChain = mock(FilterChain.class);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", url);
		if (enableWidgetRequestAttribute != null) {
			request.setAttribute("X-Stagemonitor-Show-Widget", enableWidgetRequestAttribute);
		}
		if (enableWidgetHeaderValue != null) {
			request.addHeader("X-Stagemonitor-Show-Widget", enableWidgetHeaderValue);
		}
		MockHttpServletResponse response = new MockHttpServletResponse();
		stagemonitorSecurityFilter.doFilter(request, response, filterChain);

		verify(filterChain, times(allowed ? 1 : 0)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		assertEquals(allowed ? 200 : 404, response.getStatus());
	}
}
