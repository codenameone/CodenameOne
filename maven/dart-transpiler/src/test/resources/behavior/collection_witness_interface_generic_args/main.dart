class Box<T> { T v; Box(this.v); }
abstract class I {}
class C implements I {}
void main() {
  print(<I>[].runtimeType);
  print(<Box<num>>[].runtimeType);
  List<I> l = [C()];
  print(l.runtimeType);
}
