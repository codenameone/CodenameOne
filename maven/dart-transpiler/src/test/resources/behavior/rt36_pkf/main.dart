import 'base.dart' as base;
import 'mine.dart';
import 'mine.dart' as m;
class Comp<T extends Comparable<T>> { T? best; void add(T v) { if (best == null || v.compareTo(best!) > 0) best = v; } }
class Node<T extends Node<T>> {}
class Leaf extends Node<Leaf> {}
void main() {
  print(m.Widget().build()); print(Text('a').build()); print(base.Text('b').build());
  base.Widget w = m.Text('c'); print(w.build()); print(w is base.Text); print(w is m.Text);
  final List<base.Widget> ws = [base.Widget(), m.Widget(), m.Text('d')]; print(ws.map((x) => x.build()).join(','));
  final c = Comp<String>(); c.add('a'); c.add('z'); print(c.best);
  final cf = Comp<String>.new; print(cf().runtimeType);
  print(Node<Leaf>().runtimeType); final ln = Leaf.new; print(ln().runtimeType);
  print(m.Text.new('e').build()); final tf = base.Text.new; print(tf('f').build());
  print(m.Widget().runtimeType == base.Widget);
}
