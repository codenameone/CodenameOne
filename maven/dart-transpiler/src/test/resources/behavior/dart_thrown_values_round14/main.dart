// Dart throws any object, and a catch binds that exact object. `on Error` and
// `on Exception` are type tests that a thrown string passes neither of, and the
// clauses of one try are tried in order.
import 'dart:async';

class Token {
  final int id;
  Token(this.id);
}

Future<void> main() async {
  final t = Token(7);
  try {
    throw t;
  } catch (e) {
    print('${identical(e, t)} ${(e as Token).id}');
  }
  try {
    throw 'boom';
  } catch (e) {
    print('[$e] ${e is String}');
  }
  try {
    throw 'boom';
  } on Exception {
    print('Exception caught a string');
  } on Error {
    print('Error caught a string');
  } catch (e) {
    print('only the catch-all caught $e');
  }
  try {
    throw StateError('bad');
  } on Exception {
    print('Exception caught an Error');
  } on Error catch (e) {
    print('Error: ${e is StateError}');
  }
  try {
    throw FormatException('fmt');
  } on Error {
    print('Error caught an Exception');
  } on Exception catch (e) {
    print('Exception: ${e is FormatException}');
  }
  try {
    throw t;
  } on String {
    print('wrong clause');
  } on Token catch (tok) {
    print('Token ${tok.id}');
  }
  final v = await Future<int>.error('async boom').catchError((e) {
    print('catchError got [$e] ${e is String}');
    return 0;
  });
  print(v);
  try {
    await Future<int>.error(t);
  } catch (e) {
    print('await rethrew the same token: ${identical(e, t)}');
  }

  final Map<int, int> ints = {1: 1};
  try {
    ints.forEach((k, v) {
      ints[k + 10] = v;
    });
  } on ConcurrentModificationError {
    print('int map refused the change');
  }
  final Map<String, int> named = {'a': 1};
  try {
    named.forEach((k, v) {
      named['b$k'] = v;
    });
  } on ConcurrentModificationError {
    print('map refused the change');
  }
  final fixed = List<int>.unmodifiable([1, 2]);
  try {
    fixed[0] = 9;
    print('replaced');
  } on UnsupportedError {
    print('unmodifiable list refused a store');
  }
  try {
    Uri.parse('https://x/a/b').pathSegments.add('c');
    print('grew');
  } on UnsupportedError {
    print('pathSegments is read-only');
  }
  print(identical(1, 1));
}
