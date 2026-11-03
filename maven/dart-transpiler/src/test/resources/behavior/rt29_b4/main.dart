typedef IntOp = int Function(int);
int a(int x) => x;
void main() {
  var set = <Type>{int, List<int>, List<int>, List, List<dynamic>, IntOp, a.runtimeType, Null, Object, dynamic};
  print(set.length);
  print(set);
}
