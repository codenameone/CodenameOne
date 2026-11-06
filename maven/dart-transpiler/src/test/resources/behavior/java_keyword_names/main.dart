// Dart identifiers that are Java reserved words, as fields, getters, enum values
// and dynamic reads.
enum Mode { native, public, plain }

abstract class Marker {
  int get char;
  bool get native;
}

class Dot implements Marker {
  @override
  final int char;
  @override
  bool get native => char > 40;
  Dot(this.char);
}

class Lab {
  static int public = 3;
  static String get mode => 'm${public}';
  static int long(int int) => int * 2;
}

void main() {
  final d = Dot(42);
  print(d.char);
  print(d.native);
  Marker m = Dot(7);
  print('${m.char} ${m.native}');
  print(Mode.native);
  print(Mode.values.map((v) => v.name).toList());
  print(Lab.public + 1);
  print(Mode.values.first == Mode.native);
  dynamic x = d;
  print(x.char);
  print(Lab.mode);
  print(Lab.long(5));
  final native = Mode.public;
  print('$native ${native.name}');
}
