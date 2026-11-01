typedef Labeler = String? Function(String name, [String? suffix]);
typedef Builder = String? Function(int day, int month);

class Builders<T> {
  final Builder? primary;
  final T? tag;
  final List<String> names;
  Builders({this.primary, this.tag, this.names = const ['a', 'b']});
  String Function(int) get shout => (int n) => 'n=$n';
}

class LoudBuilders extends Builders<String> {
  LoudBuilders() : super(tag: 'loud');
}

class Cell {
  final Builders builders;
  final dynamic locale;
  final Labeler labeler;
  Cell(this.builders, this.locale, {Labeler? labeler})
      : labeler = labeler ?? ((String _, [String? __]) => null);

  int Function(int) get twin => (int n) => n * 2;

  String describe(int d) {
    final built = builders.primary?.call(d, 7);
    final first = builders.names.first;
    final loud = builders.shout(d);
    final twice = twin(d);
    final label = labeler('x');
    return '$built $first $loud $twice $label ${greet(locale)}';
  }
}

String greet(String who) => 'hi $who';

void main() {
  final c = Cell(Builders<int>(primary: (d, m) => '$d/$m', tag: 3), 'en');
  print(c.describe(4));
  final c2 = Cell(Builders(), 'fr', labeler: (n, [s]) => '$n${s ?? '!'}');
  print(c2.describe(5));
  print(c2.builders.tag);
  print(LoudBuilders().shout(9));
}
