int parse(String? s) => s == null ? throw ArgumentError('null input') : int.parse(s);

String need(String? v) => v ?? (throw StateError('missing'));

void main() {
  print(parse('42'));
  try {
    parse(null);
  } catch (e) {
    print('caught: $e');
  }
  print(need('ok'));
  try {
    need(null);
  } on StateError catch (e) {
    print('state: ${e.message}');
  }
}
