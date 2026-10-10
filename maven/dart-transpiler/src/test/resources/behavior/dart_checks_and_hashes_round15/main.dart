// List.from and Uint8List.fromList check element types; int.parse trims Dart's
// whitespace; Object.hash/hashAll agree with == across int and double; a caught
// concurrent modification is Dart's error; ByteData offsets near the long limit
// are range errors.
import 'dart:typed_data';

class Money {
  final num amount;
  Money(this.amount);

  @override
  bool operator ==(Object other) => other is Money && other.amount == amount;

  @override
  int get hashCode => Object.hashAll([amount]);
}

void main() {
  final dynamic mixed = <dynamic>[1];
  try {
    print(List<String>.from(mixed));
  } on TypeError {
    print('List<String>.from refused an int');
  }
  print(List<String>.from(<dynamic>['a', 'b']));
  final dynamic notInts = <dynamic>['x'];
  try {
    print(Uint8List.fromList(notInts));
  } on TypeError {
    print('Uint8List.fromList refused a string');
  }
  print(int.parse('\u00A0123\u00A0'));
  print(int.tryParse('\u2003 42 \u3000'));
  print(Object.hashAll([1]) == Object.hashAll([1.0]));
  print(Object.hash(2, 'x') == Object.hash(2.0, 'x'));
  print({Money(1)}.contains(Money(1.0)));
  final Map<String, int> m = {'a': 1};
  try {
    m.forEach((k, v) {
      m['b$k'] = v;
    });
  } on ConcurrentModificationError catch (e) {
    print('caught as the Dart error: ${e is ConcurrentModificationError}');
  }
  final bd = ByteData(8);
  try {
    bd.getInt32(9223372036854775807);
  } on RangeError {
    print('offset near the limit is a RangeError');
  }
}
