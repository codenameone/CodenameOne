int perf(int a, int b) {
  var q = a ~/ b; var r = a % b; var n = -a; var ab = a.abs(); var c = a.clamp(0, 10);
  return q + r + n + ab + c;
}
double perfD(double a, double b) {
  var r = a % b; var n = -a; var ab = a.abs(); var c = a.clamp(0.0, 10.0);
  return r + n + ab + c;
}
int perfDI(double a, double b) => a ~/ b;
void main() {
  print(perf(-7, 2)); print(perf(7, -2)); print(perfD(-7.5, 2.0)); print(perfDI(-7.5, 2.0));
  print(-7 ~/ 2); print(7 ~/ -2); print(-7 % 2); print(7 % -2); print(-7.5 % 2); print(7.5 % -2);
  print(7.9 ~/ 2); print(-7.9 ~/ 2); print((7.0 ~/ 2).runtimeType);
  num x = 5; num y = 2.5; print(x ~/ y); print(x % y); print(-x); print(-y);
  try { print(5 ~/ 0.0); } catch (e) { print(e is UnsupportedError); }
  try { print(5 ~/ 0); } catch (e) { print(e.runtimeType); }
  print(5.clamp(0, 2.5)); print(1.5.clamp(2, 3)); print(5.clamp(0.0, 10)); print((5.clamp(0.0,10)).runtimeType);
  num z = 7; print(z.clamp(0, 3)); print(z.remainder(-3)); print((-7).remainder(3)); print(z.abs());
  print(double.nan.compareTo(1.0)); print(1.0.compareTo(double.nan)); print(double.nan.compareTo(double.nan));
  num nn = double.nan; print(nn.compareTo(0)); print((0 as num).compareTo(double.nan));
  print((-0.0).compareTo(0.0));
  var q = 10; q ~/= 3; print(q); var w = 10.5; w %= 3; print(w); num v = 9; v ~/= 2; print(v);
  int zero = 0; int five = 5; double half = 0.5;
  try { print(five % zero); } catch (e) { print('${e.runtimeType} ${e is UnsupportedError} ${e is IntegerDivisionByZeroException}'); }
  try { print(five.remainder(zero)); } catch (e) { print(e); }
  try { print(five ~/ zero); } on IntegerDivisionByZeroException catch (e) { print('caught $e'); }
  try { print(five ~/ zero); } on UnsupportedError catch (e) { print('unsupported $e'); }
  print(five.clamp(half, 3)); print(half.clamp(1, 2.0)); print(half.clamp(0.0, 1.0)); print(five.clamp(1, 3));
  print(five.clamp(1, 3).runtimeType); print(half.clamp(1, 2).runtimeType);
}
