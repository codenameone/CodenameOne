int counter = 0;
int next(String tag) { counter++; print('init $tag $counter'); return counter; }

class Base {
  int baseField = next('base');
  Base() { print('Base ctor sees ${describe()}'); }
  String describe() => 'Base';
}

class Plain extends Base {
  int x = 5;
  final l = [1];
  int tracked = next('plain');
  Plain() : super() { print('Plain body x=$x'); }
  @override
  String describe() => 'x=$x l=$l tracked=$tracked';
}

class WithInitList extends Base {
  final int a;
  int b = next('b');
  WithInitList(int v) : a = next('a') + v { print('WithInitList body a=$a b=$b'); }
  @override
  String describe() => 'a=$a b=$b';
}

class A {
  A() { print('A ctor sees ${describe()}'); }
  String describe() => 'A';
}
class B<T> extends A {
  final List<T> items = <T>[];
  int x = next('x');
  late int y = next('y');
  final T seed;
  B(this.seed) : super() { print('B body x=$x'); }
  @override
  String describe() => 'B items=${items == null ? "null" : items.length} x=$x seed=$seed';
}
class C extends B<String> {
  int z = next('z');
  C() : super('s');
}
void mainGeneric() {
  var b = B<int>(7);
  print(b.y); print(b.y);
  var c = C();
  print(c.z);
}

void main() {
  var p = Plain();
  print(p.describe());
  var w = WithInitList(100);
  print(w.describe());
  mainGeneric();
}
