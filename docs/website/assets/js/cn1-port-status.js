(function () {
    "use strict";

    var root = document.querySelector("[data-port-status]");
    if (!root) {
        return;
    }

    var contractElement = root.querySelector("[data-port-status-contract]");
    var notice = root.querySelector("[data-port-status-notice]");
    var contract;
    try {
        contract = JSON.parse(contractElement.textContent);
    } catch (error) {
        notice.textContent = "The port-status contract could not be loaded.";
        notice.classList.add("is-error");
        return;
    }

    var reports = {};
    var reportBase = "https://raw.githubusercontent.com/codenameone/CodenameOne/" +
        contract.report_branch + "/ports/";

    function daysOld(value) {
        var timestamp = Date.parse(value || "");
        if (Number.isNaN(timestamp)) {
            return Infinity;
        }
        return (Date.now() - timestamp) / 86400000;
    }

    function featureStatus(feature, report) {
        if (!report) {
            return {state: "unknown", label: "No current report", tests: []};
        }
        var stale = daysOld(report.generated_at) > contract.stale_after_days;
        var results = feature.tests.map(function (name) {
            var result = report.tests && report.tests[name];
            return {name: name, status: result ? result.status : "not-run"};
        });
        var failed = results.filter(function (item) { return item.status === "fail"; });
        var passed = results.filter(function (item) { return item.status === "pass"; });
        var skipped = results.filter(function (item) { return item.status === "skip"; });
        var notRun = results.filter(function (item) { return item.status === "not-run"; });
        var state;
        var label;

        if (failed.length) {
            state = "fail";
            label = failed.length + " failed: " + failed.map(function (item) { return item.name; }).join(", ");
        } else if (!report.suite_finished) {
            state = "partial";
            label = "Suite did not finish";
        } else if (passed.length === results.length) {
            state = "pass";
            label = "All " + results.length + " mapped test" + (results.length === 1 ? "" : "s") + " passed";
        } else if (skipped.length === results.length) {
            state = "partial";
            label = "All mapped tests skipped";
        } else {
            state = "partial";
            label = passed.length + " passed, " + skipped.length + " skipped, " + notRun.length + " not run";
        }
        if (stale) {
            state = state === "fail" ? "fail" : "stale";
            label = "Stale report. " + label;
        }
        return {state: state, label: label, tests: results};
    }

    function markFor(state) {
        if (state === "pass") { return "✓"; }
        if (state === "fail") { return "×"; }
        if (state === "partial") { return "−"; }
        if (state === "stale") { return "!"; }
        return "?";
    }

    function updateCell(cell, status) {
        cell.className = "is-" + status.state;
        cell.title = status.label;
        var mark = cell.querySelector(".cn1-port-status__mark");
        mark.className = "cn1-port-status__mark is-" + status.state;
        mark.textContent = markFor(status.state);
        cell.querySelector("[data-status-label]").textContent = status.label;
    }

    function formatDate(value) {
        var date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return "Unknown run time";
        }
        return date.toLocaleString(undefined, {dateStyle: "medium", timeStyle: "short"});
    }

    function updatePort(port, report) {
        var card = root.querySelector('[data-port-card="' + port.id + '"]');
        var state = card.querySelector("[data-port-state]");
        var meta = card.querySelector("[data-port-meta]");
        if (!report) {
            state.textContent = port.report_pending ? "Detailed reporting pending" : "No published report";
            card.classList.add("is-unknown");
            return;
        }
        var summary = report.summary || {};
        var stale = daysOld(report.generated_at) > contract.stale_after_days;
        var failed = summary.fail || 0;
        var unfinished = !report.suite_finished;
        card.classList.add(failed ? "is-fail" : (stale || unfinished ? "is-partial" : "is-pass"));
        state.textContent = failed ? failed + " failing tests" : (unfinished ? "Run incomplete" : (stale ? "Report is stale" : "Suite completed"));
        var detail = (summary.pass || 0) + " passed · " + (summary.skip || 0) + " skipped · " + (summary["not-run"] || 0) + " not run";
        meta.replaceChildren(document.createTextNode(formatDate(report.generated_at)), document.createElement("br"), document.createTextNode(detail));
        if (report.run_url) {
            var separator = document.createTextNode(" · ");
            var link = document.createElement("a");
            link.href = report.run_url;
            link.target = "_blank";
            link.rel = "noopener";
            link.textContent = "Open run";
            meta.append(separator, link);
        }
    }

    function render() {
        contract.features.forEach(function (feature) {
            contract.ports.forEach(function (port) {
                var cell = root.querySelector('[data-feature-cell][data-port="' + port.id + '"][data-feature="' + feature.id + '"]');
                updateCell(cell, featureStatus(feature, reports[port.id]));
            });
        });
        contract.ports.forEach(function (port) {
            updatePort(port, reports[port.id]);
        });
    }

    function loadPort(port) {
        if (port.report_pending) {
            return Promise.resolve();
        }
        return fetch(reportBase + encodeURIComponent(port.id) + ".json", {cache: "no-store"})
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("HTTP " + response.status);
                }
                return response.json();
            })
            .then(function (report) {
                if (report.port !== port.id || report.schema_version !== contract.schema_version) {
                    throw new Error("Report contract mismatch");
                }
                reports[port.id] = report;
            })
            .catch(function () {
                reports[port.id] = null;
            });
    }

    function installFilters() {
        var category = root.querySelector("[data-category-filter]");
        var search = root.querySelector("[data-feature-search]");
        function apply() {
            var categoryValue = category.value;
            var query = search.value.trim().toLowerCase();
            root.querySelectorAll("[data-feature-row]").forEach(function (row) {
                var categoryMatches = !categoryValue || row.dataset.category === categoryValue;
                var searchMatches = !query || row.dataset.search.indexOf(query) !== -1;
                row.hidden = !(categoryMatches && searchMatches);
            });
        }
        category.addEventListener("change", apply);
        search.addEventListener("input", apply);
    }

    installFilters();
    Promise.all(contract.ports.map(loadPort)).then(function () {
        render();
        var loaded = Object.keys(reports).filter(function (id) { return reports[id]; }).length;
        notice.textContent = loaded
            ? "Showing the latest published reports for " + loaded + " port target" + (loaded === 1 ? "" : "s") + "."
            : "No detailed port reports have been published yet. The contract is ready; CI will populate this page after the next master runs.";
        notice.classList.toggle("is-error", loaded === 0);
    });
}());
