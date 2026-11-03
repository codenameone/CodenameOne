void main() {
  var c = DateTime.now().year > 2000;
  var y = c ? 7 : 2.0;
  var d = !c ? 7 : 2.5;
  print(y ~/ 2);
  print(d ~/ 2);
  print(y % 4);
  print(d % 2);
  print(y.remainder(4));
  print(d.remainder(2));
  print(y < d);
  print(y >= 7);
  print(-y);
  print(-d);
  print(y.compareTo(d));
  print(d.compareTo(y));
  print(y.clamp(0, 5));
  print(d.clamp(3, 5));
  print(y * d);
  print(y - d);
  print(y.abs());
  print((-d).abs());
  num n = y;
  n = n ~/ 3;
  print(n);
  try { print(y ~/ 0); } on UnsupportedError { print('div0'); }
}
