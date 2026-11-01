// A mixin that implements an interface and reads the interface's abstract
// getters, which only the applying class provides.

abstract class Pipe<T> {
  String get label;
  List<T> get items;
  void describe();
}

abstract mixin class PipeMixin<T> implements Pipe<T> {
  @override
  void describe() {
    print('$label has ${items.length}');
  }
}

class SimplePipe<T> with PipeMixin<T> {
  @override
  final String label;
  @override
  final List<T> items;
  SimplePipe(this.label, this.items);
}

Pipe<int> doubled(SimplePipe<int> p) => SimplePipe('twice', [...p.items, ...p.items]);

void main() {
  final p = SimplePipe<int>('nums', [1, 2, 3]);
  p.describe();
  doubled(p).describe();
}
