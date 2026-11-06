import 'a.dart';
import 'b.dart';
class M { String _n() => 'M._n'; String get _g => 'M._g'; }
void main() {
  final b = B();
  print(b.callN());
  print(b.callB());
  print(b.interp());
  print(b.dyn());
  print(b.dynB(b));
  print(b.dynB(A()));
  print(b);
  print(eaNames());
  print(ebNames());
  print(callZZ(NB()));
  print(callZZb(NB()));
  print(callZZ(NA()));
  print(callZZb(NA()));
  print(C().useM());
  print(C().own());
  try { dynamic d = b; print(d._n()); } catch (e) { print('main nsm'); }
  try { dynamic d = b; print(d._g); } catch (e) { print('main nsm g'); }
  print(M()._n() + M()._g);
  print(aTag() + bTag() + aF() + bF());
}
