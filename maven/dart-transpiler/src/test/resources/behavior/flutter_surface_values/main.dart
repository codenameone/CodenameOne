// Value types behind the keyboard, the system bars, path transforms and recognizers.
//
// TextInputType is a class in Flutter, not an enum: numberWithOptions makes values
// of its own that compare equal by their options. copyWith on a system overlay style
// must change what it was given and keep the rest. Path.transform maps every point
// through the matrix, and scaleByDouble scales all four columns.
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  const plain = TextInputType.number;
  const opts = TextInputType.numberWithOptions(decimal: true);
  print('number: $plain');
  print('options: $opts');
  print('text: ${TextInputType.text}');
  print('signed ${opts.signed} decimal ${opts.decimal} index ${opts.index}');
  print('same number: ${plain == const TextInputType.numberWithOptions()}');
  print('options differ: ${plain == opts}');
  print('email index: ${TextInputType.emailAddress.index}');

  final base = SystemUiOverlayStyle.dark;
  final changed = base.copyWith(statusBarColor: const Color(0xFF112233));
  print('status colour: ${changed.statusBarColor == const Color(0xFF112233)}');
  print('icons kept: ${changed.statusBarIconBrightness == base.statusBarIconBrightness}');
  print('base untouched: ${base.statusBarColor == null}');
  print('changed differs: ${changed != base}');

  final m = Matrix4.identity()..scaleByDouble(2, 3, 4, 5);
  print('diagonal: ${m.storage.where((v) => v != 0).join(' ')}');

  final p = Path()
    ..moveTo(1, 1)
    ..lineTo(2, 1)
    ..lineTo(2, 2)
    ..close();
  final t = Matrix4.identity()..scaleByDouble(10, 10, 1, 1);
  final q = p.transform(t.storage);
  final b = q.getBounds();
  print('bounds: ${b.left} ${b.top} ${b.right} ${b.bottom}');

  final GestureRecognizer r = TapGestureRecognizer()..onTap = () {};
  r.dispose();
  print('disposed');
  print('kDefaultFontSize: $kDefaultFontSize');
}
