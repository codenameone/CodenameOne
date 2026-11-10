// A member whose type is a function typedef the reading library does not see: `fmt.dart`
// is imported with `show stars`, so `Fmt` and `Pick` are invisible here, and this library
// declares its own unrelated `Fmt`. The member keeps the type it was declared with.
import 'fmt.dart' show stars;
import 'style.dart';
typedef Fmt = int;
void main() {
  final s = Style(stars, (x) => x.isEmpty);
  print(s.fmt(3));
  final f = s.fmt;
  print(f(2).length);
  print(s.pick?.call('') ?? 'none');
  print(Style((n) => 'n=$n').pick == null);
  Fmt local = 7;
  print(local + 1);
  print(Fmt);
}
