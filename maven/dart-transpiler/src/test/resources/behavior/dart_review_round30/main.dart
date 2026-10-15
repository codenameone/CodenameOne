// Five dart:core / library-structure gaps a real Flutter project hits:
//
// List.insertAll(index, iterable) had no lowering (E0137), and the runtime's
// only candidate, java.util.List.addAll(int, Collection), cannot take a lazy
// Iterable such as a where() view.
//
// takeWhile/skipWhile were implemented by DartIterable but unreachable from
// a List or Set receiver: the collection table only knew take/skip.
//
// A conditional import recorded the concatenation of every alternative as
// one path, so neither implementation resolved. On this platform dart:io
// and dart:html are absent, so Dart picks the default uri.
//
// A `part of` file came back as an empty library and its declarations
// vanished; they belong to the owning library (sharing its privacy).
//
// package:<own app>/x.dart was treated as external, so two project libraries
// declaring the same class bound `b.Helper` to whichever registered last.
import 'r30_cond_stub.dart' if (dart.library.io) 'r30_cond_io.dart' if (dart.library.html) 'r30_cond_web.dart';
import 'package:r30app/r30_pkg_b.dart';

part 'r30_part.dart';

const int _factor = 21;
final String _secret = '!';

void main() {
  final xs = <int>[1, 4];
  xs.insertAll(1, [2, 3]);
  print(xs);
  final names = <String>['a', 'b'];
  names.insertAll(0, {'x', 'y', 'z'}.where((s) => s != 'z'));
  print(names);
  names.insertAll(names.length, names);
  print(names);
  print([1, 2, 5, 1].takeWhile((e) => e < 3).toList());
  print([1, 2, 5, 1].skipWhile((e) => e < 3).toList());
  print({3, 1, 7}.takeWhile((e) => e != 7).toList());
  print({3, 1, 7}.skipWhile((e) => e != 7).toList());
  print([1, 2, 3].map((e) => e * 2).skipWhile((e) => e < 4).toList());
  print(platformName());
  print(Helper().name);
  print(PartThing('p').describe());
  print(partFunction(2));
}
