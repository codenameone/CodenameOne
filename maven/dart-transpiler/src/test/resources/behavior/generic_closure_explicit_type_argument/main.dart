void main() {
  var w = <T>(T x) => [x];
  print(w<Object>(1) is List<int>);
  print(w<num>(1).runtimeType);
  print(w(1).runtimeType);
  print(w<String>('a'));
}
