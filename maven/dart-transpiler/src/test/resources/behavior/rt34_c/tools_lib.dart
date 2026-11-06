import 'inner_lib.dart';
import 'shape_lib.dart' as base;

class Shape {
  final base.Shape inner;
  Shape(this.inner);
}

class Tools {
  static Inner mk(String s) => Inner('t$s');
  static String viaBase() {
    final m = <Object, int>{};
    m[base.Shape.make] = 1;
    Object o = base.Shape.make;
    return 'base ${m[o]} ${(base.Shape.make as Object) == o} ${(o as Function)("w")}';
  }
}
