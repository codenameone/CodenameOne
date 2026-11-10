import 'a.dart' as a;
import 'b.dart' as b;
void main() {
  a.Id x = a.Id(5);
  b.Id y = b.Id('abc');
  print(x.w);
  print(y.w);
  print(<a.Id>[x].first.w);
  Object o = y;
  print(o is b.Id);
}
