typedef IntOp = int Function(int);
int a(int x) => x;
List<T> wrap<T>(T x) => [x];
void main() {
  print(wrap<IntOp>(a).runtimeType);
}
