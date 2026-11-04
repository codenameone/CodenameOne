class Item<T> {
  final T v;
  Item(this.v);
  Item.named({required T v}) : this(v);
  String toString() => 'a.Item<$T>($v)';
}
String describe(Item<int> i, {String tag = '-'}) => '$tag:$i';
T pick<T>(T a, T b) => b;
