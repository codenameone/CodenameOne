// Program classes that extend the core error and exception classes.

class QueueClosed extends StateError {
  final String queue;
  QueueClosed(this.queue, String m) : super(m);
  String describe() => 'closed ' + queue + ': ' + message;
  @override
  String toString() => 'QueueClosed(' + queue + ') ' + message;
}

class PlainState extends StateError {
  PlainState(String m) : super(m);
  String viaImplicit() => 'implicit ' + toString() + ' / ' + message;
}

class BadCount extends ArgumentError {
  BadCount(int n) : super.value(n, 'count', 'must be positive');
}

class SimpleArg extends ArgumentError {
  SimpleArg(String m) : super(m);
}

class OutOfBox extends RangeError {
  OutOfBox(int i) : super.range(i, 0, 9, 'slot');
}

class BadToken extends FormatException {
  BadToken(String src, int off) : super('bad token', src, off);
  String where() => 'at ' + offset.toString() + ' in ' + source.toString();
}

class AppFailure implements Exception {
  final String reason;
  AppFailure(this.reason);
  @override
  String toString() => 'AppFailure: ' + reason;
}

class Broken extends Error {
  final String what;
  Broken(this.what);
  @override
  String toString() => 'Broken ' + what;
}

void main() {
  var q = QueueClosed('jobs', 'no more work');
  print(q.message);
  print(q.toString());
  print(q.describe());

  var p = PlainState('stuck');
  print(p.message);
  print(p.toString());
  print(p.viaImplicit());

  try {
    throw PlainState('thrown');
  } on StateError catch (e) {
    print('as StateError: ' + e.message);
  }
  try {
    throw QueueClosed('io', 'gone');
  } on QueueClosed catch (e) {
    print('as QueueClosed: ' + e.queue + ' ' + e.message);
  }

  try {
    throw BadCount(-3);
  } on ArgumentError catch (e) {
    print('as ArgumentError: ' + e.toString());
  }
  try {
    throw SimpleArg('plain');
  } on SimpleArg catch (e) {
    print('as SimpleArg: ' + e.message.toString());
  }
  try {
    throw OutOfBox(12);
  } on RangeError catch (e) {
    print('as RangeError: ' + e.toString());
  }
  try {
    throw BadToken('a+b', 1);
  } on FormatException catch (e) {
    print('as FormatException: ' + e.message);
    print((e as BadToken).where());
  }
  try {
    throw AppFailure('disk');
  } on Exception catch (e) {
    print('as Exception: ' + e.toString());
  }
  try {
    throw Broken('gear');
  } on Error catch (e) {
    print('as Error: ' + e.toString());
  }
}
