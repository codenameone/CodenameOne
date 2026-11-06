extension ON on String? { String tag() => 'nullable'; }
extension NN on String { String tag() => 'nonnull'; }
void sh<T>(T x) { print('$T'); }

class A {
  final String? _f;
  A(this._f);
  void m() {
    if (_f != null) sh(_f);
    if (this._f != null) print(this._f.tag());
    if (_f != null) { other(); sh(_f); }
  }
  void other() {}
  Future<void> am() async {
    if (_f != null) { await Future.value(1); sh(_f); }
  }
}

class NSM implements A {
  @override
  dynamic noSuchMethod(Invocation i) => super.noSuchMethod(i);
}

class B {
  final String? _g;
  B(this._g);
}
class NB implements B {
  dynamic noSuchMethod(Invocation i) => 'x';
}
void useB(B b) {
  if (b._g != null) print(b._g.tag());
}

class C {
  final String? _h;
  C(this._h);
}
class C2 {
  String? get _h => null;
}
void useC(C c) { if (c._h != null) print(c._h.tag()); }

class D {
  String? _v;
  final String? _w = 'w';
  D(this._v);
}
void useD(D d) {
  if (d._v != null) print(d._v.tag());
  if (d._w != null) print(d._w.tag());
}

class E {
  final String? _e = 'e';
}
class E2 extends E {
  String? get _e => 'over';
}
void useE(E e) { if (e._e != null) print(e._e.tag()); }

void main() async {
  A('a').m();
  await A('a').am();
  useB(B('b'));
  useC(C('c'));
  useD(D('d'));
  useE(E());
}
