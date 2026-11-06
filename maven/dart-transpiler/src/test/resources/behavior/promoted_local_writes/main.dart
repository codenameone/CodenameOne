class Box { int b = 1; }
extension BoxE on Box { int get v => b; set v(int x) { b = x; } }
void main() {
  num n = 2;
  if (n is int) { n += 3; print(n); n++; print(n); print(n--); print(n); n = n * 2; print(n); }
  Object o = 'q';
  if (o is String) { o = o + 'r'; print(o); }
  Object k = 'k';
  if (k is String) { do { print(k); k = 7; } while (k is! int); }
  print(k);
  Box? b = Box();
  print(b?.v++);
  print(b!.v--);
  print(b.v);
  b?.v += 10;
  print(b?.v);
  Box? none;
  none?.v ??= 4;
  none?.v += 4;
  print(none?.v);
}
