typedef Cb = int Function(String);
void callDyn(dynamic f, dynamic a) { try { print(f(a)); } catch (e) { print('TE ${e is TypeError}'); } }
int len(String s) => s.length;
class Btn { final void Function()? onTap; Btn(this.onTap); }
void main() {
  callDyn(len, 'abc'); callDyn(len, 3);
  callDyn((int x) => x * 2, 4); callDyn((int x) => x * 2, 'q');
  dynamic d = (String s, [int n = 1]) => s * n; print(d('a', 3)); print(d('b'));
  dynamic named = ({required int a, int b = 2}) => a + b; print(named(a: 1)); try { named(a: 'x'); } catch (e) { print(e is TypeError); }
  Object o = len; print(o is Cb); print(o is int Function(Object)); print(o is Object Function(String));
  print(o is Function); print((o as Cb)('zz'));
  dynamic h = <T>(T x) => x; print(h<int>(3)); print(h('s'));
  List<dynamic> fs = [len, (int i) => i]; for (final f in fs) { print(f is int Function(String)); }
  final b = Btn(() => print('tap')); dynamic bb = b; bb.onTap();
  final m = <String, dynamic>{'cb': (int v) => v + 1}; print(m['cb'](1));
  try { print(m['cb']('1')); } catch (e) { print(e is TypeError); }
  void Function(int) vf = (num n) => print(n); vf(7); dynamic vd = vf; try { vd(1.5); } catch(e) { print('vd ${e is TypeError}'); }
}
