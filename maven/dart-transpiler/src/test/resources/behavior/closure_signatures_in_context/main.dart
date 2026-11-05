// Every closure carries its Dart signature, wherever its Java type comes from: a callback
// argument, a typed local, a bound or static tear-off.

String describe(Object Function(String) callback) {
  return '${callback is String Function(String)} ${callback is int Function(String)} '
      '${callback is Object Function(Object)}';
}

String probe(Object callback) {
  return '${callback is int Function(Object)} ${callback is String Function(Object)} '
      '${callback is int Function()} ${callback is void Function()} ${callback is Object? Function()}';
}

void runVoid(void Function() action) {
  action();
  print('void callback int: ${action is int Function()}');
  print('void callback void: ${action is void Function()}');
}

int twice(int x) => x * 2;

String shout(String s) => s.toUpperCase();

class Greeter {
  final String name;
  Greeter(this.name);
  String greet(String other) => '$name greets $other';
  static int length(String s) => s.length;
}

T identity<T>(T value) => value;

class Box {
  static T keep<T>(T value) => value;
  T pick<T>(T value) => value;
  int plain(int value) => value + 1;
}

class SubBox extends Box {
  Object probeInherited() {
    Object own = pick;
    return own;
  }
}

void main() {
  // A closure passed as an argument, matching and mismatching the tested type.
  print(describe((s) => s + '!'));
  print(describe((s) => s.length));
  print(probe((Object o) => 'text'));
  print(probe((Object o) => 7));
  print(probe(() {}));
  print(probe(() => 3));

  // A void-bodied closure in a function-typed parameter.
  runVoid(() {
    print('ran');
  });

  // Typed locals.
  String Function(Object) typed = (o) => 'v$o';
  print(typed is String Function(Object));
  print(typed is int Function(Object));
  Object held = typed;
  print(held is String Function(int));
  void Function() noop = () {};
  print(noop is int Function());
  print(noop is void Function());
  print(noop is dynamic Function());
  int Function(int) square = (x) => x * x;
  print(square is int Function(int));
  print(square is String Function(int));
  print(square(5));

  // A closure stored in a list and tested later.
  final List<Object> callbacks = [(int a) => a + 1, (String a) => a.length, () {}];
  for (final c in callbacks) {
    print('${c is int Function(int)} ${c is int Function(String)} ${c is int Function()}');
  }

  // Tear-offs.
  Object t1 = twice;
  print(t1 is int Function(int));
  print(t1 is String Function(int));
  print(describe(shout));
  print(probe(shout));
  final g = Greeter('ann');
  Object t2 = g.greet;
  print(t2 is String Function(String));
  print(t2 is int Function(String));
  print(describe(g.greet));
  Object t3 = Greeter.length;
  print(t3 is int Function(String));
  print(t3 is String Function(String));

  // A generic function, and a closure whose types come from a type argument.
  Object id = identity;
  print(id is Object Function(Object));
  int Function(int) idInt = identity;
  print(idInt is int Function(int));
  print(idInt is String Function(int));

  // A tear-off with no context takes its declared function type.
  var t = twice;
  Object heldT = t;
  print(heldT is int Function(int));
  print(heldT is String Function(int));
  var s2 = shout;
  print((s2 as Object) is String Function(String));
  print(t(21));

  // Generic methods of other classes, static, instance and inherited.
  Object k = Box.keep;
  print(k is Object Function(Object));
  print(k is int Function(int));
  Object p = Box().pick;
  print(p is Object Function(Object));
  print(p is int Function(int));
  Object pl = Box().plain;
  print(pl is int Function(int));
  print(pl is String Function(int));
  Object inh = SubBox().probeInherited();
  print(inh is Object Function(Object));
  print(inh is int Function(int));
  int Function(int) keepInt = Box.keep;
  print(keepInt is int Function(int));
  print(keepInt(4));
}
