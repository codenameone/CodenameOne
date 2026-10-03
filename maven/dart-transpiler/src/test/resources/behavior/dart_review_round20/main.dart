// A null-aware selector shorts the WHOLE rest of the chain when its receiver is
// null -- an assignment through it, an index after it, later selectors -- so
// nothing to its right is evaluated. And an assignment through a setter is an
// expression whose value is the assigned value, compound and ++/-- included.
// The library imports itself under a prefix so that section 8 can construct
// through one.
import 'main.dart' as self;

final log = <String>[];

int side(int v) {
  log.add('side $v');
  return v;
}

class Node {
  int v = 0;
  int? maybe;
  List<int> items = [0, 0];
  Node get self => this;
  int _p = 0;
  int get p => _p;
  set p(int x) {
    _p = x;
    log.add('set p $x');
  }

  int? _q;
  int? get q => _q;
  set q(int? x) {
    _q = x;
    log.add('set q $x');
  }
}

class Grid {
  final cells = <int>[0, 0, 0];
  int operator [](int i) => cells[i];
  void operator []=(int i, int v) {
    cells[i] = v;
    log.add('cell $i=$v');
  }
}

int _top = 0;
int get top => _top;
set top(int v) {
  _top = v;
  log.add('set top $v');
}

int lazyCounter = int.parse('5');

class Late {
  late int z;
}

Node shared = Node();
int makes = 0;
Node make() {
  makes++;
  return shared;
}

// A callback field reached through `a?.m()` is tested for null once, and its
// arguments run only when it is set.
class Callbacks {
  void Function()? onTap;
  int Function(int)? compute;
  Callbacks? next;
}

// A constant static has no failure to recover from; a plain collection literal
// neither. Both still initialise on first read.
class Konst {
  final int v;
  const Konst(this.v);
}

class Statics {
  static const Konst k = Konst(3);
  static const String label = 'k';
  static final cache = <String, int>{};
  static final names = <String>['a', 'b'];
}

// An untyped variable initialised by a constructor call has the constructed
// class's type, whatever form the call takes.
class Pt {
  final int x;
  Pt(this.x);
  Pt.origin() : x = 0;
  const Pt.fixed(this.x);
  int twice() => x * 2;
}

class Box<T> {
  T? value;
  Box();
  T? get v => value;
}

final topPt = Pt(1);
var topNamed = Pt.origin();
const topFixed = Pt.fixed(5);
final topBox = Box<int>();
final topNew = new Pt(2);
final topPrefixed = self.Pt(3);
const topConstPrefixed = const self.Pt.fixed(4);

class Inferred {
  static const k = Pt.fixed(7);
  static final s = Pt(8);
  static var n = new Pt.origin();
  final i = Pt(9);
  var b = Box<String>();
}

// A member of a generic class read through a subclass, a mixin application, an
// implemented interface or a typed receiver has the type its type arguments give it.
class Base<T> {
  T? v;
  T get g => v!;
  T m() => v!;
}

class Sub extends Base<String> {}

class Box2<T> {
  T? value;
  T get() => value!;
}

class A<X> {
  X? f;
  X get fg => f!;
  X fm() => f!;
}

class B<Y> extends A<Y> {}

class C extends B<int> {}

mixin Holder<H> {
  H? held;
  H take() => held!;
}

class UsesHolder with Holder<String> {}

abstract class Source<S> {
  S produce();
}

class StrSource implements Source<String> {
  String produce() => 'made';
}

void show(String what) {
  print('$what $log');
  log.clear();
}

void main() {
  Node? none;
  final Node? some = Node();

  // 1. assignment through a null-aware selector
  none?.v = side(1);
  none?.v += side(2);
  none?.maybe ??= side(3);
  none?.p = side(4);
  none?.p += side(5);
  print(none?.v = side(6));
  print(none?.p = side(7));
  show('shorted');
  some?.v = side(1);
  some?.v += side(2);
  some?.maybe ??= side(3);
  some?.maybe ??= side(30);
  some?.p = side(4);
  some?.p += side(5);
  print(some?.v = side(6));
  print(some?.p = side(7));
  print('${some!.v} ${some.maybe} ${some.p}');
  show('assigned');

  // 2. index and later selectors after a null-aware selector
  print(none?.items[side(0)]);
  print(none?.self.items[side(1)]);
  print(none?.self.v);
  none?.items[side(0)] = side(9);
  none?.self.items[side(1)] += side(9);
  show('shorted chain');
  print(some?.items[side(0)]);
  some?.items[side(0)] = side(9);
  some?.self.items[side(1)] += side(8);
  print(some?.self.items);
  show('chain');

  // 3. assignments through a setter used as values
  makes = 0;
  print((make().p += 3) > 0);
  print((make().p = 4) > 0);
  print(make().p++);
  print(++make().p);
  print(make().p--);
  print(--make().p);
  print(make().q ??= 11);
  print(make().q ??= 12);
  print('makes=$makes p=${shared.p} q=${shared.q}');
  show('setter values');
  // ... and in an operand evaluated lazily
  makes = 0;
  final f = false;
  print(f && (make().p += 3) > 0);
  print(!f && (make().p += 3) > 0);
  var n = 0;
  while ((make().p -= 1) > 0) {
    n++;
  }
  print('n=$n makes=$makes p=${shared.p}');
  show('lazy setter values');

  // 4. more assignment values: plain fields, receivers evaluated once, top-level
  // setters, operator []=, accessor-backed postfix, ++/-- behind ?.
  makes = 0;
  print(some!.v = 5);
  print(make().v += 3);
  print(make().v++);
  print('v=${shared.v} makes=$makes');
  print(top = 4);
  print(top += 2);
  top -= 1;
  print(top++);
  print(top);
  final grid = Grid();
  print(grid[1] = 7);
  print(grid[1] += 1);
  print(grid.cells);
  print(lazyCounter++);
  print(++lazyCounter);
  print(lazyCounter);
  final late = Late();
  late.z = 1;
  print(late.z++);
  print(late.z);
  print(none?.v++);
  none?.v++;
  none?.p--;
  print(some.v++);
  print(some?.v++);
  print(some?.p++);
  print('${some.v} ${some.p}');
  final void Function() cb = () => shared.p = 40;
  cb();
  print(shared.p);
  some.q = null;
  some.q ??= 2;
  some.q ??= 3;
  print(some.q);
  show('values');

  // 5. a null-aware cascade shorts all of its sections
  none?..v = side(1)..p = side(2);
  print(none?..v = side(3));
  final Node? other = Node();
  other?..v = side(4)..p = side(5);
  print('${other?.v} ${other?.p}');
  show('cascade');

  // 6. a callback field invoked through ?.call
  final cbs = Callbacks();
  cbs.onTap?.call();
  print(cbs.compute?.call(side(6)));
  cbs.onTap = () => log.add('tapped');
  cbs.compute = (x) => x * 2;
  cbs.onTap?.call();
  print(cbs.compute?.call(side(7)));
  print(cbs.next?.compute?.call(side(8)));
  cbs.next = cbs;
  print(cbs.next?.compute?.call(side(9)));
  show('callbacks');

  // 7. constant and literal statics, and a conditional arm whose ?? needs a temp
  print('${Statics.k.v} ${Statics.label}');
  Statics.cache['x'] = side(10);
  print('${Statics.cache} ${Statics.names}');
  final Node? fresh = Node();
  final pick = log.isEmpty ? 'empty' : fresh?.maybe ?? side(11);
  fresh?.maybe = 12;
  final pick2 = log.isEmpty ? 'empty' : fresh?.maybe ?? side(13);
  print('$pick $pick2');
  show('statics');

  // 8. untyped variables initialised by constructor calls
  print('${topPt.x} ${topNamed.x} ${topFixed.twice()} ${topNew.x} ${topPrefixed.x} ${topConstPrefixed.x}');
  topBox.value = 6;
  print(topBox.v! + 1);
  print('${Inferred.k.x} ${Inferred.s.twice()} ${Inferred.n.x}');
  final inf = Inferred();
  inf.b.value = 'box';
  print('${inf.i.x} ${inf.b.v!.length}');
  topNamed = Pt(10);
  print(topNamed.x);

  // 9. generic members through subclasses, mixins, interfaces and receivers
  final sub = Sub();
  sub.v = 'abc';
  print('${sub.m().length} ${sub.g.length} ${sub.v!.length}');
  final box = Box2<String>();
  box.value = 'four';
  print(box.get().length);
  final b = B<int>();
  b.f = 41;
  print('${b.f! + 1} ${b.fg + 2} ${b.fm() + 3}');
  final c = C();
  c.f = 10;
  print('${c.f! + 1} ${c.fg + 2} ${c.fm() + 3}');
  final u = UsesHolder();
  u.held = 'held';
  print('${u.take().length} ${u.held!.length}');
  Source<String> src = StrSource();
  print(src.produce().length);
}
