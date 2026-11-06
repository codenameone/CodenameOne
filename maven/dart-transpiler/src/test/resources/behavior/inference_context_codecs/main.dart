import 'dart:async';
import 'dart:convert';

abstract class Shape {
  String get name;
}

class Square implements Shape {
  String get name => 'square';
}

class Circle implements Shape {
  String get name => 'circle';
}

class Reverser extends Converter<String, String> {
  const Reverser();
  String convert(String input) => input.split('').reversed.join();
}

class ReverseCodec extends Codec<String, String> {
  const ReverseCodec();
  Reverser get encoder => const Reverser();
  Reverser get decoder => const Reverser();
}

class Ring<E> {
  final List<E?> table;
  Ring(int n) : table = List<E?>.filled(n, null);

  factory Ring.from(Iterable<E> source) {
    if (source is List) {
      var ring = Ring<E>(source.length);
      var copy = source;
      ring.table.setRange(0, copy.length, copy, 0);
      return ring;
    }
    var ring = Ring<E>(source.length);
    ring.table.setRange(0, source.length, source, 0);
    return ring;
  }
}

class Repeater {
  final dynamic Function() callback;
  Timer? timer;
  Repeater(this.callback) : timer = Timer(Duration.zero, callback);
}

extension Greeter on String {
  Future<void> greet(bool skip) async {
    if (skip) return;
    print('hello $this');
  }

  Future<int> lengthLater() async => length;
}

int fired = 0;

Future<void> main() async {
  // A list literal argument takes its element type from the parameter.
  var shapes = <Shape>[];
  shapes.addAll([Square(), Circle()]);
  print(shapes.map((s) => s.name).join(','));

  // List.filled with no type argument, from the context and from the fill value.
  var bottoms = <int, List<int>>{};
  bottoms.putIfAbsent(7, () => List.filled(3, 0))[1] = 5;
  List<int> zeros = List.filled(2, 4);
  print('${bottoms[7]} ${zeros.reduce((a, b) => a + b)}');

  // String.codeUnits is a List<int>.
  List<int> units = 'abc'.codeUnits;
  print(units.fold<int>(0, (a, b) => a + b));

  // A codec written in Dart returns its own converters.
  const codec = ReverseCodec();
  print(codec.encode('stressed'));
  print(codec.decoder.convert('lager'));
  Codec<String, String> general = codec;
  print(general.encoder.convert('drawer'));
  print(utf8.encoder.convert('hi'));
  print(json.decoder.convert('[1,2]'));

  // An Iterable promoted by `is List` keeps its element type.
  print(Ring<int>.from([1, 2, 3]).table);
  print(Ring<int>.from({4, 5}).table);

  // A value-returning callback into a void callback slot.
  var r = Repeater(() => ++fired);
  await Future.delayed(const Duration(milliseconds: 10));
  print('fired $fired ${r.timer!.isActive}');

  // async extension methods: an early `return;` and an arrow body.
  await 'world'.greet(false);
  await 'nobody'.greet(true);
  print(await 'four'.lengthLater());
}
