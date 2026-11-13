String desc(Object o) => switch (o) {
  {'type': 'pt', 'x': int x, 'y': int y} => 'point $x,$y',
  {'type': 'name', 'v': String v} => 'name $v',
  {'n': null} => 'explicit null',
  Map() => 'map other',
  _ => 'not map',
};
void main() {
  var [h, ...t] = [10, 20, 30];
  print('$h $t ${t.runtimeType}');
  List<num> nums = [1, 2.5, 3];
  if (nums case [var f, ...var r]) print('$f $r ${r.runtimeType}');
  var [...init, last] = <String>['a', 'b', 'c'];
  print('$init ${init.runtimeType} $last');
  print(desc({'type': 'pt', 'x': 1, 'y': 2}));
  print(desc({'type': 'pt', 'x': 1}));
  print(desc({'type': 'name', 'v': 'q', 'extra': 1}));
  print(desc({'n': null}));
  print(desc({'m': null}));
  print(desc(3));
  final m = {'a': 1, 'b': 'two'};
  if (m case {'a': int a, 'b': String b}) print('if $a $b');
  if (m case {'c': _}) print('bad'); else print('no c');
  var {'a': av, 'b': bv} = m;
  print('decl $av $bv');
  final {'k': List<int> ks} = {'k': [1, 2]};
  print(ks);
  switch (<String, Object?>{'x': [1, 2]}) {
    case {'x': [var p, var q]}:
      print('nested $p $q');
  }
}
