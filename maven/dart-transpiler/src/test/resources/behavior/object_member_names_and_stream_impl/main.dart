import 'dart:async';

// Members named like java.lang.Object's final/protected methods.
class Builder {
  final List<String> parts = [];
  String finalize() => parts.join('+');
  String getClass() => 'Builder';
  int notify(int n) => n * 2;
  void notifyAll() => parts.add('all');
}

// Abstract field in a mixin, real field in the class that applies it.
abstract mixin class HasTrigger {
  abstract final String? trigger;
  String describe() => 'trigger=${trigger ?? "none"}';
}

class Base {
  String describe() => 'base';
}

final class Triggered extends Base with HasTrigger {
  Triggered(this.trigger);
  @override
  final String? trigger;
}

// A Stream implemented the documented way: extend Stream, override listen.
class Doubling extends Stream<int> {
  final List<int> source;
  Doubling(this.source);
  @override
  StreamSubscription<int> listen(void Function(int)? onData,
      {Function? onError, void Function()? onDone, bool? cancelOnError}) {
    return Stream<int>.fromIterable(source.map((x) => x * 2)).listen(onData,
        onError: onError, onDone: onDone, cancelOnError: cancelOnError);
  }
}

// A set view with a mixin that overrides members of its superclass.
mixin ReadOnly<E> on Base {
  @override
  String describe() => 'read-only ${super.describe()}';
}

class View extends Base with ReadOnly<int> {}

mixin Settable {
  abstract int level;
  void bump() => level = level + 1;
}

class Gauge with Settable {
  @override
  int level = 0;
}

class Plain {}

Future<void> main() async {
  final g = Gauge();
  g.bump();
  g.bump();
  g.level = g.level * 10;
  print('gauge ${g.level}');
  print(Plain());
  print('${Builder()}');

  final b = Builder();
  b.parts.add('a');
  b.parts.add('b');
  b.notifyAll();
  print(b.finalize());
  print(b.getClass());
  print(b.notify(21));
  dynamic d = b;
  print(d.finalize());
  print('interp ${b.finalize()}');
  print(Triggered('go').describe());
  print(Triggered(null).describe());
  final out = await Doubling([1, 2, 3]).toList();
  print(out);
  final mapped = await Doubling([5]).map((x) => x + 1).toList();
  print(mapped);
  print(View().describe());
  final notify = 7;
  print('local $notify ${notify + 1}');
}
