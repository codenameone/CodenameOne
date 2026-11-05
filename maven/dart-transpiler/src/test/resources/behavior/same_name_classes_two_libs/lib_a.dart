class Base {
  String who() => 'A.Base';
  int get depth => 1;
}

mixin M {
  String tag() => 'A.M';
}

class FromA extends Base with M {
  @override
  String who() => 'FromA>' + super.who();
}
