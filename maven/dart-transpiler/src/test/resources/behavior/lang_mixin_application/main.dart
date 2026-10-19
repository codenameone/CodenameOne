class Base {
  final String name;
  final int size;
  Base(this.name, {this.size = 1});
  Base.small(String n) : this(n, size: 0);
  String describe() => '$name/$size';
}

mixin Loud on Base {
  String shout() => describe().toUpperCase();
}

mixin Counted {
  int count = 0;
  void tick() => count++;
}

class LoudBase = Base with Loud, Counted;

void main() {
  final a = LoudBase('bell', size: 3);
  a.tick();
  a.tick();
  print(a.shout());
  print(a.count);
  final b = LoudBase.small('mouse');
  print(b.describe());
  print(b is Base);
}
