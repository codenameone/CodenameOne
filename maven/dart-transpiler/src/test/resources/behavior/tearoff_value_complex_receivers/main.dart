typedef Handler = void Function(String);

class Counter {
  int n = 0;
  final String id;
  Counter(this.id);
  void bump(String why) => n++;
  int twice(int v) => v * 2;
  int scaled(int v, {int scale = 1}) => v * scale;
}

int made = 0;
final Counter shared = Counter('shared');
Counter pick() {
  made++;
  return shared;
}

Counter? maybe;
Counter fallback = Counter('fallback');

class Bus {
  final List<Handler> handlers = [];
  void add(Handler h) => handlers.add(h);
  bool remove(Handler h) => handlers.remove(h);
}

class Base {
  String who() => 'base';
}

mixin Loud on Base {
  @override
  String who() => 'loud';
}

mixin Quiet {
  String hush() => 'quiet';
}

class Shout extends Base with Loud, Quiet {
  @override
  String who() => 'shout';
  @override
  String hush() => 'SHH';
  Object superWho() => super.who;
  Object superHush() => super.hush;
}

class Plain extends Base with Loud {
  Object superWho() => super.who;
}

void main() {
  // A call result as the receiver: evaluated once, equal by receiver and method.
  Handler h1 = pick().bump;
  Handler h2 = pick().bump;
  print('made $made');
  print('call == ${h1 == h2} ${h1.hashCode == h2.hashCode}');
  final bus = Bus();
  bus.add(pick().bump);
  print('removed ${bus.remove(pick().bump)} left ${bus.handlers.length}');
  print('made $made');

  // An if-null receiver.
  Handler n1 = (maybe ?? fallback).bump;
  Handler n2 = (maybe ?? fallback).bump;
  print('ifnull == ${n1 == n2}');
  n1('x');
  print('fallback ${fallback.n}');
  maybe = Counter('m');
  Handler n3 = (maybe ?? fallback).bump;
  print('changed == ${n1 == n3}');

  // Into Object, a Set and a typedef with a named parameter.
  final Object o1 = pick().twice;
  final Object o2 = pick().twice;
  print('object == ${o1 == o2}');
  final list = <Object>[pick().twice];
  print('list remove ${list.remove(pick().twice)} ${list.length}');
  print('made $made');

  // A super tear-off keyed by the class that declares the method.
  final s = Shout();
  print('super hush ${(s.superHush() as String Function())()}');
  print('vs override ${s.superWho() == s.who}');
  final p = Plain();
  print('plain ${p.superWho() == p.who}');
}
