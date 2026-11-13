import 'dart:async';
typedef Listener<T> = void Function(T value);
class Notifier<T> { final _ls = <Listener<T>>[]; T _v; Notifier(this._v);
  void addListener(Listener<T> l) => _ls.add(l);
  set value(T v) { _v = v; for (final l in _ls) l(v); } T get value => _v; }
class Event { final String type; Event(this.type); }
class Click extends Event { Click() : super('click'); }
void main() async {
  var n = Notifier<int>(0);
  n.addListener((v) => print('got $v'));
  n.value = 3;
  dynamic dn = n;
  try { dn.value = 'x'; } catch (e) { print('set ${e is TypeError}'); }
  try { dn.addListener((String s) {}); } catch (e) { print('al ${e is TypeError}'); }
  dn.addListener((Object o) => print('obj $o'));
  n.value = 4;
  var bus = StreamController<Event>.broadcast();
  bus.stream.where((e) => e is Click).cast<Click>().listen((c) => print('click ${c.type}'));
  bus.add(Event('x')); bus.add(Click());
  await Future.delayed(Duration.zero);
  var futs = <Future<String>>[for (var i = 0; i < 3; i++) Future.delayed(Duration(milliseconds: 3 - i), () => 's$i')];
  print(await Future.wait(futs));
  print(await Future.any(futs));
  var sc = StreamController<int>();
  sc.stream.timeout(Duration(milliseconds: 5), onTimeout: (sink) { sink.addError('to'); sink.close(); })
    .listen(print, onError: (e) => print('err $e'), onDone: () => print('done'));
  await Future.delayed(Duration(milliseconds: 30));
  Future<void> f() async { throw FormatException('bad'); }
  await f().catchError((e) => print('caught ${e.runtimeType}'));
  var r = await f().then((_) => 1, onError: (e) => -1);
  print(r);
  Stream<int> counter(int n) async* { for (var i = 0; i < n; i++) { yield i; } }
  await for (var v in counter(3)) { print('c$v'); }
  print(await counter(4).reduce((a, b) => a + b));
  print(await counter(4).fold<String>('', (a, b) => '$a$b'));
  var x = await counter(3).first; print(x.runtimeType);
}
