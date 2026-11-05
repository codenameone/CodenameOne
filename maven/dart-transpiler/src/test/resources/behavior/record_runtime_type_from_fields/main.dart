(int, String) pair(int a) => (a, 'v$a');
void main() {
  var r = (null, 1, x: null);
  print([r.runtimeType, r, r == (null, 1, x: null)]);
  var p = pair(3);
  print(p.runtimeType);
  var q = (1.5, 'a', ok: true, n: 2);
  print(q.runtimeType);
  print((1, [1, 2]).runtimeType);
}
