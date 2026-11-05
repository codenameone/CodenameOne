// Program declarations named like Flutter types, in a library that imports Flutter. Dart's
// scoping: a declaration of the library shadows every import of that name, and `hide`
// removes the import's. Expected output is hand-written (the plain Dart SDK cannot run a
// Flutter import); every line is what the declarations below compute.
import 'package:flutter/material.dart' hide Row, Image;

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

class Icon {
  final String glyph;
  Icon(this.glyph);
}

class Image {
  final int width, height;
  Image(this.width, this.height);
  int get area => width * height;
}

Row makeRow(int n) => [for (var i = 0; i < n; i++) i + n];

class Label extends StatelessWidget {
  const Label({super.key});
  @override
  Widget build(BuildContext context) => const Text('still Flutter');
}

void main() {
  const c = Card('clubs', 3);
  print(c.label);
  print(Color.red.name);
  Color col = Color.green;
  print(col.index);
  print(Wrap(4).twice().plusOne);
  final box = Container();
  box.cards.add(c);
  box.bump();
  print('${box.size} ${box.counter}');
  Key k = 'beta';
  print(k.toUpperCase());
  print(makeRow(3));
  print(Icon('#').glyph);
  print(Image(5, 6).area);
  print(const Label() is StatelessWidget);
}
