class M1 { var k; }
mixin Mx { int get k => 5; set k(v) {} }
class UsesMx extends M1 with Mx {}
void main() { M1 m = UsesMx(); print(m.k); print(UsesMx().k + 1); }
