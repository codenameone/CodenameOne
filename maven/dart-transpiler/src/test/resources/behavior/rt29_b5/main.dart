void main() {
  print(<int Function(int)>[].runtimeType);
  print(<int Function(int)?>[].runtimeType);
  print(List == List<dynamic>);
  print(<dynamic>[].runtimeType == List);
}
