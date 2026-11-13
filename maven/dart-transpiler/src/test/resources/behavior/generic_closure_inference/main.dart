// Generic methods whose type parameter is bound by a closure argument, and closure
// parameters that are Java keywords.

class Painter {
  final StringBuffer _buffer = StringBuffer();
  int depth = 0;

  T _wrap<T>(T Function() callback, {String? color}) {
    if (color != null) _buffer.write('[$color]');
    depth++;
    final result = callback();
    depth--;
    if (color != null) _buffer.write('[/]');
    return result;
  }

  String render(int n) {
    _wrap(() => _buffer.write('>' * 2), color: 'blue');
    _wrap(() {
      _buffer.write('|');
    }, color: null);
    final length = _wrap(() {
      _buffer.write('-' * n);
      return n + 1;
    }, color: 'red');
    final label = _wrap(() => 'len=$length');
    _buffer.write(label);
    final doubled = length * 2;
    _buffer.write(' doubled=$doubled');
    return _buffer.toString();
  }
}

T twice<T>(T Function() f) {
  f();
  return f();
}

void main() {
  print(Painter().render(3));
  var count = 0;
  final v = twice(() => ++count);
  print(v + 10);
  final words = ['char', 'int', 'class'];
  print(words.map((char) => char.length).toList());
  print(words.where((int) => int.startsWith('c')).join(','));
  words.forEach((byte) {
    final double = byte.toUpperCase();
    print(double);
  });
  String Function(String) f = (new_) => new_ + '!';
  print(f('x'));
}
