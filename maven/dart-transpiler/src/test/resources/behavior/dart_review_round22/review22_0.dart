// Never imported by main.dart: its extensions must not answer for main.
extension Loud on String {
  String get shout => 'wrong';
}

extension on int {
  int get twice => -1;
}
