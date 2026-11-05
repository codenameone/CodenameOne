import 'shape_lib.dart' as lib;
import 'tools_lib.dart';

class Inner {
  Inner();
  String toString() => 'mainInner';
}

class Shape {
  final int n;
  Shape(this.n);
  String toString() => 'local($n)';
}

class Box {
  static T hold<T>(T v) => v;
  T pick<T>(T v) => v;
  int plain(int x) => x + 1;
}

int inc(int x) => x + 1;
T gid<T>(T x) => x;
class P<T> { final T a; final String b; P(this.a, {this.b = ''}); String toString() => 'P<$T>($a$b)'; }

void main() {
  // A static tear-off of another library, held as Object.
  Object mk = lib.Shape.make;
  final m = <Object, int>{};
  m[lib.Shape.make] = 1;
  m[lib.Shape.twice] = 2;
  print('xlib ${m.length} ${m[mk]} ${mk == lib.Shape.make}');
  print('xlib call ${(mk as lib.Shape Function(String))("a")} ${(lib.Shape.twice as Object) == lib.Shape.twice}');
  Object kp = lib.Shape.keep;
  print('xlib generic ${kp is Function}');
  // toString of a tear-off agrees with its canonical value.
  Object io = inc;
  print('str ${inc.toString() == io.toString()} ${Box.hold.toString() == (Box.hold as Object).toString()}');
  // A generic instance tear-off with no optional parameters, held as Function.
  final b = Box();
  Function pf = b.pick;
  print('pick ${pf(3)} ${pf("s")}');
  Function pl = b.plain;
  print('plain ${pl(4)}');
  // A generic static method held as Object.
  Object ho = Box.hold;
  print('hold ${ho is Function} ${ho == Box.hold}');
  final hs = <Object>{Box.hold, ho, inc, io};
  print('set ${hs.length}');
  Object tm = Tools.mk;
  final tmap = <Object, int>{Tools.mk: 7};
  tmap[Tools.mk] = 8;
  tmap[lib.Shape.twice] = 9;
  print('tools2 ${tmap.length} ${(Tools.mk as Object) == tm} ${tmap[Tools.mk]}');
  print('tools ${tmap[tm]} ${(tm as Function)("q")} ${Inner()} ${tm == Tools.mk}');
  print(Tools.viaBase());
  int Function(int) gi = gid; int Function(int) gi2 = gid;
  var ge = gid<int>;
  print('gen ${gi == gi2} ${gi == ge} ${{gi, gi2, ge}.length} ${gi.toString() == ge.toString()}');
  Object Function(String) pn = P.new;
  print('infer ${pn("x")} ${Function.apply(P<int>.new, [2], {#b: "!"})}');
  try { Function.apply(gid, [1], {#nope: 1}); } catch (e) { print('apply nsm'); }
  var pv = P<int>.new; dynamic pd = P<num>.new;
  print('named ctor ${pv(3, b: "?")} ${pd(4, b: "#")}');
  print('local ${Shape(1)} ${lib.Shape.make("z")}');
}
