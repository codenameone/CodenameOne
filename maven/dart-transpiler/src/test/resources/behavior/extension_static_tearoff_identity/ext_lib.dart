import 'shape_lib.dart' as s;

extension type Id(int v) {
  static Id make(int n) => Id(n * 2);
  static T gen<T>(T x) => x;
}

extension E on String {
  static String make(int n) => 'e$n';
  static T gen<T>(T x) => x;
}

// erases to a prefixed type of another library
extension type Area(s.Shape shape) {
  int get size => shape.w * shape.h;
}
