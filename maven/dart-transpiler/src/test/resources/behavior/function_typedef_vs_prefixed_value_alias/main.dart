import 'f.dart';
import 'g.dart' as g;
void main() {
  Op o = (x) => x + 1;
  print(applyF(o, 4));
  print(o(10));
  g.Op s = 'hi';
  print(g.shout(s));
  print(s.length);
  Object a = o;
  print(a is Op);
  Object b = 'x';
  print(b is g.Op);
  print(b is Op);
  print(Op == Op);
  print(g.Op);
  g.Op c = b as g.Op;
  print(c);
  List<Op> ops = [o, (x) => x * 3];
  print(ops.map((f) => f(2)).toList());
}
