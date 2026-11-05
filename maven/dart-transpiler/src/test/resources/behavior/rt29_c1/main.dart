(int, int Function(int)) pair() => (3, (x) => x + 1);
void main() {
  var p = pair();
  var f = p.$2;
  print(f(p.$1));
}
