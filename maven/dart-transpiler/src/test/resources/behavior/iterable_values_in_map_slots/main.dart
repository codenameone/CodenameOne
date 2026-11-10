const Map<String, Iterable<int>> cm = {'a': [1, 2], 'b': {3}};
void main() {
  Map<String, Iterable<int>> m = {'z': [8], 's': {4, 5}};
  print(m['z']!.first);
  print(m['s']!.length);
  print(cm['a']!.last);
  print(cm['b']!.first);
  Map<String, Iterable<int>?> mn = {'n': null, 'l': [9]};
  print(mn['l']?.first);
  print(mn['n']);
  List<Iterable<String>> li = [['x'], {'y'}];
  print(li.map((e) => e.first).join());
  Set<Iterable<int>> si = {[1], [2, 3]};
  print(si.map((e) => e.length).toList());
  Map<String, Map<String, Iterable<int>>> nm = {'o': {'i': [7, 8]}};
  print(nm['o']!['i']!.last);
  List<Map<int, Iterable<String>>> lm = [{1: ['p']}];
  print(lm[0][1]!.first);
  Map<String, Iterable<Iterable<int>>> deep = {'d': [[1], {2}]};
  print(deep['d']!.map((e) => e.first).toList());
  Map<String, Iterable<int>> fromMap = {'k': {1: 2}.keys};
  print(fromMap['k']!.first);
}
