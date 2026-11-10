int h1(int x) => x + 1;
num h2(num x) => x * 2;

Function pick(bool c) => c ? h1 : h2;

int Function(int) choose(bool c) {
  if (c) return h1;
  return (int x) => x - 1;
}

num Function(int) sel(int k) => switch (k) {
      0 => h1,
      1 => h2,
      _ => (int x) => x * 10,
    };

void main() {
  bool c = DateTime.now().year > 2000;
  var f = c ? h1 : h2;
  print(f(3));
  var g = !c ? h1 : h2;
  print(g(3));
  print(pick(true)(4));
  print(pick(false)(4));
  int Function(int)? maybe = c ? null : h1;
  var h = maybe ?? h2;
  print(h(5));
  print(choose(false)(5));
  print(sel(0)(2));
  print(sel(1)(2));
  print(sel(2)(2));

  var named = [({int a = 0}) => a];
  print(named.runtimeType);
  print(named[0](a: 7));
  var n2 = ({int a = 1, int b = 2}) => a + b;
  print(n2.runtimeType);

  var both = [({int a = 0}) => a, ({int a = 0, int b = 0}) => a + b];
  print(both.runtimeType);
  for (final fn in both) {
    print(fn(a: 3));
  }
  var cc = c ? ({int a = 0}) => a * 2 : ({int a = 0, int b = 5}) => a + b;
  print(cc(a: 4));
  more();
}

void more() {
  bool c = DateTime.now().year > 2000;
  var s = switch (c) { true => h1, false => h2 };
  print(s(6));
  var k = (bool q) {
    if (q) return h1;
    return h2;
  };
  print(k(true)(1));
  print(k(false)(1));
  int Function({int a, int b}) wide = ({int a = 1, int b = 2}) => a * 10 + b;
  int Function({int a}) narrow = c ? wide : ({int a = 0}) => a;
  print(narrow(a: 3));
  print(narrow is int Function({int a, int b}));
  print((narrow as dynamic)(a: 4, b: 9));
  print([wide, ({int a = 0}) => -a].map((f) => f(a: 2)).toList());
}
