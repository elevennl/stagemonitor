# next
## Breaking Changes
 * renamed HttpExecutionContextMonitorFiler to HttpExecutionContextMonitorFilter
 * renamed SpringHttpExecutionContextMonitorFiler to SpringHttpExecutionContextMonitorFilter
 * restructured request metric names - you'll need to update the request dashboard
 * deleted starter projects
 * renamed stagemonitor-spring to stagemonitor-spring-mvc
 * rates are reported in seconds
 * removed collector middle package

## Top Features
 * support for monitoring ehcache (stagemonitor-ehcache)
 * overwrite properties with Java system property (-Dstagemonitor.property=value)
 * disable plugins with property 'stagemonitor.plugins.disabled'
 * record stack trace of thrown exceptions
 * record bytes written
 * record ip address of client
 * optionally parse and analyze user-agent header
 * overwrite properties with Java system properties
 * disable specific plugins with `stagemonitor.plugins.disabled`
 * annotate methods with @Monitored or make ExecutionContextMonitorAspect concrete in aop.xml to monitor method executions like RMI calls
 * support for metrics-annotations: @Timed, @Metered, @ExceptionMetered, @Gauge (enabled by @MonitorGauges on type)