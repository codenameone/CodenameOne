extension on String {
  String get label => 'A:' + this;
  String tag() => 'tagA:' + this;
}

String viaA(String s) => s.label;
String tagA(String s) => s.tag();
