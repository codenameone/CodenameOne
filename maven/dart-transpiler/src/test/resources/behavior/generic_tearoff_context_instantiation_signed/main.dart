T id<T>(T x) => x;
typedef Fn<T> = T Function(T);
List<T> one<T>(T x) => [x];
class K { static T s<T>(T x) => x; T m<T>(T x) => x; }
void main() {
  Fn<int> fi = id;
  print(fi is Fn<String>);
  print(fi is Fn<int>);
  print(fi(3));
  int Function(int) a = K.s; print(a is String Function(String));
  int Function(int) b = K().m; print(b is String Function(String));
  List<int> Function(int) c = one; print(c is List<String> Function(String));
  print(c is List<int> Function(int));
  print(c(4));
}
