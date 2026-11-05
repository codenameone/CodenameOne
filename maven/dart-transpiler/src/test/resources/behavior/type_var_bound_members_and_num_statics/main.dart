class S<T extends String> {
  T v;
  S(this.v);
  int len() => v.length;
  String up() => v.toUpperCase();
  bool has(String p) => v.contains(p);
}

class Chain<T extends String, U extends T> {
  U u;
  Chain(this.u);
  String tail() => u.substring(1);
}

class Wrap<L extends List<String>> {
  L items;
  Wrap(this.items);
  String joined() => items.join('+');
}

String fixed(num n) => n.toStringAsFixed(2);

int cmpNum<T extends num>(T a, T b) => a.compareTo(b);
int cmpStr<T extends String>(T a, T b) => a.compareTo(b);

void main() {
  final s = S<String>('hello');
  print(s.len());
  print(s.up());
  print(s.has('ell'));
  print(Chain<String, String>('world').tail());
  print(Wrap<List<String>>(['a', 'b']).joined());
  print(fixed(3.14159));
  print(fixed(2));
  num n = 7;
  print(n.abs().toStringAsFixed(1));
  print(n.round());
  print(n.clamp(1, 5));
  print(n.isNegative);
  print(n.isNaN);
  print(n.sign);
  print(n.floorToDouble());
  print(n.toInt().toStringAsFixed(3));
  num m = -2.5;
  print(m.toStringAsFixed(1));
  print(m.truncate());
  print(cmpNum(3, 9));
  print(cmpStr('z', 'a'));
  Comparable<num> cn = 4.5;
  print(cn.compareTo(1));
}
