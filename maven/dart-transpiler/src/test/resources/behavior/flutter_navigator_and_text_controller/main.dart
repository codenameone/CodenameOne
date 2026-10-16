// Three review findings, headless (no Display, no mounted Form -- see
// NestedNavigatorWidgetTest for the version that actually mounts a page):
//
//  1. Navigator's constructor used to declare only `key`, so
//     `Navigator(initialRoute: ..., onGenerateRoute: ...)` failed to
//     transpile at all (E0140: "no parameter named initialRoute") even
//     though the Java class implements every one of these setters.
//  2. Navigator.maybePop was typed `bool` and answered synchronously; Flutter's
//     own signature is `Future<bool> maybePop(BuildContext context, [T?
//     result])`, so `await Navigator.maybePop(context)` did not compile.
//  3. TextEditingController declared a `text` getter plus a separate
//     setText(), but nothing declared a Dart `set text(...)`, so the emitter
//     had no stub setter to route `controller.text = v` through.
import 'package:flutter/material.dart';

void main() async {
  // --- #3: controller.text = v must call the notifying setter ------------
  final TextEditingController ctl = TextEditingController(text: 'start');
  int notified = 0;
  ctl.addListener(() {
    notified++;
  });
  ctl.text = 'typed';
  print('TEXT ${ctl.text} NOTIFIED $notified');

  // --- #1: the nested-Navigator constructor parameters must compile ------
  String? seenRouteName;
  final Navigator nav = Navigator(
    initialRoute: '/detail',
    onGenerateRoute: (RouteSettings settings) {
      seenRouteName = settings.name;
      return MaterialPageRoute(builder: (BuildContext context) => const SizedBox());
    },
    onUnknownRoute: (RouteSettings settings) => null,
    restorationScopeId: 'nav-scope',
  );
  print('NAV_IS_WIDGET ${nav is Widget}');
  print('ROUTE_NAME_BEFORE_BUILD $seenRouteName');

  // --- #2: maybePop is a Future<bool> with an optional result ------------
  final bool poppedWithNothingPushed = await Navigator.maybePop(null);
  print('MAYBEPOP_EMPTY $poppedWithNothingPushed');

  final MaterialPageRoute route = MaterialPageRoute(
    builder: (BuildContext context) => const SizedBox(),
  );
  final Future<Object?> pushed = Navigator.push(null, route);
  final bool poppedWithOnePushed = await Navigator.maybePop(null, 'closed-by-maybepop');
  final Object? pushResult = await pushed;
  print('MAYBEPOP_PUSHED $poppedWithOnePushed RESULT $pushResult');
}
