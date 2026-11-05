abstract class Shape { String n(); }
mixin Tag { String tag() => 'tag'; }
class Sq extends Shape with Tag { String n() => 'sq'; }
class Ci extends Shape with Tag { String n() => 'ci'; }
class Box<T> { final T a; final T b; Box(this.a, this.b); }
class Named<T> { final T x; final T y; Named({required this.x, required this.y}); }
T pick<T>(T a, T b) => a;
List<T> both<T>(T a, T b) => [a, b];
void main() {
  var r = (1, (2, 'x'));
  print(r.runtimeType);
  var n = (a: 1, b: (c: 2.5, d: null));
  print(n.runtimeType);
  List<(int, String)> l = [(1, 'a')];
  print(l.runtimeType);
  print([(1, 'a'), (2, 'b')].runtimeType);
  print({(1, 2): 'v'}.runtimeType);
  print((1, 2).runtimeType == (3, 4).runtimeType);
  print((1, 2).runtimeType == (3, 4.0).runtimeType);
  print((1, 2).runtimeType.hashCode == (5, 6).runtimeType.hashCode);
  print((x: 1, y: 2).runtimeType == (y: 1, x: 2).runtimeType);
  Object o = (1, 2);
  print(o is (num, num));
  print(o is (int, String));
  print(o is (int, int)?);
  print(((1, 2) as Object) == (1, 2));
  print({(1, 2), (1, 2)}.length);
  print(both(1, 2.0).runtimeType);
  print(both(1, 'a').runtimeType);
  print(both(null, 1).runtimeType);
  var c = DateTime.now().year > 0 ? Sq() : Ci();
  print([c].runtimeType);
  var c2 = DateTime.now().year > 0 ? 1 : 2.0;
  print([c2].runtimeType);
  var c3 = DateTime.now().year > 0 ? null : 'a';
  print([c3].runtimeType);
  print(Box(Sq(), Ci()).runtimeType);
  print(Box(1, 2.5).runtimeType);
  print(Box(null, 3).runtimeType);
  print(Named(x: 1, y: 2).runtimeType);
  print(Named(x: Sq(), y: Ci()).runtimeType);
  print({1, null}.runtimeType);
  print([1, 'a'].runtimeType);
  var bsc = both(Sq(), Ci());
  print(bsc.runtimeType);
  print(bsc.map((s) => (s as Shape).n()).join(','));
  var nums = both(1, 2.5);
  num total = nums[0] + nums[1];
  print(total);
  print({1, 2.0}.runtimeType);
  print({Sq(), Ci()}.runtimeType);
  print({(1, 'a')}.runtimeType);
  Object o2 = (1, 'a');
  print(o2 is Record);
  print(1 is Record);
  print(o2 is (int, String));
  print(o2 is (String, int));
  print(o2 is (int, String, int));
  Object o3 = (x: 1, y: 'b');
  print(o3 is ({int x, String y}));
  print(o3 is ({int x, int y}));
  print(o3 is ({int x, String z}));
  print(o3 is (int, String));
  Object lr = [(1, 'a')];
  print(lr is List<(int, String)>);
  print(lr is List<(int, int)>);
  print(lr is List<Record>);
  var c4 = DateTime.now().year > 0 ? 1 : 2.0;
  num c5 = c4;
  print(c5);
  var k = DateTime.now().year > 0 ? Sq() : Ci();
  print(k is Shape);
  print(k.runtimeType);
}
