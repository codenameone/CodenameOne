// `await` on a NULLABLE future: Dart evaluates `await null` to null. The emitter
// used Await.await$ for every Future-typed operand, which throws TypeError on a
// null Future.
Future<int>? maybe(bool give) => give ? Future.value(7) : null;

Future<void> main() async {
  Future<int>? none = maybe(false);
  print(await none);
  Future<int>? some = maybe(true);
  print(await some);
  Future<int> plain = Future.value(3);
  print(await plain);
}
