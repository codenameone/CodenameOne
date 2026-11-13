String greet(String name, [String? suffix = '!']) => '$name${suffix ?? '<null>'}';

int addTo(int a, {int by = 10, int? cap}) {
  final r = a + by;
  return cap != null && r > cap ? cap : r;
}

class Fmt {
  final String prefix;
  Fmt(this.prefix);

  static String wrap(String s, [String? left = '[', String right = ']']) =>
      '${left ?? '<null>'}$s$right';

  String tag(String s, {String? sep = ':'}) => '$prefix${sep ?? '<null>'}$s';

  String apply(String s) {
    String Function(String, {String? sep}) f = tag;
    return f(s) + ' ' + f(s, sep: null) + ' ' + f(s, sep: '=');
  }
}

void main() {
  String Function(String, [String?]) g = greet;
  print(g('a'));
  print(g('b', null));
  print(g('c', '?'));

  int Function(int, {int by, int? cap}) h = addTo;
  print(h(1));
  print(h(1, by: 2));
  print(h(1, cap: 5));
  print(h(1, by: 3, cap: null));

  String Function(String, [String?, String]) w = Fmt.wrap;
  print(w('x'));
  print(w('x', null));
  print(w('x', '(', ')'));

  final fmt = Fmt('p');
  String Function(String, {String? sep}) t = fmt.tag;
  print(t('y'));
  print(t('y', sep: null));
  print(t('y', sep: '-'));
  print(fmt.apply('z'));

  String Function(String, [String?]) lam = (s, [String? x = 'dflt']) => '$s/${x ?? '<null>'}';
  print(lam('l'));
  print(lam('l', null));
  print(lam('l', 'v'));
}
