class C {
  static String so<T>(T x, [T? y]) => 'so $T:$x:$y';
  String im<T>(T x, [T? y]) => 'im $T:$x:$y';
  String nm<T>(T x, {T? y}) => 'nm $T:$x:$y';
}

List<Object> inGeneric<T>(C c) {
  String Function(T, [T?]) p = c.im;
  String Function(T, {T? y}) n = c.nm;
  return [p, n];
}

void main() {
  var c = C(), d = C();
  String Function(int, [int?]) p1 = c.im, p2 = c.im, p3 = d.im;
  String Function(String, [String?]) p4 = c.im;
  print(p1(1) + ' ' + p1(1, 2) + ' ' + p4('a', 'b'));
  print('${p1 == p2} ${p1 == p3} ${(p1 as Object) == p4} ${identical(p1, p2)} ${p1.hashCode == p2.hashCode}');
  String Function(int, {int? y}) n1 = c.nm, n2 = c.nm;
  print(n1(3) + ' ' + n1(3, y: 4));
  print('${n1 == n2} ${n1.hashCode == n2.hashCode} ${{n1, n2}.length} ${{p1, p2, p3, p4}.length}');
  var g1 = inGeneric<int>(c), g2 = inGeneric<int>(c), g3 = inGeneric<double>(c);
  print('${g1[0] == g2[0]} ${g1[1] == g2[1]} ${g1[0] == g3[0]} ${g1[0] == p1} ${g1[1] == n1}');
  print((g3[0] as String Function(double, [double?]))(1.5));
  print((g3[1] as String Function(double, {double? y}))(1.5, y: 2.5));
  extra();
}

String topo<T>(T x, [T? y]) => 'topo $T:$x:$y';

List<Object> topGeneric<T>() {
  String Function(T, [T?]) f = topo;
  String Function(T, [T?]) g = C.so;
  return [f, g];
}

void extra() {
  String Function(int, [int?]) a = topo, b = topo;
  String Function(String, [String?]) s = topo;
  print('${a == b} ${identical(a, b)} ${(a as Object) == s} ${{a, b, s}.length} ${a(1, 2)}');
  var g1 = topGeneric<int>(), g2 = topGeneric<int>();
  print('${g1[0] == g2[0]} ${identical(g1[0], g2[0])} ${g1[0] == a} ${g1[0].hashCode == a.hashCode}');
  String Function(int, [int?]) s1 = C.so, s2 = C.so;
  print('${s1 == s2} ${identical(s1, s2)} ${g1[1] == s1} ${g1[1] == g2[1]} ${{s1, s2, g1[1]}.length} ${s1(5)}');
}
