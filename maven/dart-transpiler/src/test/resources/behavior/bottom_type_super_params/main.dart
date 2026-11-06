import 'dart:async';

// The bottom type: a method that never returns, and Never as a type argument.
Never fail(String why) => throw StateError(why);

abstract class Outcome<T> {
  T get value;
}

class Broken implements Outcome<Never> {
  final String reason;
  Broken(this.reason);
  @override
  Never get value => fail(reason);
}

class Ok<T> implements Outcome<T> {
  @override
  final T value;
  Ok(this.value);
}

int pick(Outcome<int> o) {
  try {
    return o.value;
  } on StateError catch (e) {
    print('caught ${e.message}');
    return -1;
  }
}

// Super parameters forwarding through a generic superclass.
class Holder<E> {
  final List<E> items;
  Holder(this.items);
  E first() => items.first;
}

class IntHolder extends Holder<int> {
  IntHolder(super.items);
  int sum() => items.fold(0, (a, b) => a + b);
}

class Wrapper<T> {
  final Stream<T> source;
  Wrapper(this.source);
}

class Lines extends Wrapper<String> {
  Lines(super.source);
}

// A dart:core error type used as an interface.
class BadInput implements FormatException {
  @override
  final String message;
  BadInput(this.message);
  @override
  dynamic get source => null;
  @override
  int? get offset => null;
}

// A transformer that only supplies bind.
class Doubler extends StreamTransformerBase<int, int> {
  @override
  Stream<int> bind(Stream<int> stream) => stream.map((v) => v * 2);
}

Future<void> main() async {
  print(pick(Ok<int>(7)));
  Outcome<int> b = Broken('nothing here');
  print(pick(b));
  final h = IntHolder([1, 2, 3, 4]);
  print('${h.first()} ${h.sum()}');
  final l = Lines(Stream.fromIterable(['a', 'b']));
  print(await l.source.join(','));
  try {
    throw BadInput('not a number');
  } catch (e) {
    print('bad: ${(e as BadInput).message}');
  }
  final out = await Stream.fromIterable([1, 2, 3]).transform(Doubler()).toList();
  print(out);
}
