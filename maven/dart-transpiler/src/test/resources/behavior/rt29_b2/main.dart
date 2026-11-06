T id<T>(T x) => x;
void main() {
  Object g = id; print(g is T Function<T>(T));
  int Function(int) inst = id; print(inst.runtimeType); print(inst(7));
}
