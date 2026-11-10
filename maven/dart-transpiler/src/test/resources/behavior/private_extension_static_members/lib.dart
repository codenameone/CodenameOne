extension Util on String {
  static int count = 0;
  static String make(int n) { count++; return 'x' * n; }
  static int _hidden = 7;
  static int peek() => _hidden;
}
extension type Id(int v) {
  static int created = 0;
  static Id next() { created++; return Id(created); }
}
