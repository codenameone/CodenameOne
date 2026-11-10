class Base {
  String who() => 'B.Base';
  String only() => 'only in B';
}

mixin M {
  String tag() => 'B.M';
  int weight() => 42;
}

class FromB extends Base with M {
  @override
  String who() => 'FromB>' + super.who();
}
