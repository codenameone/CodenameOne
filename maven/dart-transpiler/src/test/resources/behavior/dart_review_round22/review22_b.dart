extension on String {
  String get label => 'B:' + this;
  String tag() => 'tagB:' + this;
}

String viaB(String s) => s.label;
String tagB(String s) => s.tag();
