T id<T>(T x) => x;
int a(int x) => x;
void main() {
  print({}.runtimeType == Map);
  print(<String,dynamic>{}.runtimeType == Map<String,dynamic>);
  print(<int?>[].runtimeType);
  print(id(a)(9));
  print(Set.from([int, int, String]).length);
}
