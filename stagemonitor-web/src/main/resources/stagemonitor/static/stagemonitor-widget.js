$(document).ready(function () {
	window.tabPlugins = [];

	window.stagemonitor = {
		initialize: function (data, configurationSources, configurationOptions, baseUrl, contextPath, passwordSet,
							  connectionId, pathsOfWidgetMetricTabPlugins, openImmediately) {
			try {
				stagemonitor.rootRequestTrace = data;
				stagemonitor.configurationSources = configurationSources;
				stagemonitor.configurationOptions = configurationOptions;
				stagemonitor.baseUrl = baseUrl;
				stagemonitor.contextPath = contextPath;
				stagemonitor.passwordSet = passwordSet;
				stagemonitor.connectionId = connectionId;
				stagemonitor.pathsOfWidgetMetricTabPlugins = pathsOfWidgetMetricTabPlugins;
				if (window.pageLoadTime) {
					data.pageLoadTime = window.pageLoadTime;
				}
				setCallTree(data);
				renderRequestTab(data);
				listenForAjaxRequestTraces(data, connectionId);
				$("#call-stack-tab").find("a").click(function () {
					renderCallTree();
				});
				if (openImmediately) {
					window.stagemonitor.onOpen();
				}

			} catch (e) {
				console.log(e);
			}
		},
		thresholdExceeded: false,
		renderPageLoadTime: function (pageLoadTimeData) {
			stagemonitor.rootRequestTrace.pageLoadTime = pageLoadTimeData;
			renderRequestTab(stagemonitor.rootRequestTrace);
		},
		initialized: false,
		onOpen: function () {
			try {
				renderCallTree();
			} catch (e) {
				console.log(e);
			}
			if (!stagemonitor.initialized) {
				try {
					renderConfigTab(stagemonitor.configurationSources, stagemonitor.configurationOptions, stagemonitor.passwordSet);
					try {
						renderMetricsTab();
					} catch (e) {
						console.log(e);
					}
					loadTabPlugins();
					$(".tip").tooltip();
					stagemonitor.initialized = true;
				} catch (e) {
					console.log(e);
				}
			}
		}
	};

	function loadTabPlugins() {
		$.each(stagemonitor.pathsOfTabPlugins, function (i, tabPluginPath) {
			utils.loadScripts([tabPluginPath + ".js"], function () {
				$.each(tabPlugins, function (i, tabPlugin) {
					try {
						$("#tab-list").append('<li id="' + tabPlugin.tabId + '">' +
							'	<a href="#' + tabPlugin.tabContentId + '" role="tab" data-toggle="tab" class="tip" data-placement="bottom"' +
							'		title="' + (tabPlugin.tooltip || '') + '">' +
							tabPlugin.label +
							'	</a>' +
							'</li>');

						$("#tab-content").append('<div class="tab-pane" id="' + tabPlugin.tabContentId + '"></div>');

						$.get(tabPluginPath + ".html", function (html) {
							tabPlugin.renderTab(html);
							$(".tip").tooltip();
						});
					} catch (e) {
						console.log(e);
					}
				});
			});
		});
	}

	$("#stagemonitor-modal-close").on("click", function () {
		closeOverlay();
	});

	$("body").on("keyup", function (event) {
		if (event.keyCode == 27) {  // escape key
			closeOverlay();
			event.preventDefault();
		}
	});

	function closeOverlay() {
		window.stagemonitor.closeOverlay();
	}

	$(".nav-tabs a").on("click", function (e) {
		$(this).tab('show');
		return false;
	});

	// serialize checkboxes as true/false
	var originalSerializeArray = $.fn.serializeArray;
	$.fn.extend({
		serializeArray: function () {
			var brokenSerialization = originalSerializeArray.apply(this);
			var checkboxValues = $(this).find('input[type=checkbox]').map(function () {
				return {'name': this.name, 'value': this.checked};
			}).get();
			var checkboxKeys = $.map(checkboxValues, function (element) {
				return element.name;
			});
			var withoutCheckboxes = $.grep(brokenSerialization, function (element) {
				return $.inArray(element.name, checkboxKeys) == -1;
			});

			return $.merge(withoutCheckboxes, checkboxValues);
		}
	});

	try {
		window.parent.StagemonitorLoaded();
	} catch (e) {
		console.log(e);
	}

	$("#tab-content").on("click", ".branch", function (event) {
		var $branch = $(this);
		if (!$(event.target).hasClass("expander")) {
			var treeTableNodeId = $branch.data("tt-id");
			$("#stagemonitor-calltree").treetable("node", treeTableNodeId).toggle();
		}
		var $queryCount = $branch.find(".query-count");
		$branch.hasClass("collapsed") ? $queryCount.show() : $queryCount.hide();
	});

});