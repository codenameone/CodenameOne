import 'ext_lib.dart';
import 'ext_lib.dart' as p;
import 'shape_lib.dart' as q;

extension Local on int {
  static int twice(int n) => n * 2;
  static T pick<T>(T a, T b) => b;
}

class C {
  static int make(int n) => n + 1;
}

void report(String label, Object a, Object b) {
  print('$label ${a == b} ${identical(a, b)} ${a.hashCode == b.hashCode} ${{a, b}.length}');
}

void main() {
  report('E.make', E.make, E.make);
  report('Id.make', Id.make, Id.make);
  report('Local.twice', Local.twice, Local.twice);
  report('C.make', C.make, C.make);
  report('p.E.make', p.E.make, E.make);
  report('p.Id.make', p.Id.make, Id.make);
  report('E vs Local', E.make, Local.twice);
  String Function(int) f = E.make;
  print(f(3));
  print({E.make, E.make, Local.twice}.length);
  print([E.make].contains(E.make));

  int Function(int) gi = Id.gen;
  int Function(int) gi2 = Id.gen;
  report('ctx Id.gen', gi, gi2);
  print(gi(7));
  report('E.gen<int>', E.gen<int>, E.gen<int>);
  report('p.E.gen<int>', p.E.gen<int>, E.gen<int>);
  report('E.gen<int> vs <String>', E.gen<int>, E.gen<String>);
  print(E.gen<int>(4));
  print(p.E.gen<String>('z'));
  int Function(int, int) pk = Local.pick;
  print(pk(1, 2));
  report('Local.pick<int>', Local.pick<int>, pk);

  print(p.Id.make(5).v);
  {
    var E = _Fake();
    print(E.make);
  }

  p.Area a2 = Area(q.Shape(2, 5));
  print(a2.size);
  List<p.Area> ws = [a2];
  print(ws.first.size);
}

class _Fake {
  String get make => 'local shadows';
}
