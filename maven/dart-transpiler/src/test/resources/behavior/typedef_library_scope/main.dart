import 'a.dart' as a;
import 'b.dart' as b;
import 'c.dart' as c;
import 'd.dart';
import 'e.dart' as e;
void main() {
  Duration d = Duration(seconds: 3);
  print(d.inMilliseconds);
  print(a.twice(21));
  print(b.up('x'));
  print(c.inc(1));
  b.Id s = b.mk();
  c.Id n = c.mk();
  print(s.length);
  print(n + 1);
  Label l = Label('q');
  print(l.show());
  print(e.half(5.0));
  List<Duration> ds = [Duration(minutes: 1)];
  print(ds.first.inSeconds);
}
