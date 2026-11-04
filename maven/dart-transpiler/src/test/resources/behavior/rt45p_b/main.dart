import 'lib.dart' show Id, Util;
import 'lib.dart' as p;
extension _Priv on int {
  static int _k = 1;
  static int bump() => ++_k;
}
void main() {
  print(Util.make(3));
  print(Util.count);
  Util.count += 5;
  print(Util.count);
  Util.count++;
  print(p.Util.count);
  print(Util.peek());
  print(Id.next().v);
  Id.created += 10;
  print(Id.next().v);
  print(p.Id.created);
  print(_Priv.bump());
  _Priv._k *= 3;
  print(_Priv._k);
  var f = Util.make;
  print(f(2));
}
