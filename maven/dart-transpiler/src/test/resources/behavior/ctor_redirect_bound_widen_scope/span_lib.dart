class Span {
  final String text;
  final String _label;
  Span(this.text, this._label);
  String get label => _label;
}

extension SpanExt on Span {
  // `label` is the extended value's getter: ctx_lib.dart's top-level `label` is not
  // imported here, so it is not in lexical scope.
  Span upper() => Span(text.toUpperCase(), label);
  String describe() => '$text/$label';
}
