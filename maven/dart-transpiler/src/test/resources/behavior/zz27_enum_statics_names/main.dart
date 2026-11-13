enum Color { red, green, blue }

enum Level {
  low, mid, high;
  static const all = [low, mid, high];
  static final first = all[0];
  static final count = all.length;
  static Level pick(int i) => all[i];
}

class Stack {
  final int v;
  Stack(this.v);
}

void main() {
  print(Level.all);
  Level f = Level.first;
  print(f.name);
  print(Level.count + 1);
  List<Level> ls = Level.all;
  print(ls.last);
  print(Level.pick(1));
  Color c = Color.green;
  print(c);
  print(Color.values.length);
  print(identical(const (1, 'a'), const (1, 'a')));
  print(identical(const (1, 'a'), const (2, 'a')));
  print(identical(const (x: 1, y: 2), const (x: 1, y: 2)));
  print((1, 'a') == (1, 'a'));
  print(Stack);
  print(Level);
  print(Color);
  print(Stack(1).runtimeType);
}
