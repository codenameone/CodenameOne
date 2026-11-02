class Box<T> {
  final T value;
  // T here is a local variable, not the class type parameter.
  final int doubled = (() { var T = 21; return T * 2; })();
  final int viaParam = ((int T) => T + 1)(4);
  Box(this.value);
}

void show(Object e) {
  if (e is RangeError) {
    print('${e.name} ${e.invalidValue} ${e.start} ${e.end} ${e.message}');
  } else if (e is ArgumentError) {
    print('${e.name} ${e.invalidValue} ${e.message}');
  }
  print(e);
}

void main() {
  final b = Box<String>('s');
  print('${b.value} ${b.doubled} ${b.viaParam}');
  show(ArgumentError('plain'));
  show(ArgumentError('m', 'nm'));
  show(ArgumentError.value(7, 'v', 'bad'));
  show(ArgumentError.notNull('p'));
  show(RangeError.value(5, 'i', 'msg'));
  show(RangeError.range(9, 0, 3, 'idx'));
  show(RangeError.index(4, [1, 2], 'ix'));
  show(RangeError('r'));
  try {
    ArgumentError.checkNotNull(null, 'q');
  } catch (e) {
    show(e);
  }
  print(RangeError.checkValueInInterval(2, 0, 5, 'w'));
  try {
    RangeError.checkValueInInterval(8, 0, 5, 'w');
  } catch (e) {
    show(e);
  }
  print(RangeError.checkNotNegative(3, 'n'));
  try {
    RangeError.checkNotNegative(-1, 'n');
  } catch (e) {
    show(e);
  }
  print(RangeError.checkValidRange(1, 2, 4));
}
