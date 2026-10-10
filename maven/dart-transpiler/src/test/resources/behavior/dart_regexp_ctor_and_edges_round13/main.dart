// A malformed RegExp throws from its constructor. Pinned alongside: what the Dart
// VM does for a negative string repetition, a negative Iterable.generate count and
// a map key that only rounds to the looked-up double.
void main() {
  try {
    RegExp('[');
    print('constructed');
  } on FormatException {
    print('constructor refused the pattern');
  }
  print('[${'x' * -1}]');
  print('[${'ab' * 2}]');
  print(Iterable.generate(-1, (i) => i).toList());
  final int big = int.parse('9007199254740993');
  final double near = double.parse('9007199254740992');
  final Map<Object, String> m = {big: 'a'};
  print(m[near]);
  print(m.containsKey(near));
  final Map<Object, String> exact = {9007199254740992: 'b'};
  print(exact[near]);
}
