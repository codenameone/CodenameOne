import 'dart:async';
Future<void> main() async {
  await Stream.fromIterable([1, 2]).map((x) => throw 'e$x').handleError((e) => print('h $e')).toList();
  final l = await Stream.fromIterable([1, 2, 3]).map<int>((x) => x == 2 ? throw 'two' : x * 10)
      .handleError((e) => print('h2 $e')).toList();
  print(l);
  final s = Stream.fromIterable(['a']).map<String>((x) => throw StateError('s $x'));
  try { await s.first; } catch (e) { print('caught ${e is StateError}'); }
  int Function(int) f = (x) => throw 'ff';
  try { f(1); } catch (e) { print('f $e'); }
}
