enum Color { red, green }
int f(int x) => x;
void main() {
  print([f, (int y) => y].runtimeType);
  print([1, 'a'].runtimeType);
  print(['a', 'b' as Comparable].runtimeType);
  print([Color.red, Color.green].runtimeType);
  print([1, 2.5].runtimeType); print([1, 2].runtimeType); print([2.5].runtimeType);
  num n = 1; print([n, 2].runtimeType);
  print([Color.red, 1].runtimeType);
  print([1, null].runtimeType); print([null].runtimeType);
  print({1: 'a', 2.0: 'b'}.runtimeType);
  print({1, 2.5}.runtimeType);
  print([DateTime(2020), Duration.zero].runtimeType);
  print([1, true].runtimeType);
  print(['a', 1, 2.0].runtimeType);
}
