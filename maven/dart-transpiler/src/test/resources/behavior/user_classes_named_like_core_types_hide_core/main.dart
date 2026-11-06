import 'dart:core' hide Object, String, Type, List, Map, Enum;
import 'lib.dart';
class Object { final int v; Object(this.v); toString() => 'MyObject($v)'; }
class String { s() => 'MyString'; }
class Type { toString() => 'MyType'; }
class List { int call(int x) => x * 2; }
class Map { toString() => 'MyMap'; }
class Enum { get name => 'MyEnum'; }
void main() {
  print(Object(3));
  print(String().s());
  print(Type());
  print(List()(21)); print(Map()); print(Enum().name);
  print([1, 2].length); print({'a': 1});
  print(Object(1).runtimeType); print(String().runtimeType);
  print(Thread<int>(5).get());
  print(Thread<int>(5).runtimeType);
  print(Dyn().toString()); print(Dyn().runtimeType);
  print(Funcs.make());
  print(DartList().length);
  print(DartList().runtimeType);
  print(Runnable.x(4));
  print(4.twice());
  var o = Thread<bool>(true) as dynamic;
  print(o is Thread<bool>); print(o is Thread<int>);
  dynamic d = DartList(); print(d.length);
  print(mk() is Object);
}
Object mk() => Object(9);
