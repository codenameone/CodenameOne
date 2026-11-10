import 'ls_a.dart';
import 'ls_b.dart';

class _Named {
  int _name$p3 = 3;
  int _name = 4;
}

void main() {
  print(LA().sum());
  print(LB().scopes(true));
  print(LB().scopes(false));
  print(topLevel());
  print(symbolA());
  print(symbolB());
  print(symbolA() == symbolB());
  print(symbolDollar());
  print(#_name$p3);
  print(#_name$p3 == symbolDollar());
  print(Symbol('_name\$p3'));
  final n = _Named();
  print(n._name$p3 + n._name);
  spyB(Spy());
}
