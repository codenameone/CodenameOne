// Non-finite conversions and a bad clamp range throw; asMap is a live,
// unmodifiable view; double.tryParse follows Dart's grammar; String + dynamic
// dispatches; Point has value equality; toList(growable: false) is fixed.
import 'dart:math';

void main() {
  double bad = double.nan;
  try {
    print(bad.toInt());
  } on UnsupportedError {
    print('toInt refused NaN');
  }
  double inf = double.infinity;
  try {
    print(inf.floor());
  } on UnsupportedError {
    print('floor refused infinity');
  }
  try {
    print(5.clamp(10, 0));
  } on ArgumentError {
    print('clamp refused the range');
  }
  print(7.clamp(0, 5));

  final list = [1, 2];
  final view = list.asMap();
  list[0] = 9;
  print(view[0]);
  print(view.length + view[1]!);
  try {
    view[0] = 5;
    print('wrote');
  } on UnsupportedError {
    print('view is read-only');
  }

  print(double.tryParse('1d'));
  print(double.tryParse('0x1.0p0'));
  print(double.tryParse(' 1.5e3 '));
  print(double.tryParse('.5'));

  dynamic n = 1;
  try {
    print('a' + n);
  } catch (e) {
    print('string plus int refused');
  }

  print(Point(1, 2) == Point(1, 2));

  final fixed = [1, 2].toList(growable: false);
  try {
    fixed.add(3);
    print('grew');
  } on UnsupportedError {
    print('fixed length');
  }
}
