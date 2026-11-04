class Widget { String build() => 'base'; }
class Text extends Widget { final String s; Text(this.s); @override String build() => 'T:$s'; }
