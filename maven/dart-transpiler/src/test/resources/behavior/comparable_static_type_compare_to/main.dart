int viaT<T extends Comparable<T>>(T a, T b) => a.compareTo(b);
int viaNull<T extends Comparable<T>>(T? a, T b) => (a ?? b).compareTo(b);
void main() {
  print('e'.compareTo('c'));
  Comparable<String> c = 'e';
  print(c.compareTo('c'));
  print(viaT('e', 'c'));
  print(viaNull<String>('e', 'c'));
  dynamic d = 'e';
  print(d.compareTo('c'));
}
