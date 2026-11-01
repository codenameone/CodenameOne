import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

// Expected values are Flutter's documented behaviour (painting/text_style.dart,
// dart:ui Color, painting/text_scaler.dart, painting/border_radius.dart).
void main() {
  final both = TextDecoration.combine([TextDecoration.underline, TextDecoration.lineThrough]);
  print('combine has underline: ${both.contains(TextDecoration.underline)}');
  print('combine has lineThrough: ${both.contains(TextDecoration.lineThrough)}');
  print('combine has overline: ${both.contains(TextDecoration.overline)}');
  print('none contains none: ${TextDecoration.none.contains(TextDecoration.none)}');

  const c = Color(0xFF336699);
  print('r g b a: ${c.r.toStringAsFixed(3)} ${c.g.toStringAsFixed(3)} ${c.b.toStringAsFixed(3)} ${c.a.toStringAsFixed(3)}');
  final half = c.withValues(alpha: 0.5);
  print('withValues alpha: ${half.a.toStringAsFixed(3)} red kept: ${half.r.toStringAsFixed(3)}');

  print('linear(2).scale(10): ${TextScaler.linear(2).scale(10)}');
  print('noScaling.scale(14): ${TextScaler.noScaling.scale(14)}');
  print('clamped: ${TextScaler.linear(3).clamp(maxScaleFactor: 2).scale(10)}');

  final r = BorderRadius.circular(8).copyWith(topLeft: const Radius.circular(2));
  print('copyWith: ${r.topLeft.x} ${r.topRight.x} ${r.bottomLeft.y}');

  const sq = Size.square(5);
  print('square: ${sq.width} ${sq.height}');
  print('kDebugMode is bool: ${kDebugMode is bool}');
}
