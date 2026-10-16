// emitBinary's `==`/`!=` used to take Java's primitive == whenever BOTH sides
// were numeric (or both bool), with no check that either side was nullable
// (int?/double?/num?, emitted as boxed Long/Double). A null operand then hit
// Java's auto-unboxing and threw NullPointerException instead of answering
// false, and two distinct boxed instances holding the same value compared by
// reference instead of by value. Nullable numerics must route through the
// same null-safe, value-based DartRuntime.eq() helper the dynamic/object
// case already uses.

void nullNumericEquality() {
  int? n = null;
  print(n == 1);
  print(n != 1);
}

void boxedValueEquality() {
  int? a = 1000;
  int? b = 1000;
  print(a == b);
}

void nullableDoubleVsNull() {
  double? d = null;
  print(d == null);
}

void main() {
  nullNumericEquality();
  boxedValueEquality();
  nullableDoubleVsNull();
}
