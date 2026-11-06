import 'lib_b.dart' show Shelf, Tagger;
import 'lib_a.dart' hide Shelf, Tagger;

class Mixed with Tagger {}

String useC() {
  final s = Shelf<String>();
  s.put('k');
  return '${s.count('k')} ${Mixed().tag('c')} ${Box(2).show()} ${Sizer().size('xy')}';
}
