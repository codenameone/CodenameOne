typedef Labeler = String? Function(String name, [String? suffix]);
typedef Builder = String? Function(int day, int month);

class Builders<T> {
  final Builder? primary;
  final T? tag;
  final List<String> names;
  Builders({this.primary, this.tag, this.names = const ['a', 'b']});
  String Function(int) get shout => (int n) => 'n=$n';
}

class Cell {
  final Builders builders;
  final dynamic locale;
  final Labeler labeler;
  Cell(this.builders, this.locale, {Labeler? labeler})
      : labeler = labeler ?? ((String _, [String? __]) => null);

  String describe(int d) {
    final built = builders.primary?.call(d, 7);
    final first = builders.names.first;
    final shout = builders.shout;
    final loud = shout(d);
    final label = labeler('x');
    return '$built $first $loud $label ${greet(locale)}';
  }
}

String greet(String who) => 'hi $who';

void main() {
  final c = Cell(Builders<int>(primary: (d, m) => '$d/$m', tag: 3), 'en');
  print(c.describe(4));
  final c2 = Cell(Builders(), 'fr', labeler: (n, [s]) => '$n${s ?? '!'}');
  print(c2.describe(5));
  print(c2.builders.tag);
}
