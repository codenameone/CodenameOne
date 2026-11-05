void top() {}
void top2(int x) {}
class P { void m() { print('P.m'); } }
mixin Mx on P { @override void m() { print('Mx.m'); } void viaSuper(List<void Function()> out) { out.add(super.m); out.add(m); } }
class K extends P with Mx {
  @override void m() { print('K.m'); }
  List<void Function()> both() => [super.m, m, this.m];
}
typedef VF = void Function();
void main() {
  var k = K();
  var l = k.both();
  print('sup ${l[0] == l[1]} ${l[1] == l[2]}');
  for (var f in l) f();
  var o = <void Function()>[];
  k.viaSuper(o);
  print('mx ${o[0] == o[1]} ${o[0] == l[0]}');
  for (var f in o) f();
  Object a = top; VF b = top; Function c = top;
  print('st ${a == b} ${b == c}');
  print('sthash ${a.hashCode == b.hashCode} ${b.hashCode == c.hashCode}');
  var s = <Object>{a, b, c};
  print('stset ${s.length}');
  void Function(int) t2 = top2; Object t2o = top2;
  print('st2 ${t2 == t2o} ${<Object>{t2, t2o}.length}');
}
