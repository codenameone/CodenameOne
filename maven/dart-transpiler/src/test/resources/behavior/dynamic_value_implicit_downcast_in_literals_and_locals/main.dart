void tryIt(String label, void Function() f) {
  try { f(); print('$label ok'); } catch (e) { print('$label ${e is TypeError ? "TypeError" : e.runtimeType}'); }
}
void main() {
  dynamic n;
  dynamic one = 1;
  dynamic s = 'bob';
  tryIt('map', () { Map<String, int> m = {'a': null as dynamic}; print(m); });
  tryIt('mapOk', () { Map<String, int> m = {'a': one}; print(m); });
  tryIt('listspread', () { List<int> l = [...[n]]; print(l); });
  tryIt('listspreadOk', () { List<int> l = [...[one]]; print(l); });
  tryIt('listif', () { List<int> l = [if (true) n]; print(l); });
  tryIt('listifelse', () { List<int> l = [if (false) 2 else s]; print(l); });
  tryIt('listfor', () { List<int> l = [for (var i = 0; i < 1; i++) n]; print(l); });
  tryIt('listforOk', () { List<int> l = [for (var i = 0; i < 2; i++) one]; print(l); });
  tryIt('setif', () { Set<int> st = {if (true) n}; print(st); });
  tryIt('setfor', () { Set<int> st = {for (var i in [1]) s}; print(st); });
  tryIt('mapfor', () { Map<String, int> m = {for (var i in [1]) 'k': n}; print(m); });
  tryIt('mapforOk', () { Map<String, int> m = {for (var i in [1]) 'k$i': one}; print(m); });
  tryIt('list1', () { List<int> l = [n]; print(l); });
  tryIt('list1Ok', () { List<int> l = [one, 2]; print(l); });
  tryIt('listStr', () { List<int> l = [s]; print(l); });
  tryIt('nullable', () { List<int?> l = [n]; print(l); });
  tryIt('nullableStr', () { List<int?> l = [n, s]; print(l); });
  dynamic m = <String, dynamic>{'n': 'bob', 'i': 7, 'd': 2.5, 'b': true};
  tryIt('int', () { int x = m['n']; print(x); });
  tryIt('intOk', () { int x = m['i']; print(x); });
  tryIt('double', () { double x = m['n']; print(x); });
  tryIt('doubleOk', () { double x = m['d']; print(x); });
  tryIt('string', () { String x = m['i']; print(x); });
  tryIt('bool', () { bool x = m['i']; print(x); });
  tryIt('boolOk', () { bool x = m['b']; print(x); });
  Map<String, dynamic> j = {'n': 'bob'};
  tryIt('typedMap', () { int x = j['n']; print(x); });
}
