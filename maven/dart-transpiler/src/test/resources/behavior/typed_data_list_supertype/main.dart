import 'dart:typed_data';

// TypedDataList: the supertype every typed list shares.
String describe(TypedDataList<int> l) =>
    '${l.length} ${l.elementSizeInBytes} ${l.lengthInBytes} ${l.offsetInBytes}';

int total(List<int> l) => l.fold(0, (a, b) => a + b);

void main() {
  final bytes = Uint8List(4);
  final words = Int32List.fromList([1, 2, 3]);
  print(describe(bytes));
  print(describe(words));
  TypedDataList<int> t = words;
  t.setRange(0, 2, [7, 8]);
  t.fillRange(2, 3, 9);
  print(words);
  print(total(t));
  final TypedDataList<double> f = Float64List(2);
  f.fillRange(0, 2, 1.5);
  print('${f.length} ${f.elementSizeInBytes} ${f.buffer.lengthInBytes}');
  print(t is List<int>);
}
