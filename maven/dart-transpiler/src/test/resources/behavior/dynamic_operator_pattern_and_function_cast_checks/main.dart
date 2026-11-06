// Dynamic dispatch of core members checks argument types as Dart does, String
// members take any Pattern, and casts of Function values use the declared type.
void te(String label, void Function() body) {
  try {
    body();
    print('$label no error');
  } catch (e) {
    print('$label ${e is TypeError}');
  }
}

void main() {
  dynamic s = 'abcab';
  print(s.startsWith(RegExp('a')));
  print(s.startsWith(RegExp('b'), 1));
  print(s.startsWith(RegExp('c')));
  print(s.contains(RegExp('c.')));
  print(s.contains(RegExp('a'), 4));
  print(s.indexOf(RegExp('b')));
  print(s.indexOf(RegExp('b'), 2));
  print(s.lastIndexOf(RegExp('a')));
  print(s.lastIndexOf('a', 2));
  print(s.replaceAll(RegExp('[ab]'), '-'));
  print(s.replaceFirst(RegExp('b'), 'B'));
  print(s.replaceFirst('a', 'A', 1));
  print(s.split(RegExp('c')));
  dynamic r = RegExp('a.');
  print(r.allMatches(s).map((m) => m.start).toList());
  te('startsWith int', () => s.startsWith(1));
  te('contains int', () => s.contains(2));
  te('replaceAll to int', () => s.replaceAll('a', 3));
  te('lastIndexOf null', () => s.lastIndexOf(null));
  te('allMatches int', () => r.allMatches(4));

  dynamic n = 3;
  te('num +', () => n + 'a');
  te('num -', () => n - 'a');
  te('num *', () => n * true);
  te('num /', () => n / 'a');
  te('num ~/', () => n ~/ 'a');
  te('num %', () => n % 'a');
  te('num <', () => n < 'a');
  te('int &', () => n & 1.5);
  te('int <<', () => n << 'a');
  dynamic d = 2.5;
  te('double +', () => d + 'a');
  dynamic str = 'ab';
  te('String *', () => str * 'x');
  te('String * double', () => str * 1.5);
  te('String +', () => str + 1);

  dynamic l = [3, 1, 2];
  te('where', () => l.where(5));
  te('map', () => l.map('x'));
  te('any', () => l.any(1));
  te('every', () => l.every(1));
  te('forEach', () => l.forEach(1));
  te('firstWhere', () => l.firstWhere(1));
  te('fold', () => l.fold(0, 1));
  te('reduce', () => l.reduce(1));
  te('expand', () => l.expand(1));
  te('sort', () => l.sort(1));
  print(l.expand((x) => [x, x]).toList());
  dynamic dm = {'a': 1};
  te('map forEach', () => dm.forEach(1));

  dynamic m = <String, List<int>>{};
  m['k'] = <int>[1];
  print(m['k']);
  te('map value elements', () => m['k'] = <String>['a']);
  dynamic ll = <List<int>>[];
  ll.add(<int>[1]);
  te('list add elements', () => ll.add(<String>['a']));
  ll.add(<int>[2]);
  te('list []= elements', () => ll[0] = <String>['b']);
  dynamic st = <Set<int>>{};
  te('set add elements', () => st.add(<String>{'c'}));
  print('$ll ${st.length}');

  Function f = (int x) => x + 1;
  te('Function as', () => f as String Function(String));
  int Function(int) g = f as int Function(int);
  print(g(2));
  print(f is int Function(int));
  print(f is String Function(String));
  Object o = f;
  print(o is int Function(int));
  print(o is Function);
  print(o is String Function(String));
  te('Object as', () => o as String Function(String));

  print(ArgumentError().message);
  print(ArgumentError());
}
