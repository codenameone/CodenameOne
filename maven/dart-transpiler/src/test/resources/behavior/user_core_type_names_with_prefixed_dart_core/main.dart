import 'dart:core' hide Object, String, Type, List, Map, Enum;
import 'dart:core' as core;
import 'lib.dart';
class Object { final core.int v; Object(this.v); core.String toString() => 'MyObject($v)'; }
class String { core.String s() => 'MyString'; }
class Type { core.String toString() => 'MyType'; }
class List { core.int call(core.int x) => x * 2; }
class Map { core.String toString() => "MyMap"; }
class Enum { core.String get name => "MyEnum"; }
void main() {
  core.print(Object(3));
  core.print(String().s());
  core.print(Type());
  core.print(List()(21)); core.print(Map()); core.print(Enum().name); core.print([1,2] is core.List<core.int>); core.print(<core.String, core.int>{"a": 1});
  core.print(Object(1).runtimeType);
  core.print(Thread<core.int>(5).get());
  core.print(Thread<core.int>(5).runtimeType);
  core.print(Dyn().toString());
  core.print(Funcs.make());
  core.print(DartList().length);
  core.print(DartList().runtimeType);
  core.print(Runnable.x(4));
  core.print(4.twice());
  core.Object o = Thread<core.String>('q');
  core.print(o is Thread<core.String>);
  core.print(o is Thread<core.int>);
}
