import 'md_barrel.dart' as md;

String describe(md.Match m) => m.text;

void main() {
  final m = md.Match('hit');
  print(describe(m));
  Object o = m;
  print(o is md.Match);
  print(o is Match);
  final re = RegExp('a(b)').firstMatch('xab');
  print(re is Match);
  print(re!.group(1));
  final list = <md.Match>[md.Match('x'), md.Match('y')];
  print(list.map((e) => e.text).join(','));
  promoteInConditional();
}

class Span {}

class TextRun extends Span {
  final String t;
  TextRun(this.t);
}

String pick(List<Span> spans) {
  final Span first = spans.first;
  final TextRun run = (spans.length == 1 && first is TextRun) ? first : TextRun('merged');
  return run.t;
}

void promoteInConditional() {
  print(pick(<Span>[TextRun('one')]));
  print(pick(<Span>[TextRun('a'), Span()]));
}
