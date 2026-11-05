T id<T>(T x) => x;
List<T> wrap<T>(T x) => [x];
void main() {
  var a = [1, 2];
  var b = [1, 2.5];
  var c = [1, 'a'];
  var d = [1, null];
  int? n;
  var e = [n, 3];
  var f = [null];
  var g = [[1], [2.5]];
  var h = [[1], ['a']];
  var m1 = {'a': 1, 'b': 2.5};
  var m2 = {'a': 1, 'b': null};
  var m3 = {1: 'a', 'b': 'c'};
  print([a, b, c, d, e, f, g, h].map((x) => x.runtimeType).join(','));
  print([m1, m2, m3].map((x) => x.runtimeType).join(','));
  print([id(1), id(1.5), id(null), id([1])].map((x) => x.runtimeType).join(','));
  print([wrap(1), wrap(1.5), wrap(null), wrap([1])].map((x) => x.runtimeType).join(','));
  print([a is List<num>, b is List<int>, b is List<num>, g is List<List<num>>, h is List<List<Object>>, d is List<int?>, e is List<int>]);
  var hh = h as List<Object>;
  print(hh.length);
  for (var x in h) { print((x as dynamic).length); print(x.first.runtimeType); }
  print(g[1][0] + 1);
  var r = m2.remove('a'); print(r.runtimeType);
  var r2 = m2.remove('zz'); print(r2);
  var loopRes = [];
  for (var i = 0; i < 2; i++) { var q = [i, i + 0.5]; loopRes.add(q.runtimeType); }
  print(loopRes);
  var sp = [...a, ...b]; print(sp.runtimeType);
  var cf = [for (var i in a) i, if (a.isEmpty) 'x']; print(cf.runtimeType);
  try { (b as List<int>); } catch (e) { print('cast fail'); }
  print(null.runtimeType);
  dynamic nn;
  print(nn.runtimeType);
  print(<Object?>[].runtimeType);
  print(<Object?>[1, null].runtimeType);
  var nl = [null];
  print([nl is List<int?>, nl is List<int>, nl is List<Object?>]);
}
