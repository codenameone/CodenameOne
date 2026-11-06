import 'base.dart' as base;
class Widget extends base.Widget { @override String build() => 'mine/' + super.build(); }
class Text extends base.Text { Text(String s) : super(s.toUpperCase()); @override String build() => 'my' + super.build(); }
