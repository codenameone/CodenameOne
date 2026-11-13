class Base {
  String who() => 'base';
  String get label => 'base-label';
  int stored = 0;
  set level(int v) => stored = v;
  int get level => stored;
  String step(int n) => 'B$n';
}

mixin Loud on Base {
  @override
  String who() => 'loud';
  @override
  String get label => 'loud-label';
  @override
  set level(int v) => stored = v * 10;
  @override
  String step(int n) => 'L>' + super.step(n);
}

mixin Quiet on Base {
  @override
  String step(int n) => 'Q>' + super.step(n);
}

class P extends Base with Loud {
  @override
  String who() => 'p/' + super.who();
  @override
  String get label => 'p/' + super.label;
  @override
  set level(int v) => super.level = v + 1;
  String Function() tear() => super.who;
}

class Q extends Base with Loud, Quiet {
  @override
  String step(int n) => 'P>' + super.step(n);
  @override
  String who() => 'q/' + super.who();
}

void main() {
  final p = P();
  print(p.who());
  print(p.label);
  p.level = 2;
  print(p.level);
  print(p.tear()());
  final q = Q();
  print(q.step(3));
  print(q.who());
}
