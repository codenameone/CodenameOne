import 'dart:async';
class Api { Future<List<Map<String, dynamic>>> fetch() async => [{'id': 1}, {'id': 2}];
  Future<int> count() => fetch().then((l) => l.length);
  Future<int> reuse() { var f = Future.value(5); return f; }
  Future<Object> obj() async => 3;
}
Future<num> asNum() async => 1;
void main() async {
  var a = Api();
  print(a.fetch() is Future<List<Map<String, dynamic>>>);
  print(a.fetch() is Future<List<int>>);
  print(a.count() is Future<int>);
  print(a.count() is Future<String>);
  print(a.reuse() is Future<int>);
  print(a.obj() is Future<int>);
  print(asNum() is Future<int>);
  var cl = () async => 'x';
  print(cl() is Future<String>);
  Future<int> Function() cl2 = () async => 1;
  print(cl2() is Future<int>);
  print(cl2() is Future<String>);
  var s = Stream.fromIterable([1, 2, 3]);
  print(s is Stream<int>);
  print(s.expand((x) => [x, x]) is Stream<int>);
  print(s.asyncMap((x) async => '$x') is Stream<String>);
  print(await Stream.fromIterable([1,2]).map((x) => x * 2).toList());
  print([Future.value(1)] is List<Future<int>>);
  FutureOr<int> fo = 3; print(fo is int);
  print(Future.value('s') is Future<Object>);
  var comp = Completer<String>(); print(comp.future is Future<String>); print(comp.future is Future<int>);
}
