// Extension type members shadow the representation's members of the same name.

extension type Cents(int raw) {
  // Shadows int.+ : adding two amounts rounds up to a whole ten.
  Cents operator +(Cents o) => Cents(((raw + o.raw + 9) ~/ 10) * 10);
  bool get isEven => true;
  int abs() => -1;
}

class Cell {
  int v = 0;
}

extension type Doubling(Cell cell) {
  int get v => cell.v;
  set v(int x) => cell.v = x * 2;
}

extension type Tagged<T>(List<T> list) implements Iterable<T> {
  int get length => list.length + 100;
}

void main() {
  var a = Cents(11);
  var b = Cents(5);
  print((a + b).raw);
  print(a.isEven);
  print('$a');
  print(a.abs());
  var d = Doubling(Cell());
  d.v = 3;
  print(d.v);
  print(d.cell.v);
  var t = Tagged<int>([1, 2]);
  print(t.length);
  print(t.first);
  print(t.map((e) => e * 2).toList());
  Iterable<int> it = t;
  print(it.length);
}
