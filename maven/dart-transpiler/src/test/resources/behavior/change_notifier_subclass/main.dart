// A class that EXTENDS ChangeNotifier (rather than mixing it in) keeps its own
// listener list, may override dispose() and call super.dispose(), and its
// notifyListeners() reaches every listener added through addListener.
import 'package:flutter/foundation.dart';

class Counter extends ChangeNotifier {
  int value = 0;
  final List<String> log = <String>[];

  void bump() {
    value++;
    notifyListeners();
  }

  @override
  void dispose() {
    log.add('disposing at $value');
    super.dispose();
  }
}

class Doubled extends Counter {
  @override
  void bump() {
    value += 2;
    notifyListeners();
  }
}

void main() {
  final c = Counter();
  var heard = 0;
  void listener() => heard++;
  c.addListener(listener);
  c.bump();
  c.bump();
  print('value ${c.value} heard $heard listeners ${c.hasListeners}');
  c.removeListener(listener);
  c.bump();
  print('after remove heard $heard listeners ${c.hasListeners}');
  c.dispose();
  print(c.log.join(';'));

  final d = Doubled();
  final seen = <int>[];
  d.addListener(() => seen.add(d.value));
  d.bump();
  d.bump();
  print('doubled $seen');
  ChangeNotifier n = d;
  n.notifyListeners();
  print('as notifier $seen');
}
