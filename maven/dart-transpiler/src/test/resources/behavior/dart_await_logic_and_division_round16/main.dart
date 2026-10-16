// replaceFirst with and without a start; && and || over dynamic operands keep
// short-circuiting; awaiting a value that is not a Future is the value; division
// and remainder by zero are UnsupportedErrors; Future.wait over an iterable that
// throws completes with that error.
Iterable<Future<int>> bad() sync* {
  throw StateError('iter');
}

bool checked = false;
bool check() {
  checked = true;
  return true;
}

Future<void> main() async {
  print('abcabc'.replaceFirst('b', 'X'));
  print('abcabc'.replaceFirst('b', 'X', 2));
  print('abcabc'.replaceFirst(RegExp('c'), 'Y', 3));

  dynamic on = true;
  dynamic off = false;
  if (on && check()) {
    print('both true');
  }
  checked = false;
  if (off && check()) {
    print('never');
  }
  print('right side skipped: ${!checked}');
  if (off || check()) {
    print('or reached the right side');
  }

  print(await 42);
  print(await null);
  final int z = int.parse('0');
  try {
    print(1 ~/ z);
  } on UnsupportedError {
    print('int ~/ 0 is an UnsupportedError');
  }
  try {
    print(1 % z);
  } on UnsupportedError {
    print('int % 0 is an UnsupportedError');
  }
  try {
    print(1.0 ~/ 0.0);
  } on UnsupportedError {
    print('double ~/ 0 is an UnsupportedError');
  }
  final f = Future.wait(bad());
  print('Future.wait returned');
  await f.catchError((e) {
    print('the future carried ${e is StateError}');
    return <int>[];
  });
}
