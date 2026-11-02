String show(int x) => 'v$x';
int twice(int x, {required int k}) => x * k;

class Holder {
  String name(int n) => 'h$n';
}

void main() {
  List<Object> ls = <Object>[1, 2];
  final ci = ls.cast<int>();
  print(ci is List<int>);
  print(ci is List<String>);
  print(ci[0] + 1);
  final cs = ls.cast<String>();
  print(cs is List<String>);
  try {
    print(cs[0].length);
  } on TypeError {
    print('read checked');
  }
  final names = <Object>['a', 'b'].cast<String>();
  print(names is List<String>);
  print(names.map((s) => s.length).toList());
  final st = <Object>{'x'}.cast<String>();
  print(st is Set<String>);
  print(st is Set<int>);
  final Map<Object, Object> m = <Object, Object>{'a': 1};
  final cm = m.cast<String, int>();
  print(cm is Map<String, int>);
  print(cm is Map<int, int>);
  print(cm['a']);
  final it = ls.map((e) => '$e');
  print(it.toList() is List<String>);
  print(ls.toList() is List<Object>);
  print(ls.toList() is List<int>);
  print(ls.where((e) => true).toList() is List<int>);

  String Function(int) f = (int x) => 'f$x';
  print(f.runtimeType);
  print(show.runtimeType);
  print(Holder().name.runtimeType);
  print(f.runtimeType == show.runtimeType);
  print(twice.runtimeType);
  int Function(int, [String]) g = (int a, [String b = '']) => a;
  print(g.runtimeType);

  print(twice is int Function(int, {required int k}));
  print(twice is int Function(int, {int k}));
  Object h = (int x, {int k = 1}) => x;
  print(h is int Function(int, {required int k}));
  print(h is int Function(int, {int k}));
}
