import 'dart:typed_data';

// A named constructor is not a member of the instance, so an extension
// method with the same name is what `x.view(...)` calls.
extension BytesView on Uint8List {
  Uint8List view(int offset, int bytes) =>
      Uint8List.view(buffer, offsetInBytes + offset, bytes);
}

class Box {
  final int v;
  Box(this.v);
  Box.twice(int v) : v = v * 2;
}

extension BoxTwice on Box {
  int twice(int by) => v * by;
}

void main() {
  final data = Uint8List.fromList([1, 2, 3, 4, 5, 6, 7, 8]);
  final sub = data.view(2, 3);
  print(sub);
  final inner = sub.view(1, 2);
  print(inner);
  inner[0] = 99;
  print(data);
  print(Box.twice(4).v);
  print(Box(5).twice(3));
}
