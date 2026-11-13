T id<T>(T x) => x;
int a(int x) => x;
void main() {
  print(id(a)(9));
}
