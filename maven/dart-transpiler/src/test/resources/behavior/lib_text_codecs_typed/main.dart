import 'dart:convert';

int sum(List<int> bytes) {
  var s = 0;
  for (final b in bytes) {
    s += b;
  }
  return s;
}

String up(String s) => s.toUpperCase();

class Holder {
  final List<int> data;
  Holder(this.data);
}

Iterable<List<int>> parts(String a, String b) sync* {
  yield utf8.encode(a);
  yield latin1.encode(b);
}

void main() {
  print(sum(utf8.encode('abc')));
  print(sum(ascii.encode('AB')));
  print(sum(latin1.encode('é')));
  print(up(utf8.decode([104, 105])));
  print(up(ascii.decode([111, 107])));
  print(up(latin1.decode([233])));
  print(up(base64.encode([1, 2, 3])));
  print(sum(base64.decode('AQID')));
  print(sum(base64Url.decode('-_8=')));
  final h = Holder(utf8.encode('xyz'));
  print(h.data.length);
  List<int> enc = utf8.encoder.convert('hi');
  print(sum(enc));
  String dec = utf8.decoder.convert([104, 105]);
  print(up(dec));
  String b64 = base64.encoder.convert([255]);
  print(b64);
  for (final p in parts('q', 'r')) {
    print(sum(p));
  }
  print(sum(utf8.encode('x') + latin1.encode('y')));
}
