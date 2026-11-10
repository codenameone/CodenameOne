T id<T>(T x) => x;
void main() {
  print(id<int>(3));
  print(id.runtimeType);
}
