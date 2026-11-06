import 'a.dart' as a;
import 'b.dart' as b;
import 'b.dart';
void main() {
  print(Item(3));
  final cb = b.Item.new;
  print(Function.apply(a.Item.named, [], {#v: 4}));
  final fa = a.describe;
  final fb = b.describe;
  print('${fa(a.Item(1), tag: 't')} ${fb(b.Item(2))}');
  print(Function.apply(a.describe, [a.Item(5)], {#tag: 'q'}));
  int Function(int, int) p = a.pick;
  print(p(1, 2));
  print(Function.apply(a.pick, ['u', 'v']));
  a.Item<String> Function(String) typed = a.Item.new;
  print(typed('s'));
  print([1].map(a.Item.new).toList());
  print([1].map(b.Item.new).toList());
}
