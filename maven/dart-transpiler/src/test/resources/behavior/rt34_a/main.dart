int top(int x) => x + 1;
int top2(int x) => x + 2;
class C { static int st(int x) => x * 2; static String sg(String s) => s; }
T gid<T>(T x) => x;
class G<T> { final T v; G(this.v); G.named(this.v); String toString() => 'G<$T>($v)'; }
typedef IF = int Function(int);
void main() {
  Object o = top; IF f = top; Function g = top;
  final hs = <Object>{o, f, g, top};
  print('set1 ${hs.length} ${hs.contains(top)}');
  final m = <Object, int>{};
  m[top] = 1; m[o] = 2; m[C.st] = 3; m[C.st] = 4; m[top2] = 5;
  print('map ${m.length} ${m[top]} ${m[C.st]}');
  print('hash ${top.hashCode == o.hashCode} ${C.st.hashCode == (C.st as Object).hashCode}');
  // closures
  var cl = (int x) => x;
  var cl2 = (int x) => x;
  print('cl ${cl == cl} ${cl == cl2} ${{cl, cl2, cl}.length} ${cl.hashCode == cl.hashCode}');
  // instantiated generic
  int Function(int) gi = gid; String Function(String) gs = gid;
  int Function(int) gi2 = gid;
  print('gen ${gi == gi2} ${(gi as Object) == (gs as Object)} ${{gi, gi2, gs}.length}');
  // ctor tear-offs
  G<int> Function(int) c1 = G.new; G<String> Function(String) c2 = G.new;
  G<int> Function(int) c3 = G.new; Object Function(int) c4 = G.new;
  print('ctor ${c1 == c3} ${(c1 as Object) == (c2 as Object)} ${c1(3)} ${c2("a")} ${c4(5)} ${{c1, c2, c3}.length}');
  G<num> Function(int) c5 = G<num>.new;
  print('ctor2 ${c5(1)} ${(c5 as Object) == (c1 as Object)} ${G<int>.named == G<int>.named} ${G<int>.new == G<num>.new}');
  int sum = 0; for (var i = 0; i < 20000; i++) { sum += {top, C.st, cl}.length; }
  print('loop $sum');
}
