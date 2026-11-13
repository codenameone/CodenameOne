typedef Cb = void Function(String);
typedef Val = int;
class Box<T> { T v; Box(this.v); }
class Src { Box<Val> box() => Box(3); Box<Cb> cb() => Box((s) => print('p:' + s)); }
