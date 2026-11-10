import 'dart:convert';
import 'dart:typed_data';

// `is List<X>` on a list of other elements must answer false, and the read the
// test promotes must never see an element of the wrong type.
String describe(List value) {
  if (value is Uint8List) {
    return 'bytes ${value.lengthInBytes}';
  } else if (value is List<String>) {
    return 'strings ${value.join("+")}';
  }
  return 'other ${value.length}';
}

String flags(Object value) {
  if (value is List<bool>) {
    final bool first = value[0];
    return first ? 'flags on' : 'flags off';
  }
  if (value is List<String>) {
    return 'text ${value[0].length}';
  }
  return 'neither';
}

class Encoders {
  static const enc = Utf8Encoder();
}

void main() {
  print(describe(Uint8List.fromList([1, 2, 3])));
  print(describe(<String>['a', 'b']));
  List<int> bytes = Encoders.enc.convert('hi');
  print(bytes);
  print(flags(<String>['abc']));
  print(flags(<bool>[true, false]));
  print(flags(<bool>[false]));
  print(flags(<int>[1]));
  Object words = <String>['x', 'y'];
  print(words is List<bool>);
  print(words is List<String>);
  Object bits = <bool>[true];
  print(bits is List<bool>);
  print(bits is List<String>);
  print(<bool>[true].map((b) => !b).toList() is List<bool>);
}
