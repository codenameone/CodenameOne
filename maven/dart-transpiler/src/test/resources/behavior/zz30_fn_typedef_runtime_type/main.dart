typedef Fn<T> = T Function(T, {int? n});
typedef Req<T> = void Function(T a, {required String s});
typedef V = void Function(int);
typedef VN = void Function(int, {String? label});
typedef VO = void Function([int]);
typedef R = int Function(int);

void f(int x) {}
void g(int x, {String? label}) {}
void h([int x = 0]) {}
int k(int x) => x;

void main() {
  print(<Fn<int>>[].runtimeType);
  print(<Fn<String>>[].runtimeType == <Fn<String>>[].runtimeType);
  print(<Req<int>>[].runtimeType);
  int fi(int a, {int? n}) => a;
  print(fi is Fn<int>);
  print(fi is Fn<String>);
  print(f.runtimeType);
  print(f.runtimeType == V);
  print(f.runtimeType == R);
  print(k.runtimeType == R);
  print(k.runtimeType == V);
  print(g.runtimeType);
  print(g.runtimeType == VN);
  print(g.runtimeType == V);
  print(h.runtimeType);
  print(h.runtimeType == VO);
  var c = (int x) {};
  print(c.runtimeType == V);
  print(c.runtimeType);
  var d = (int x) => print(x);
  print(d.runtimeType);
  var e = (int x, {String? label}) {
    return;
  };
  print(e.runtimeType);
  print(e.runtimeType == VN);
  void Function(int) typed = (x) {};
  print(typed.runtimeType == V);
  print(V);
  print(VN);
  print(<void Function(int)>[].runtimeType);
  print(<List<void Function(String)>>[].runtimeType);
  print(<Map<String, void Function()>>[].runtimeType);
  print(<V>[].runtimeType == <void Function(int)>[].runtimeType);
  var fs = <void Function(int)>[f];
  print(fs.runtimeType);
  print(fs.first.runtimeType == V);
  print([g].runtimeType);
  more();
}

void more() {
  dynamic Function(int) f = (x) {};
  print(f.runtimeType);
  Object? Function(int) g = (x) {};
  print(g.runtimeType);
}
