String top<T>(T x) => '$T:$x';

class C {
  static String sm<T>(T x) => 'sm $T:$x';
  String im<T>(T x) => 'im $T:$x';
}

void report(String label, Object a, Object b) {
  print('$label == ${a == b} identical ${identical(a, b)} hash ${a == b ? a.hashCode == b.hashCode : "-"}');
}

List<Object> inGeneric<T>(C c) {
  String Function(T) s = C.sm;
  String Function(T) t = top;
  String Function(T) i = c.im;
  return [s, t, i];
}

void main() {
  String Function(int) a = C.sm, b = C.sm;
  report('static', a, b);
  String Function(String) as = C.sm;
  report('static int/String', a, as);
  String Function(int) t1 = top, t2 = top;
  report('top', t1, t2);
  String Function(double) t3 = top;
  report('top int/double', t1, t3);
  var c = C(), d = C();
  String Function(int) i1 = c.im, i2 = c.im, i3 = d.im;
  report('inst same', i1, i2);
  report('inst other recv', i1, i3);
  String Function(bool) i4 = c.im;
  report('inst int/bool', i1, i4);
  print({a, b, as}.length);
  print({t1, t2, t3}.length);
  print({i1, i2, i3, i4}.length);
  print(<Object>{a}.contains(b));
  print(<Object>{i1}.contains(i2));
  var g1 = inGeneric<int>(c), g2 = inGeneric<int>(c), g3 = inGeneric<String>(c);
  for (var k = 0; k < 3; k++) {
    report('generic $k int/int', g1[k], g2[k]);
    report('generic $k int/String', g1[k], g3[k]);
  }
  report('generic static vs direct', g1[0], a);
  report('generic top vs direct', g1[1], t1);
  report('generic inst vs direct', g1[2], i1);
  print(a(1) + ' ' + t3(2.5) + ' ' + i4(true));
  print((g3[0] as String Function(String))('x'));
}
