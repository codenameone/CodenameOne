import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

// Expected values are Flutter's documented behaviour (dart:ui FontVariation,
// painting/text_style.dart, cupertino/colors.dart, widgets/icon_data.dart,
// rendering Size).
void main() {
  const v = FontVariation('wght', 600);
  print('variation: ${v.axis} ${v.value}');
  print('weight helper: ${FontVariation.weight(300).axis}');
  print('variation equal: ${v == const FontVariation('wght', 600)}');
  const s = TextStyle(fontVariations: [v], leadingDistribution: TextLeadingDistribution.even,
      fontFamilyFallback: ['serif']);
  print('style variations: ${s.fontVariations!.length} ${s.leadingDistribution} ${s.fontFamilyFallback}');
  final d = CupertinoDynamicColor.withBrightness(
      color: const Color(0xFF112233), darkColor: const Color(0xFF445566));
  print('dynamic light: ${d.color == const Color(0xFF112233)} dark: ${d.darkColor == const Color(0xFF445566)}');
  const it = IconThemeData(weight: 500, fill: 1, grade: 25, opticalSize: 48);
  print('icon theme: ${it.weight} ${it.fill} ${it.grade} ${it.opticalSize} ${it.shadows}');
  print('icon mirrors: ${const IconData(0xe000).matchTextDirection}');
  print('size zero: ${Size.zero.width} ${Size.zero.height}');
  const InlineSpan span = TextSpan(text: 'x', style: TextStyle(fontSize: 12));
  print('inline style: ${span.style!.fontSize}');
}
