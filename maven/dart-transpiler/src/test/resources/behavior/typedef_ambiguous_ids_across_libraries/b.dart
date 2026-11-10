typedef Id = String;
typedef Fn = String Function(String);
typedef _Hid = double;
class Holder<T extends Id> { T v; Holder(this.v); int len() => v.length; }
extension IdX on Id { String shout() => toUpperCase() + '!'; }
