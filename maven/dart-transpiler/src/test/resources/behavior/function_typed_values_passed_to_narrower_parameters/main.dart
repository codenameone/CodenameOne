typedef Cb = void Function(num);
class W { void take(num n) => print('take $n'); }
void run(void Function(int) f) => f(3);
void main() {
  final w = W();
  run(w.take);
  Cb c = (num n) => print('c $n');
  run(c);
  void Function(Object?) wide = (o) => print('wide $o');
  run(wide);
  dynamic d = (int x) => print('narrow $x');
  try { (d as void Function(num))(1.5); } catch (e) { print('cast ${e is TypeError}'); }
  final fs = <void Function(int)>[w.take, (num n) => print('n $n')];
  for (final f in fs) f(4);
  print(fs is List<void Function(int)>);
  print((w.take) is void Function(int));
  print((w.take) is void Function(String));
}
