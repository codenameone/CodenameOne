import 'ext_lib.dart';
import 'ext_lib.dart' as p;
import 'shape_lib.dart' as q;

extension type Wrap(Area a) {
  int get twice => a.size * 2;
}

extension type Wrap2(p.Area a) {}

extension G on int {
  static List<T> gen<T>(T x) => [x, x];
  static Map<String, T> keyed<T>(T x) => {'k': x};
}

class C2 {
  static List<T> gen<T>(T x) => [x, x];
}

void main() {
  List<int> lc = C2.gen<int>(5);
  print(lc);
  Wrap w = Wrap(Area(q.Shape(2, 3)));
  print(w.twice);
  var w2 = Wrap(Area(q.Shape(1, 4)));
  print(w2.twice);
  print(w2.a.size);
  var w3 = Wrap2(p.Area(q.Shape(3, 3)));
  print(w3.a.size);
  List<Wrap> lw = [w, w2];
  print(lw.length);
  List<int> li = G.gen<int>(3);
  li.add(4);
  print(li);
  var ls = G.gen('a');
  print(ls.runtimeType);
  List<String> Function(String) tf = G.gen<String>;
  print(tf('b'));
  var m = G.keyed<int>(1);
  print(m);
  List<q.Shape> shapes = [q.Shape(1, 1)];
  List<Area> areas = [Area(shapes.first)];
  Map<String, Area> am = {'x': areas.first};
  print(am['x']!.size);
  var ids = <Id>[Id(1)];
  print(ids.first.v);
  var sh = Shapes([q.Shape(2, 2), q.Shape(1, 3)]);
  print(sh.total);
  print(sh.items.length);
  Shapes sh2 = Shapes(<q.Shape>[q.Shape(5, 1)]);
  print(sh2.total);
  var nm = Named({'a': q.Shape(7, 1)});
  print(nm.widthOf('a'));
  List<Shapes> many = [sh, sh2];
  print(many.map((x) => x.total).toList());
  print(identical(G.gen<int>, G.gen<int>) || G.gen<int> == G.gen<int>);
  List<int> Function(int) ti = G.gen<int>;
  print(ti(6).runtimeType);
}
