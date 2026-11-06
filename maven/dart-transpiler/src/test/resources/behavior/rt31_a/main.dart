import 'dart:async';
class Button { final void Function()? onPressed; final void Function(String)? onChanged; Button({this.onPressed, this.onChanged}); }
class Holder { final int Function({required int a, int b}) f; Holder(this.f); }
typedef Cmp = int Function(int, int);
int counter = 0;
int bump() => ++counter;
void main() async {
  var b = Button(onPressed: () => bump(), onChanged: (s) => s.length);
  b.onPressed!(); b.onChanged!('abc'); print(counter);
  var b2 = Button(onPressed: () { counter += 10; });
  b2.onPressed?.call(); print(counter);
  var h = Holder(({required int a, int b = 3}) => a * b);
  print(h.f(a: 2)); print(h.f(a: 2, b: 5));
  var l = [3, 1, 2];
  print(l.map((e) => e * 2).toList());
  print(l.fold<int>(0, (p, e) => p + e));
  print(l.fold(0, (p, e) => (p as int) + e));
  print(l.reduce((a, b) => a > b ? a : b));
  l.sort((a, b) => b.compareTo(a)); print(l);
  Cmp c = (a, b) => a - b; l.sort(c); print(l);
  var strs = ['bb', 'a', 'ccc']; strs.sort((x, y) => x.length.compareTo(y.length)); print(strs);
  var r = await Future.value(2).then((v) => v + 1).then((v) => 'x$v');
  print(r);
  var e = await Future<int>.error(StateError('boom')).catchError((err) => 7);
  print(e);
  var e2 = await Future<int>.error('s').then((v) => v * 2, onError: (err) => -1);
  print(e2);
  await Future.value(1).then((_) { print('void then'); });
  var fz = await Future.value(5).then((v) async => v * 3); print(fz);
  var s = Stream.fromIterable([1, 2, 3, 4]);
  print(await s.map((x) => x * 10).where((x) => x > 10).asyncMap((x) async => x + 1).toList());
  var flag = counter > 5;
  Future<int> Function() g = flag ? () async => 1 : () async { return 2; };
  print(await g());
  var tf = bump; var tf2 = bump;
  print(identical(tf, tf2)); print(tf == tf2);
  print(identical(print, print));
  int Function(int) sel(bool q) => q ? (x) => x + 1 : (x) => x - 1;
  print(sel(true)(5)); print(sel(false)(5));
  print(gen(true).map((f) => f()).toList()); 
  Object o = () => 1; print(o is int Function()); print(o is Function);
  var n = () => null; print(n());
  var thr = () => throw 'no'; try { thr(); } catch (x) { print('caught $x'); }
}
Iterable<int Function()> gen(bool q) sync* { yield q ? () => 1 : () => 2; yield () => 3; }
