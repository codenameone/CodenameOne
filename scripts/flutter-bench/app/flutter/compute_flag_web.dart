// Web builds: the harness opens the page with ?benchCompute=1, the same query
// the Codename One app reads through browser.window.location.search.
bool computeRequested(List<String> args) =>
    Uri.base.queryParameters.containsKey('benchCompute');
