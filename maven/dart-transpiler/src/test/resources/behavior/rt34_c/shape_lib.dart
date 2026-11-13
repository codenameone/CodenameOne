class Shape {
  final String tag;
  Shape(this.tag);
  static Shape make(String t) => Shape('lib:$t');
  static int twice(int x) => x * 2;
  static T keep<T>(T v) => v;
  String toString() => 'Shape($tag)';
}
