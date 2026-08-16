// Null-aware member access, `?.`, in every position it actually gets written.
//
// The interesting half is the METHOD call. `a?.b` was shorted correctly, but `a?.m()`
// only honoured the `?.` when the receiver happened to be function-valued; every other
// receiver emitted a plain, unguarded invocation. That is silent until the receiver is
// null, and then it is a NullPointerException in code the Dart says cannot throw - the
// gallery's `_timeDilationTimer?.cancel()` threw the first time slow motion was toggled,
// because that timer is null until something has been dilated.
//
// Everything here is plain Dart, so both implementations must agree exactly.
class Box {
  Box(this.label);
  final String label;
  int calls = 0;

  String describe() {
    calls++;
    return 'box:$label';
  }

  Box? get self => this;
  Box? get nothing => null;
  int get size => 7;
}

Box? maybe(bool present) => present ? Box('x') : null;

void main() {
  // 1. The call itself: shorted on null, run on non-null.
  print('CALL ${maybe(true)?.describe()}');
  print('CALL ${maybe(false)?.describe()}');

  // 2. In STATEMENT position, where the result is discarded - this is where the missing
  //    guard actually bit, because nothing forced the expression into a value context.
  final Box? absent = maybe(false);
  absent?.describe();
  print('STATEMENT survived');

  // 3. Chains: a null anywhere shorts the WHOLE chain, and the rest must not run.
  final Box present = Box('y');
  print('CHAIN ${present.self?.describe()}');
  print('CHAIN ${present.nothing?.describe()}');
  print('CHAIN ${present.nothing?.self?.describe()}');

  // 4. A property read after a call, and a call after a property read.
  print('MIXED ${maybe(true)?.self?.size}');
  print('MIXED ${maybe(false)?.self?.size}');

  // 5. Short-circuiting is REAL: the receiver expression runs once, and a shorted call
  //    must not invoke the method at all.
  final Box counted = Box('z');
  final Box? nullBox = null;
  counted.describe();
  nullBox?.describe();
  print('SIDE_EFFECTS ${counted.calls}');

  // 6. ?. combined with ?? - the usual way a nullable result gets a default.
  print('DEFAULTED ${maybe(false)?.describe() ?? "fallback"}');
  print('DEFAULTED ${maybe(true)?.describe() ?? "fallback"}');

  // 7. On a function-valued field, which was the one case that already worked.
  void Function()? nullCallback;
  int fired = 0;
  void Function()? liveCallback = () => fired++;
  nullCallback?.call();
  liveCallback?.call();
  print('CALLBACKS $fired');
}
