class Notifier {
  final _ls = <void Function()>[];
  void addListener(void Function() l) => _ls.add(l);
  void removeListener(void Function() l) => _ls.remove(l);
  void notify() { for (final l in List.of(_ls)) l(); }
  int get count => _ls.length;
}
class W {
  int hits = 0;
  void onChange() { hits++; }
}
void main() {
  final n = Notifier();
  final ws = List.generate(50, (_) => W());
  for (var r = 0; r < 200; r++) {
    for (final w in ws) n.addListener(w.onChange);
    n.notify();
    for (final w in ws) n.removeListener(w.onChange);
  }
  print('count ${n.count} hits ${ws[0].hits}');
  final w = W();
  n.addListener(w.onChange); n.addListener(w.onChange);
  n.removeListener(w.onChange);
  print('dup ${n.count}');
  final set = <void Function()>{};
  for (var i = 0; i < 1000; i++) { set.add(w.onChange); }
  print('set ${set.length}');
  set.remove(w.onChange);
  print('set after ${set.length}');
}
