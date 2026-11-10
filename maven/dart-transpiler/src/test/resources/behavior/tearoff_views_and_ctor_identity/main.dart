int twice(int x) => x * 2;
T gen<T>(T v) => v;

class P {
  final int v;
  P(this.v);
  P.named(this.v);
}

class G<T> {
  final T v;
  G(this.v);
}

void takeG(G<bool> Function(bool) f) {
  print('gctor param ${f(true).runtimeType} ${f == G<bool>.new}');
}

G<String> Function(String) makeG() => G.new;

void main() {
  Object? Function(int) wide = twice;
  dynamic Function(Never) never = twice;
  Function f = twice;
  print('wide ${wide == f} ${identical(wide, f)} ${wide.hashCode == f.hashCode} ${wide(3)}');
  print('never ${never == f} ${never.hashCode == f.hashCode} ${(never as int Function(int))(4)}');
  print('set ${<Object>{wide, never, f, twice}.length}');

  print('ctor new ${P.new == P.new} ${identical(P.new, P.new)}');
  print('ctor named ${P.named == P.named} ${P.new == P.named}');
  P Function(int) pn = P.new;
  Object po = P.new;
  print('ctor views ${pn == po} ${pn.hashCode == po.hashCode} ${pn(5).v}');

  print('gctor ${G<int>.new == G<int>.new} ${G<int>.new == G<String>.new}');
  G<int> Function(int) gi = G.new;
  print('gctor inferred ${gi == G<int>.new} ${gi.hashCode == G<int>.new.hashCode} ${gi(6).v} ${gi(1).runtimeType}');
  takeG(G.new);
  print('gctor ret ${makeG()('s').runtimeType} ${makeG() == G<String>.new}');
  List<G<double> Function(double)> gl = [G.new];
  print('gctor list ${gl[0](1.5).runtimeType} ${gl[0] == G<double>.new}');

  print('gen inst ${gen<int> == gen<int>} ${gen<int> == gen<String>}');
  int Function(int) gin = gen;
  print('gen inferred ${gin == gen<int>} ${gin.hashCode == gen<int>.hashCode} ${gin(7)}');
}
