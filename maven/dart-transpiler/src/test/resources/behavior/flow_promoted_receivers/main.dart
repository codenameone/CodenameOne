void sh<T>(T x) {
  print('$T');
}

String? maybe(bool b) => b ? 'm' : null;

extension NullableTag on String? {
  String tag() => 'nullable';
}

extension Ext on String {
  String tag() => 'nonnull';
}

class Holder {
  final String? _priv = 'p';
  void run() {
    if (_priv != null) sh(_priv);
  }
}

void closures() {
  String? z = 'c';
  var k = () => z;
  sh(k());
}

void dispatch(bool f) {
  String? x = 'a';
  print(x.tag());
  x = maybe(f);
  print(x.tag());
  if (x != null) print(x.tag());
  String? n;
  print(n.tag());
  Object? o = 'str';
  print(o.runtimeType);
  String? s = 'v';
  print(s.runtimeType);
  print([s].runtimeType);
  print({s: 1}.runtimeType);
  var lst = [x];
  print(lst.runtimeType);
  x = null;
  print([x].runtimeType);
  print(x.tag());
}

void main() {
  Holder().run();
  closures();
  dispatch(true);
}
