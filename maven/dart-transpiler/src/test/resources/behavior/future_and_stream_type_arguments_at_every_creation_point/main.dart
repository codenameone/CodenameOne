import 'dart:async';
class Api {
  Future<int> count() => Future.value([1,2]).then((l) => l.length);
  Future<int> reuse() { var f = Future.value(5); return f; }
  Future<Object> obj() async => 3;
}
Future<num> asNum() async => 1;
void main() async {
  var a = Api();
  print(a.count() is Future<int>); print(a.count() is Future<String>);
  print(a.reuse() is Future<int>); print(a.obj() is Future<int>); print(asNum() is Future<int>);
  var cl = () async => 'x'; print(cl() is Future<String>); print(cl() is Future<int>);
  Future<int> Function() cl2 = () async => 1; print(cl2() is Future<int>); print(cl2() is Future<String>);
  var s = Stream.fromIterable([1, 2, 3]); print(s is Stream<int>); print(s is Stream<String>);
  print(s.expand((x) => [x, x]) is Stream<String>);
  print(s.asyncMap((x) async => '$x') is Stream<int>);
  print(Future.value('s') is Future<Object>);
  var comp = Completer<String>(); print(comp.future is Future<String>); print(comp.future is Future<int>);
  print(Future.delayed(Duration.zero, () => 1) is Future<String>);
  print(Future(() => 1) is Future<String>);
  print(Future.wait([Future.value(1)]) is Future<List<String>>);
  Future<int> g() async { return Future.value(3); } print(g() is Future<String>); print(await g());
  Future<List<int>> h() async { var r = <int>[]; r.add(1); return r; } print(h() is Future<List<String>>);
  var sc = StreamController<String>(); print(sc.stream.map((x) => x.length) is Stream<String>); sc.close();
}
