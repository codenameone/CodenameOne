import 'dart:typed_data';
import 'dart:math' as math;

class ParseFailure implements Exception {
  final String message;
  ParseFailure(this.message);
  @override
  String toString() => 'ParseFailure: $message';
}

class Named {
  final String label;
  Named(this.label);
  String describe() => 'named $label';
}

class Sized {
  int get size => 0;
}

class Base {
  int base() => 1;
}

class Both extends Named {
  Both() : super('x');
  int base() => 1;
  int get size => 2;
  @override
  String describe() => 'both $label $size';
}

abstract class Greeter {
  String greet();
}

mixin Hello implements Greeter {
  @override
  String greet() => 'hello';
}

mixin Loud {
  String shout() => 'loud A';
}

mixin Louder {
  String shout() => 'loud B';
}

class Speaker with Hello, Loud, Louder {}

mixin Logger {
  double logOf(num x) => math.log(x);
}

class Calc with Logger {}

mixin Holder<E> {
  List<E> items = [];
  List<E> copy() => List<E>.of(items);
}

class IntHolder with Holder<String> {}

extension ByteWords on List<int> {
  int readWord(int offset) => this[offset] | this[offset + 1] << 8;
  void writeWord(int offset, int value) {
    this[offset] = value & 0xff;
    this[offset + 1] = (value >> 8) & 0xff;
  }
}

extension Total on Iterable<int> {
  int get total => fold(0, (a, b) => a + b);
}

class Cell<T> {
  final T value;
  Cell(this.value);
  @override
  String toString() => 'Cell($value)';
}

String pad(String s, [int width = 4, String fill = '.']) => s.padLeft(width, fill);

void main() {
  print(Cell<int>(7));
  var fixed = Map<String, int>.unmodifiable({'a': 1});
  try {
    fixed['b'] = 2;
  } on UnsupportedError {
    print('map read-only ${fixed.length}');
  }
  var frozen = List<int>.unmodifiable([1, 2]);
  try {
    frozen.add(3);
  } on UnsupportedError {
    print('list read-only ${frozen.length}');
  }
  try {
    throw ParseFailure('bad');
  } on Exception catch (e) {
    print('caught $e');
  }
  Both b = Both();
  Named n = b;
  var s = b;
  print('${n.label} ${n.describe()} ${s.size} ${b.base()}');
  print(Speaker().greet());
  print(Speaker().shout());
  print(Calc().logOf(math.e).toStringAsFixed(3));
  var h = IntHolder();
  h.items.add('3');
  h.items.add('4');
  print(h.copy());
  var bytes = Uint8List(4);
  bytes.writeWord(1, 0x1234);
  print('${bytes.readWord(1)} $bytes ${bytes.total}');
  List<int> asList = bytes;
  print(asList.length);
}
