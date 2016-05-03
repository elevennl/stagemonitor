package org.stagemonitor.requestmonitor;

import static net.bytebuddy.matcher.ElementMatchers.inheritsAnnotation;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.not;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class ClassLevelMonitorRequestsTransformer extends AbstractMonitorRequestsTransformer {

	@Override
	protected ElementMatcher.Junction<TypeDescription> getIncludeTypeMatcher() {
		return super.getIncludeTypeMatcher().and(inheritsAnnotation(MonitorRequests.class));
	}

	@Override
	protected ElementMatcher.Junction<MethodDescription.InDefinedShape> getExtraMethodElementMatcher() {
		return not(isAnnotatedWith(MonitorRequests.class)).and(isPublic());
	}

}
