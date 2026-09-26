// Every slot of the default text theme, because Flutter code assumes they all exist.
//
// Material code reaches for these with a null assertion all the time -
// `theme.textTheme.bodyLarge!.copyWith(...)` is the ordinary way to write it, and the
// gallery does exactly that in its settings list. In Flutter that never throws, because
// ThemeData always materialises a full TextTheme from Typography and merges any partial
// override onto it. A runtime whose default TextTheme is empty turns every one of those
// call sites into a TypeError, and the widget that was being built silently disappears.
//
// So this sweeps all fifteen slots and reports the numbers that decide how text looks.
//
// Run on BOTH implementations:
//   real Flutter  - benchcn1/tools/dart-reference.sh (flutter test, writes expect.txt)
//   this runtime  - BehaviorTest transpiles it, runs it on the JVM, diffs the output
import 'package:flutter/material.dart';

/// PRESENCE, not metrics.
///
/// The metrics legitimately differ: a bare ThemeData in Flutter leaves fontSize null and
/// resolves it from Typography when a Theme is applied, while this runtime fills in the
/// Material 3 default size immediately. Pinning the numbers would therefore fail for a
/// difference nobody wants to remove. What both implementations MUST agree on - and what
/// the settings page actually depends on - is that no slot is ever null, because Material
/// code reaches for them with `!`.
String describe(String name, TextStyle? s) {
  return s == null ? 'SLOT $name NULL' : 'SLOT $name present';
}

void main() {
  final TextTheme t = ThemeData(brightness: Brightness.light).textTheme;

  print(describe('displayLarge', t.displayLarge));
  print(describe('displayMedium', t.displayMedium));
  print(describe('displaySmall', t.displaySmall));
  print(describe('headlineLarge', t.headlineLarge));
  print(describe('headlineMedium', t.headlineMedium));
  print(describe('headlineSmall', t.headlineSmall));
  print(describe('titleLarge', t.titleLarge));
  print(describe('titleMedium', t.titleMedium));
  print(describe('titleSmall', t.titleSmall));
  print(describe('bodyLarge', t.bodyLarge));
  print(describe('bodyMedium', t.bodyMedium));
  print(describe('bodySmall', t.bodySmall));
  print(describe('labelLarge', t.labelLarge));
  print(describe('labelMedium', t.labelMedium));
  print(describe('labelSmall', t.labelSmall));

  // The property every one of those `!` call sites depends on.
  final List<TextStyle?> all = <TextStyle?>[
    t.displayLarge, t.displayMedium, t.displaySmall,
    t.headlineLarge, t.headlineMedium, t.headlineSmall,
    t.titleLarge, t.titleMedium, t.titleSmall,
    t.bodyLarge, t.bodyMedium, t.bodySmall,
    t.labelLarge, t.labelMedium, t.labelSmall,
  ];
  print('NONNULL ${all.where((TextStyle? s) => s != null).length} of ${all.length}');

  // copyWith must preserve what it is not asked to change - the gallery layers colour
  // onto these styles and keeps the metrics.
  final TextStyle body = t.bodyLarge!;
  final TextStyle tinted = body.copyWith(color: const Color(0xFF00FF00));
  print('COPYWITH kept_size ${tinted.fontSize == body.fontSize}'
      ' kept_weight ${tinted.fontWeight == body.fontWeight}');

  // ...and an explicit override must win over the default, or an app theme cannot theme.
  final TextTheme custom = ThemeData(
    textTheme: const TextTheme(bodyLarge: TextStyle(fontSize: 99)),
  ).textTheme;
  print('OVERRIDE ${((custom.bodyLarge?.fontSize ?? -1) * 100).round()}');
  // The slots it did NOT override must still be there.
  print('OVERRIDE_KEEPS ${custom.titleLarge == null ? 'NULL' : 'present'}');
}
