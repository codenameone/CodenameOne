dynamic dyn() => 1;
void main() {
  List<dynamic> d = [1, 2];
  print(d is List<int>);
  var e = [dyn(), dyn()];
  print(e is List<int>);
  Set<dynamic> s = {1};
  print(s is Set<int>);
  Map<dynamic, dynamic> m = {1: 2};
  print(m is Map<int, int>);
  d.add('x'); print(d.length);
  Object f = (dynamic x) => x;
  print(f is int Function(int));
}
