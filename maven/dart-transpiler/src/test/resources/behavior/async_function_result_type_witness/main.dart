import 'dart:async';
Future<int> fut() async => 1;
Future<String> fs() async { await null; return 'x'; }
Stream<int> gen() async* { yield 1; yield 2; }
void main() async {
  print(fut() is Future<String>);
  print(fut() is Future<int>);
  print(fs() is Future<int>);
  print(gen() is Stream<String>);
  print(gen().map((x) => '$x') is Stream<int>);
  print(gen().where((x) => x > 1) is Stream<String>);
  print(Future.value(3) is Future<String>);
  var c = StreamController<int>();
  print(c.stream is Stream<String>);
  print(await fut());
  print(await gen().toList());
}
