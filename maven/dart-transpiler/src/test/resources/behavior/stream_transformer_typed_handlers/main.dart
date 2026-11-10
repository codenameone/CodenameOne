import 'dart:async';
class Repo<T> { final _c = StreamController<List<T>>.broadcast(); final List<T> _items = [];
  Stream<List<T>> get items => _c.stream;
  void add(T t) { _items.add(t); _c.add(List.unmodifiable(_items)); }
  Future<T?> find(bool Function(T) p) async { for (var i in _items) if (p(i)) return i; return null; }
  void close() => _c.close();
}
class Snap<T> { final T? data; final Object? error; Snap(this.data, this.error); }
void main() async {
  var r = Repo<String>();
  var snaps = <Snap<List<String>>>[];
  var sub = r.items.listen((v) => snaps.add(Snap(v, null)), onError: (e) => snaps.add(Snap(null, e)));
  r.add('a'); r.add('b');
  await Future.delayed(Duration.zero);
  print(snaps.map((s) => s.data).toList());
  print(await r.find((s) => s == 'b'));
  print(await r.find((s) => s == 'z'));
  var t = r.items.transform(StreamTransformer<List<String>, int>.fromHandlers(handleData: (d, s) => s.add(d.length)));
  var got = <int>[]; var s2 = t.listen(got.add);
  r.add('c'); await Future.delayed(Duration.zero); print(got);
  await sub.cancel(); await s2.cancel(); r.close();
  var results = await Future.wait([Future.value(1), Future.value(2)]);
  print(results is List<int>);
  print(results);
  var c = Completer<int>(); Timer(Duration(milliseconds: 1), () => c.complete(5)); print(await c.future);
  try { await Future<int>.error(StateError('x')); } on StateError catch (e) { print('se ${e.message}'); }
  Function cb = (int x) => x * 2;
  try { (cb as dynamic)('s'); } catch (e) { print('fn ${e is TypeError}'); }
}
