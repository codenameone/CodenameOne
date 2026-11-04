class K { static String s<T>(T x, [T? y]) => '$T:$x:$y'; }
void main() {
  String Function(int, [int?]) c = K.s;
  print(c(3));
  print(c == K.s<int>);
}
