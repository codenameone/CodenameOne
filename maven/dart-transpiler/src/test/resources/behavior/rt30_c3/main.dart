void main() {
  var a = () { try { return 1; } catch (e) { return 2; } };
  print(a.runtimeType); print(a());
}
