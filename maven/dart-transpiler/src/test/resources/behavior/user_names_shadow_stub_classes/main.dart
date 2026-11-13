// No Flutter import: every name below is the program's own, never a widget or runtime type.
class Card {
  final String suit;
  final int rank;
  const Card(this.suit, this.rank);
  String get label => '$rank of $suit';
}

enum Color { red, green, blue }

extension type Wrap(int value) {
  Wrap twice() => Wrap(value * 2);
  int get plusOne => value + 1;
}

mixin State {
  int counter = 0;
  void bump() => counter++;
}

class Container with State {
  final List<Card> cards = [];
  int get size => cards.length;
}

typedef Key = String;
typedef Row = List<int>;

class Text {
  final String data;
  Text(this.data);
  @override
  String toString() => 'Text<$data>';
}

abstract class Icon {
  String get glyph;
}

class Star extends Icon {
  @override
  String get glyph => '*';
}

class Image {
  final int width, height;
  Image(this.width, this.height);
  int get area => width * height;
}

Row makeRow(int n) => [for (var i = 0; i < n; i++) i * i];

void main() {
  const c = Card('hearts', 7);
  print(c.label);
  print(Color.green.name);
  print(Color.values.length);
  Color col = Color.blue;
  print(col.index);
  final w = Wrap(5);
  print(w.twice().plusOne);
  final box = Container();
  box.cards.add(c);
  box.bump();
  box.bump();
  print('${box.size} ${box.counter}');
  Key k = 'alpha';
  print(k.toUpperCase());
  final r = makeRow(4);
  print(r);
  print(Text('hi'));
  Icon i = Star();
  print(i.glyph);
  print(Image(3, 4).area);
  final cards = <Card>[Card('spades', 1), c];
  print(cards.map((x) => x.rank).toList());
  print(<Color, int>{Color.red: 1}[Color.red]);
}
