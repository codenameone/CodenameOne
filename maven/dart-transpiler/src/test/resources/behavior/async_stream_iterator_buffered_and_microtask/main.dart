import 'dart:async';
void main() async {
  var c = StreamController<int>();
  var it = StreamIterator(c.stream);
  c.add(1); c.add(2); c.close();
  var a = await it.moveNext();
  print('a $a');
  if (a) print('cur ${it.current}');
  var c3 = StreamController<int>();
  var it3 = StreamIterator(c3.stream);
  var f = it3.moveNext();
  c3.add(5);
  print('b ${await f} ${it3.current}');
  var r = <String>[];
  Future.microtask(() => r.add('m2'));
  Timer.run(() => r.add('t1'));
  scheduleMicrotask(() => r.add('m1'));
  await Future.delayed(Duration(milliseconds: 5));
  print(r.join(' '));
}
