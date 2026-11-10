import 'dart:async';
class Item { final int id; Item(this.id); }
class Repo<T> { final List<T> _l = []; Future<List<T>> all() async => _l; Future<T?> find(bool Function(T) p) async { for (final x in _l) if (p(x)) return x; return null; } }
Future<R> guarded<R>(FutureOr<R> Function() body) async => await body();
Stream<int> ticks() async* { yield 1; yield 2; }
void main() async {
  final r = Repo<Item>(); r._l.add(Item(1));
  final a = await r.all(); print(a is List<Item>); print(a.runtimeType);
  final f = await r.find((i) => i.id == 1); print(f?.id);
  final g = await guarded(() => 5); print(g is int);
  final h = await guarded<String>(() async => 'q'); print(h);
  final l = await ticks().toList(); print(l is List<int>);
  final fut = ticks().first; print(fut is Future<int>);
  final m = await Future.any([Future.value('a')]); print(m is String);
  final e = <Future<int>>[Future.value(1)]; final w = await Future.wait(e); print(w.runtimeType);
  final then = Future.value(2).then((v) => 'v$v'); print(then is Future<String>); print(await then);
  final then2 = Future.value(2).then((v) async => v * 2); print(then2 is Future<int>);
  Future<num> fn = Future<int>.value(1); print(await fn);
  final ce = Future<int>.error('x').catchError((e) => 9); print(await ce);
  try { await Future<int>.error('x').catchError((e) => 'nope'); } catch (e) { print('ce ${e.runtimeType}'); }
  final s = StreamController<String>.broadcast(); final sub = s.stream.map((x) => x.length).listen(print); s.add('abcd'); await Future.delayed(Duration.zero); await sub.cancel();
}
