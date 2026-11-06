class A<T extends int> {
  T v; A(this.v);
  int q() => v ~/ 2;
  int r() => v % 3;
  bool big() => v > 5;
  int neg() => -v;
}
class B<T extends num, U extends T> {
  U u; B(this.u);
  num dbl() => u * 2;
  bool lt(T t) => u < t;
}
class C<T extends num?> {
  T v; C(this.v);
  num orZero() => (v ?? 0) + 1;
}
T maxOf<T extends num>(T a, T b) => a > b ? a : b;
num sumAll<T extends num>(List<T> xs) { num s = 0; for (final x in xs) { s = s + x; } return s; }
double half<T extends double>(T d) => d / 2;
void main() {
  final a = A<int>(13);
  print([a.q(), a.r(), a.big(), a.neg()]);
  final b = B<num, int>(7);
  print([b.dbl(), b.lt(3), b.lt(9.5)]);
  print(C<int?>(null).orZero());
  print(C<double?>(2.5).orZero());
  print(maxOf(3, 9));
  print(maxOf(2.5, 1.0));
  print(sumAll([1, 2, 3]));
  print(half(5.0));
  print(a.v + a.v);
}
