void main() {
  List<T> local<T>(T x) => <T>[x];
  print(local<num>(1) is List<int>);
  print(local(1) is List<int>);
  print(local<num>(2).runtimeType);
  print(local('s'));
}
