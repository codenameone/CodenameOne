class C {
  int n = 0;
  void tick() { n++; }
  int get g => n;
  C operator +(C o) => this;
  T id<T>(T x) => x;
}
class Base { String m() => 'base'; }
class Sub extends Base {
  @override String m() => 'sub';
  String Function() sup() => super.m;
}
mixin M { String mm() => 'mm'; }
class WithM with M {}
extension E on C { int twice() => n * 2; }

void Function() tk(C c) => c.tick;
String Function() wm(WithM w) => w.mm;
int Function() tw(C c) => c.twice;
String Function() sm(Sub s) => s.m;
T Function<T>(T) gid(C c) => c.id;
void main() {
  final c = C();
  final s = <Function>{};
  s.add(tk(c)); s.add(tk(c));
  print('set len ${s.length} contains ${s.contains(tk(c))}');
  final m = <Function, int>{};
  m[tk(c)] = 1; m[tk(c)] = 2;
  print('map len ${m.length} val ${m[tk(c)]}');
  final cb = tk(c);
  final list = <void Function()>[cb];
  list.remove(tk(c));
  print('list after remove ${list.length}');
  var x = tk(c);
  print('x==tk(c) ${x == tk(c)} identical ${identical(x, tk(c))}');
  print('hash eq ${tk(c).hashCode == tk(c).hashCode}');
  final c2 = C();
  print('diff recv ${tk(c) == tk(c2)}');
  print('generic ${gid(c) == gid(c)}');
  int Function(int) gi = gid(c);
  print('generic view ${gi(4)}');
  final sb = Sub();
  print('super ${sb.sup() == sb.sup()} ${sb.sup()()} vs m ${sb.sup() == sm(sb)}');
  final w = WithM();
  print('mixin ${wm(w) == wm(w)} ${{wm(w), wm(w)}.length}');
  print('ext ${tw(c) == tw(c)}');
  Object o = tk(c); Function f = tk(c);
  print('views ${o == f} ${{o, f}.length}');
  print('str ${[tk(c)].indexOf(tk(c))} ${[tk(c)].contains(tk(c))}');
  Object v = 3;
  final fn = () { if (v is int) { return v + 1; } return 0; };
  print('cond ${fn()}');
  final g = v is int ? () { if (v is int) return v * 2; return -1; } : () => 0;
  print('cond2 ${g()}');
}
