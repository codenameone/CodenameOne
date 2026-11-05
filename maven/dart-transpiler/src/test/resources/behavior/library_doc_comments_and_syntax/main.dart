/// Library doc comment before an unnamed library directive.
@deprecated
library;

import 'dart:core';

class Oops {
  final String msg;
  Oops(this.msg);
  String toString() => 'Oops($msg)';
}

void fail(String m) => throw Oops(m);

void plain() {
  try {
    fail('plain');
  } catch (e) {
    print('caught $e');
    rethrow;
  }
}

void typed() {
  try {
    fail('typed');
  } on Oops catch (e) {
    print('typed caught ${e.msg}');
    rethrow;
  }
}

void noVar() {
  try {
    fail('novar');
  } on Oops {
    print('no variable');
    rethrow;
  }
}

String join(int a, {int b = 0, int c = 0}) => '$a/$b/$c';

int counter = 0;
int next(String tag) {
  counter++;
  print('eval $tag $counter');
  return counter;
}

class Box {
  int w = 0;
  String c = '';
}

class Holder {
  final Box box;
  final List<int> xs;
  final StringBuffer sb;
  Holder(String c) : box = Box()..c = c..w = 2, xs = <int>[]..add(1)..add(2), sb = StringBuffer('>')..write('a')..write('b');
}

class Point {
  final int x;
  Point(this.x);
  @override
  bool operator ==(Object other) => other is Point && super == other || (other is Point && other.x == x);
  @override
  int get hashCode => x;
}

class Same {
  @override
  bool operator ==(Object other) => super == other;
  @override
  int get hashCode => 1;
}

String sw(int v) {
  switch (v) {
    case 1:
    case 2:
      var x = 'low $v';
      return x;
    case 3:
      var x = 'three';
      return x;
    default:
      var x = 'other';
      return x;
  }
}

class Sq {
  final int side;
  Sq(this.side);
}

class Rect {
  final int w;
  final bool tall;
  Rect(this.w, this.tall);
}

String shape(Object o) {
  switch (o) {
    case int n when n > 10:
    case Sq(side: var n):
      return 'sized $n';
    case Rect(tall: var d):
    case bool d:
      return 'flag $d';
    default:
      return 'none';
  }
}

void main() {
  try {
    plain();
  } on Oops catch (e) {
    print('outer $e');
  }
  try {
    typed();
  } on Oops catch (e, _) {
    print('outer $e');
  }
  try {
    noVar();
  } catch (e) {
    print('outer $e');
  }
  print(join(b: 2, 1));
  print(join(c: next('c'), next('a'), b: next('b')));
  final h = Holder('red');
  print('${h.xs} ${h.sb} ${h.box.c} ${h.box.w}');
  print(Point(1) == Point(1));
  final s = Same();
  print(s == s);
  print(s == Same());
  print(#foo);
  print(#foo == #foo);
  print(#foo == #bar);
  final syms = {#a, #a, #b};
  print(syms.length);
  for (var i = 1; i <= 4; i++) {
    print(sw(i));
  }
  for (final o in <Object>[42, Sq(4), Rect(1, true), 3, false, 'x']) {
    print(shape(o));
  }
}
