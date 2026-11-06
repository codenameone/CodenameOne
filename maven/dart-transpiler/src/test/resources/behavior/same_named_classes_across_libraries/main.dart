import 'lib_a.dart';
import 'lib_b.dart' as b;
import 'lib_c.dart';

class Local with Tagger implements Sizer {
  int size(String s) => s.length + 100;
}

void main() {
  print(useA());
  print(b.useB());
  final a = Shelf<int>();
  a.put(7);
  print('${a.first() * 2} ${a.describe()}');
  final bs = b.Shelf<int>();
  bs.put(3);
  bs.put(3);
  print('${bs.count(3)} ${bs.describe()}');
  print('${Box(9).show()} ${b.Box('z').show()}');
  final l = Local();
  print('${l.tag('m')} ${l.size('ab')}');
  b.Sizer bz = b.LoudSizer();
  Sizer az = Local();
  print('${bz.size('abcd')} ${az.size('a')} ${bz is b.Sizer} ${az is b.Sizer}');
  print(useC());
}
