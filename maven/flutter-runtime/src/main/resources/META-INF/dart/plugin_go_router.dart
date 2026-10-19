// package:go_router, over com.codename1.flutter.plugins.gorouter.
//
// Not transpiled from pub: go_router is built on the Router/Navigator 2.0 page API,
// which the runtime does not model. The stub matches locations against the route
// tree itself and pushes the matched page through the existing Navigator, and
// MaterialApp.router takes the router in place of a route table.

@JavaName('com.codename1.flutter.plugins.gorouter.RouteBase')
abstract class RouteBase {
  external List<RouteBase> get routes;
}

@JavaName('com.codename1.flutter.plugins.gorouter.GoRouterState')
class GoRouterState {
  external Uri get uri;
  external String get matchedLocation;
  external String? get fullPath;
  external String? get name;
  external String? get path;
  external Map<String, String> get pathParameters;
  external Map<String, String> get queryParameters;
  external Object? get extra;
  external Exception? get error;
  external static GoRouterState of(BuildContext context);
}

@JavaName('com.codename1.flutter.plugins.gorouter.GoRoute')
class GoRoute extends RouteBase {
  external GoRoute({required String path, String? name,
      Widget Function(BuildContext context, GoRouterState state)? builder,
      Page<dynamic> Function(BuildContext context, GoRouterState state)? pageBuilder,
      Object? Function(BuildContext context, GoRouterState state)? redirect,
      List<RouteBase> routes, dynamic parentNavigatorKey});
}

@JavaName('com.codename1.flutter.plugins.gorouter.ShellRoute')
class ShellRoute extends RouteBase {
  external ShellRoute({Widget Function(BuildContext context, GoRouterState state, Widget child)? builder,
      List<RouteBase> routes, dynamic navigatorKey});
}

@JavaName('com.codename1.flutter.plugins.gorouter.GoPage')
class Page<T> {
  external Key? get key;
  external String? get name;
  external Object? get arguments;
}

@JavaName('com.codename1.flutter.plugins.gorouter.GoPage')
class MaterialPage<T> extends Page<T> {
  external MaterialPage({Key? key, required Widget child, String? name, Object? arguments,
      bool maintainState, bool fullscreenDialog});
  external Widget get child;
}

@JavaName('com.codename1.flutter.plugins.gorouter.GoPage')
class NoTransitionPage<T> extends Page<T> {
  external NoTransitionPage({Key? key, required Widget child, String? name, Object? arguments});
  external Widget get child;
}

@JavaName('com.codename1.flutter.plugins.gorouter.GoPage')
class CustomTransitionPage<T> extends Page<T> {
  external CustomTransitionPage({Key? key, required Widget child, String? name, Object? arguments,
      dynamic transitionsBuilder, Duration? transitionDuration, bool maintainState, bool fullscreenDialog});
  external Widget get child;
}

@JavaName('com.codename1.flutter.plugins.gorouter.GoRouter')
class GoRouter {
  external GoRouter({required List<RouteBase> routes, String? initialLocation,
      Object? Function(BuildContext context, GoRouterState state)? redirect,
      Widget Function(BuildContext context, GoRouterState state)? errorBuilder,
      int redirectLimit, bool debugLogDiagnostics, dynamic navigatorKey, Listenable? refreshListenable});
  external static GoRouter of(BuildContext context);
  external static GoRouter? maybeOf(BuildContext context);
  external GoRouterState? get state;
  external void go(String location, {Object? extra});
  external Future<T?> push<T extends Object?>(String location, {Object? extra});
  external void pushReplacement(String location, {Object? extra});
  external void replace(String location, {Object? extra});
  external void pop<T extends Object?>([T? result]);
  external bool canPop();
  external void goNamed(String name, {Map<String, String> pathParameters, Map<String, dynamic> queryParameters, Object? extra});
  external Future<T?> pushNamed<T extends Object?>(String name, {Map<String, String> pathParameters, Map<String, dynamic> queryParameters, Object? extra});
  external String namedLocation(String name, {Map<String, String> pathParameters, Map<String, dynamic> queryParameters});
}

@JavaName('com.codename1.flutter.plugins.gorouter.GoRouterHelper')
extension GoRouterHelper on BuildContext {
  external void go(String location, {Object? extra});
  external Future<T?> push<T extends Object?>(String location, {Object? extra});
  external void pushReplacement(String location, {Object? extra});
  external void replace(String location, {Object? extra});
  external void pop<T extends Object?>([T? result]);
  external bool canPop();
  external void goNamed(String name, {Map<String, String> pathParameters, Map<String, dynamic> queryParameters, Object? extra});
  external Future<T?> pushNamed<T extends Object?>(String name, {Map<String, String> pathParameters, Map<String, dynamic> queryParameters, Object? extra});
  external String namedLocation(String name, {Map<String, String> pathParameters, Map<String, dynamic> queryParameters});
}
