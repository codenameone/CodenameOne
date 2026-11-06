import 'other.dart';
extension ON on String? { String tag() => 'nullable'; }
extension NN on String { String tag() => 'nonnull'; }
void sh<T>(T x) { print('$T'); }

class P {
  final String? _p;
  P(this._p);
  void m() {
    if (this._p != null) print(this._p.tag());
    if (_p != null) print(this._p.tag());
    if (this._p != null) sh(_p);
  }
  Future<void> am() async {
    if (_p != null) { await Future.value(1); print(_p.tag()); sh(_p); }
  }
}
void useP(P p) {
  if (p._p != null) print(p._p.tag());
  final q = p;
  if (q._p == null) return;
  print(q._p.tag());
}
class G<T extends Object> {
  final T? _t;
  G(this._t);
  String run() => _t != null ? '${_t.hashCode >= 0}' : 'none';
}
void main() async {
  P('a').m();
  await P('b').am();
  useP(P('c'));
  useP(Sub());
  print(G<int>(3).run());
  String? x = 'v';
  x!;
  print(x.tag());
  String? y = 'w';
  print(y != null ? y.tag() : 'z');
  String? z = 'u' as String?;
  print((z == null ? 'n' : z).tag());
}
