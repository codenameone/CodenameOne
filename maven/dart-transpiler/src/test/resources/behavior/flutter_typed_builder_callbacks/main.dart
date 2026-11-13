// Untyped closure parameters passed to typed widget-builder slots.
//
// `(context, value, _) {...}` given to ValueListenableBuilder<DateTime>.builder takes
// ValueWidgetBuilder<DateTime>, and `(context, index) => ...` given to
// PageView.builder's itemBuilder takes IndexedWidgetBuilder, also inside a
// collection-if of a children list and as a cascade receiver.
import 'package:flutter/material.dart';

String stamp(DateTime d) => '${d.year}-${d.month}';

Widget page(bool showClock, ValueNotifier<DateTime> clock, List<String> labels) {
  return Column(children: [
    if (showClock)
      ValueListenableBuilder<DateTime>(
        valueListenable: clock,
        builder: (context, value, _) {
          return Text(stamp(value));
        },
      ),
    if (labels.isNotEmpty)
      SizedBox(
        height: 40,
        child: PageView.builder(
          itemCount: labels.length,
          itemBuilder: (context, index) => Text(labels[index].toUpperCase()),
        ),
      ),
  ]);
}

void main() {
  final clock = ValueNotifier<DateTime>(DateTime(2024, 3, 9));
  final w = page(true, clock, ['a', 'b']);
  print('built: ${w is Column}');
  final pv = PageView.builder(itemCount: 1, itemBuilder: (context, index) => Text('$index'));
  print('page view: ${pv is Widget}');
  print('stamp: ${stamp(clock.value)}');
}
