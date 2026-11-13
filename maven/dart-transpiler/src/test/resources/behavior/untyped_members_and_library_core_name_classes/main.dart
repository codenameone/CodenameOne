import 'lib.dart' as l;
class A { toString() => 'a'; foo() => 1; get g => 2; }
void main() {
  print(A()); print(A().foo()); print(A().g);
  print(l.String('x').s()); print(l.String('x')); print(l.String('y').runtimeType);
  print(l.mk().length);
}
