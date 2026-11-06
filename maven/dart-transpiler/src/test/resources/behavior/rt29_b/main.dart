typedef IntOp = int Function(int);
int a(int x) => x; int b(int y) => y + 1; String s(int x) => '';
T id<T>(T x) => x;
List<T> wrap<T>(T x) => [x];
void main() {
  var m = <Type, String>{};
  m[a.runtimeType] = 'a'; m[b.runtimeType] = 'b'; m[s.runtimeType] = 's';
  m[IntOp] = 'typedef';
  print(m.length); print(m.values.toList());
  var set = <Type>{int, List<int>, List<int>, List, List<dynamic>, Map<String, int>, Map<String,int>, IntOp, a.runtimeType, Null, Object, dynamic};
  print(set.length);
  print(set);
  print(List == List<dynamic>);
  print([1].runtimeType == List<int>);
  print(<dynamic>[].runtimeType == List);
  print({}.runtimeType == Map);
  print(<String,dynamic>{}.runtimeType == Map<String,dynamic>);
  print(<int?>[].runtimeType);
  print(<int Function(int)>[].runtimeType);
  print(<int Function(int)?>[].runtimeType);
  print(id<int>(3)); print(id(a)(9)); print(wrap<IntOp>(a).runtimeType);
  print(id.runtimeType);
  Object g = id; print(g is T Function<T>(T));
  int Function(int) inst = id; print(inst.runtimeType); print(inst(7));
  print(Set.from([int, int, String]).length);
  print(a.runtimeType.hashCode == b.runtimeType.hashCode);
  print(a.runtimeType == b.runtimeType);
  print(s.runtimeType == a.runtimeType);
}
