void main() async {
  var f = Future(() => 42);
  print(await f);
  var g = Future<String>(() => 'hi');
  print(await g);
  print(g is Future<int>);
  var d = Future.delayed(Duration.zero, () => 7);
  print(await d);
  print(await Future.sync(() => 9));
  print(await Future.microtask(() => 10));
}
