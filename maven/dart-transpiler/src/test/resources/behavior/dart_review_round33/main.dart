// whereType<T?>() and whereType<dynamic>() admit null, so Dart keeps null
// elements; Class.isInstance(null) is false and the runtime dropped them.
// whereType<String>() (non-nullable) still drops them.
//
// String.startsWith(RegExp(...)) emitted Java's String.startsWith, which
// takes only a String: the generated source did not compile. A RegExp
// matches as a prefix at the index (0 by default).
void main() {
  final List<Object?> xs = <Object?>[null, 'x', 1, null, 'y'];
  print(xs.whereType<String?>().toList());
  print(xs.whereType<String>().toList());
  print(xs.whereType<dynamic>().length);
  print(xs.whereType<Object>().length);

  final String s = 'abc123';
  print(s.startsWith(RegExp('[a-c]+')));
  print(s.startsWith(RegExp('[0-9]')));
  print(s.startsWith(RegExp('[0-9]'), 3));
  print(s.startsWith(RegExp('b'), 0));
  print(s.startsWith('abc'));
  print(s.startsWith('123', 3));
}
