// int.parse's radix, locale-free case conversion, Dart's double notation,
// microsecond DateTimes and Uri equality, through transpiled code.

void main() {
  print(int.parse('ff', radix: 16));
  print(int.tryParse('0x1F'));
  print(int.tryParse('zz', radix: 36));
  print('istanbul'.toUpperCase());
  print('MiXeD'.toLowerCase());
  print(0.0001);
  print(1e16);
  print(1e21);
  final a = DateTime.utc(2024, 1, 15, 10, 30, 0, 0, 1);
  final b = DateTime.utc(2024, 1, 15, 10, 30, 0, 0, 999);
  print(a == b);
  print(b.difference(a).inMicroseconds);
  print(Uri.parse('https://EXAMPLE.com/a') == Uri.parse('https://example.com/a'));
}
