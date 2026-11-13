// `is`/`as List<X>` test the list's reified type argument, as Dart does, not its elements.
bool isStrings(Object o) => o is List<String>;
bool isBools(Object o) => o is List<bool>;
bool isNums(Object o) => o is List<num>;
bool isObjects(Object o) => o is List<Object>;
bool isNullableStrings(Object o) => o is List<String?>;

String tryAs(Object o) {
  try {
    final l = o as List<String>;
    return 'ok ${l.length}';
  } catch (e) {
    return 'TypeError';
  }
}

void main() {
  final emptyStrings = <String>[];
  print('empty String is List<String>: ${isStrings(emptyStrings)}');
  print('empty String is List<bool>: ${isBools(emptyStrings)}');
  final wide = <Object>['a', 'b'];
  print('Object[a,b] is List<String>: ${isStrings(wide)}');
  print('Object[a,b] is List<Object>: ${isObjects(wide)}');
  print('String[a] is List<Object>: ${isObjects(<String>['a'])}');
  print('String[a] is List<String?>: ${isNullableStrings(<String>['a'])}');
  print('String?[a] is List<String>: ${isStrings(<String?>['a'])}');
  print('double? list is List<num>: ${isNums(<double?>[1.5])}');
  final List<String> inferred = ['x'];
  print('inferred is List<String>: ${isStrings(inferred)}');
  final mapped = [1, 2].map((i) => 'n$i').toList();
  print('map().toList() is List<String>: ${isStrings(mapped)} ${mapped.length}');
  final copied = List<Object>.from(['a']);
  print('List<Object>.from is List<String>: ${isStrings(copied)}');
  final copiedS = List<String>.from(wide);
  print('List<String>.from is List<String>: ${isStrings(copiedS)}');
  print('filled is List<bool>: ${isBools(List<bool>.filled(2, true))}');
  print('filled Object is List<bool>: ${isBools(List<Object>.filled(2, true))}');
  print('generate is List<String>: ${isStrings(List<String>.generate(2, (i) => '$i'))}');
  print('toList copy keeps type: ${isStrings(emptyStrings.toList())}');
  print('as on Object list: ${tryAs(wide)}');
  print('as on String list: ${tryAs(<String>['q'])}');
  print('as on empty String list: ${tryAs(emptyStrings)}');
}
