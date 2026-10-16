extension StringX on String {
  String shout() {
    return this.toUpperCase() + '!';
  }

  String get first => this[0];

  String repeatTwice() => this + this;
}

extension IntX on int {
  int squared() => this * this;
}

mixin Greeter {
  String greeting = 'Hello';
  String greet(String who) {
    return '$greeting, $who';
  }
}

mixin Counter {
  int count = 0;
  void bump() {
    count++;
  }
}

class Host with Greeter, Counter {
}

class Base2 {
  String id() => 'base';
}

class Sub2 extends Base2 with Greeter {
}

void main() {
  print('dart'.shout());
  print('dart'.first);
  print('ab'.repeatTwice());
  print(5.squared());
  Host h = Host();
  print(h.greet('World'));
  h.bump();
  h.bump();
  print(h.count);
  h.greeting = 'Hi';
  print(h.greet('Again'));
  Sub2 s = Sub2();
  print(s.id());
  print(s.greet('Mix'));
}
