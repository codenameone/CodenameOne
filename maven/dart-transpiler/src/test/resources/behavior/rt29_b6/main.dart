typedef IntOp = int Function(int);
int a(int x) => x; int b(int y) => y + 1; String s(int x) => '';
void main() {
  var m = <Type, String>{};
  m[a.runtimeType] = 'a'; m[b.runtimeType] = 'b'; m[s.runtimeType] = 's';
  m[IntOp] = 'typedef';
  print(m.length); print(m.values.toList());
  print(a.runtimeType.hashCode == b.runtimeType.hashCode);
  print(Object == dynamic);
  print(<Map<String, int>>{}.length);
  print(Map<String, int> == Map<String, int>);
}
