import 'dart:core' hide String;
import 'dart:core' as c;
class String { final c.String v; String(this.v); c.String s() => 'S:$v'; c.String toString() => 'Str($v)'; }
c.String mk() => 'four';
