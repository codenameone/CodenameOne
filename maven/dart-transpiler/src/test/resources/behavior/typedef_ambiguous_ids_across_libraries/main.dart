import 're.dart';
import 'lib.dart' as L;
import 'a.dart' as A;
typedef _Id = bool;
class Sub extends A.Base with A.M {}
T apply<T>(T Function(T) f, T v) => f(v);
void main() {
  Id s = 'hey';
  print(s.shout());
  print(Holder<Id>('abcd').len());
  print(L.partId() + 1);
  print(L.partFn()(4));
  final sub = Sub();
  print(sub.ids().first + 5);
  print(sub.twice());
  print(sub.op()(3));
  Fn f = (x) => x + x;
  print(apply<String>(f, 'q'));
  A.Fn g = (x) => x - 1;
  print(apply<int>(g, 9));
  _Id b = true;
  print(!b);
  print(Id);
  print(A.Id);
  print(Fn);
  print(A.Fn);
  print(f.runtimeType);
  print(<Id>[].runtimeType);
  print(<A.Id>[].runtimeType);
  print(s is Id);
  try { (1 as dynamic) as Id; } catch (e) { print(e is TypeError); }
}
