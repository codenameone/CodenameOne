// An abstract class that IMPLEMENTS a concrete class reads the implemented class's
// fields through their getters, and `??` of unrelated types is an Object.

class Spot {
  final Uri? url;
  final int line;
  final int column;
  Spot(this.line, this.column, {this.url});
  String describe() => 'spot $line';
}

abstract class SpotMixin implements Spot {
  String get tool {
    final source = url ?? 'unknown source';
    return '$source:${line + 1}:${column + 1}';
  }

  int distance(Spot other) {
    if (url != other.url) {
      throw ArgumentError('mismatch');
    }
    return (line - other.line).abs();
  }

  @override
  String describe() => 'mixin spot $line:$column';
}

class FileSpot extends SpotMixin {
  @override
  final Uri? url;
  @override
  final int line;
  @override
  final int column;
  FileSpot(this.line, this.column, [this.url]);
}

void main() {
  final a = FileSpot(2, 4);
  final b = FileSpot(7, 1);
  print(a.tool);
  print(FileSpot(0, 0, Uri.parse('file:///x.dart')).tool);
  print(a.distance(b));
  print(b.describe());
  print(Spot(3, 3).describe());
  try {
    a.distance(FileSpot(1, 1, Uri.parse('a:b')));
  } on ArgumentError catch (e) {
    print('error ${e.message}');
  }
}
