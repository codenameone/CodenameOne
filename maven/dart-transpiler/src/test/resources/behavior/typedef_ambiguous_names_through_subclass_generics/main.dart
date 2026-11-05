import 'r.dart';
typedef Val$a0 = double;
typedef Val$a1 = bool;
typedef Val$a2 = String;
void main() {
  final m = Mid();
  print(m.box().v + 1);
  m.cb().v('hi');
  print(m.tag().length);
  print(m.inc()(4));
  Val$a0 d = 1.5;
  Val$a1 b = false;
  Val$a2 s = 'z';
  print(d * 2); print(!b); print(s + s);
  print(m.box().runtimeType);
}
