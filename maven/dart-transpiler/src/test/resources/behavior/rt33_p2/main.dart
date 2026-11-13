abstract class Base {
  String get name;
  set name(String v);
  String hello() => 'Base';
  int v = 0;
}
mixin M1 on Base {
  @override String hello() => 'M1>' + super.hello();
  String get tag;
  String get name => 'M1' + tag;
  @override set name(String x) { v += 1; }
}
mixin M2 on Base {
  @override String hello() => 'M2>' + super.hello();
  @override String get name => 'M2(' + super.name + ')';
  @override set name(String x) { v += 10; super.name = x; }
}
mixin Plain {
  int cnt = 0;
  void bump() { cnt++; }
  String who();
  String greet() => 'hi ' + who();
}
class A extends Base with M1, M2, Plain {
  String get tag => 'A';
  String who() => 'A';
  @override String hello() => 'A>' + super.hello();
  @override void bump() { super.bump(); super.bump(); }
}
class B extends A {
  @override String hello() => 'B>' + super.hello();
  @override String get name => 'B[' + super.name + ']';
}
mixin Fn on Base { String f() => super.hello(); }
class D extends Base with M1, Fn { String get tag => 'D'; }
void main() {
  var a = A();
  print(a.hello());
  print(a.name);
  a.name = 'x';
  print(a.v);
  a.bump();
  print('${a.cnt} ${a.greet()}');
  var b = B();
  print(b.hello());
  print(b.name);
  b.name = 'y';
  print(b.v);
  var d = D();
  print(d.f());
  var h = a.hello;
  print(h());
  var hs = <String Function()>[a.hello, b.hello];
  print(hs.map((f) => f()).join('|'));
}
