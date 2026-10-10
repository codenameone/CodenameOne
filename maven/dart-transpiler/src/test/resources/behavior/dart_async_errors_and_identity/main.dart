// An async function's exceptions belong to the Future it returns, and Dart's
// List, Map and Set compare by identity.

Future<int> fails() async {
  throw StateError('boom');
}

Future<int> failsAfterAwait() async {
  await Future.value(1);
  throw StateError('late');
}

void main() {
  Future<int>? f;
  try {
    f = fails();
    print('async throw returned a future');
  } catch (e) {
    print('async throw escaped synchronously');
  }
  f?.catchError((e) {
    print('caught through the future');
  });
  failsAfterAwait().catchError((e) {
    print('caught after await');
  });

  print([1] == [1]);
  var a = [1];
  print(a == a);
  var m = <Object, int>{};
  m[[1]] = 1;
  m[[1]] = 2;
  print(m.length);
}
