import 'dart:async';

class Box {
  final String label;
  final int size;
  Box(String raw)
      : label = (() {
          var t = raw.trim();
          return t.isEmpty ? 'empty' : t.toUpperCase();
        })(),
        size = (() => raw.length)();
}

class Once {
  Future<void>? _done;
  int runs = 0;
  Future<void> run() => _done ??= () {
        runs++;
        return Future.wait([Future.value(1), Future.value(2)]);
      }();
}

void main() async {
  var b = Box('  ab ');
  print('${b.label} ${b.size}');
  print(Box('   ').label);
  var o = Once();
  await o.run();
  await o.run();
  print('runs ${o.runs}');
  var n = (int x) {
    return x * 3;
  }(4);
  print(n);
  final total = () {
    var s = 0;
    for (var i = 1; i <= 4; i++) {
      s += i;
    }
    return s;
  }();
  print('total $total');
}
