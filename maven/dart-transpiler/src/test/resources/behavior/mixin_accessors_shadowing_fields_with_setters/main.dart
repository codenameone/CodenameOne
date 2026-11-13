class M1 { var k; int n = 1; }
mixin Mx { int get k => 5; set k(v) { print('Mx.set $v'); } }
mixin Nx { int get n => 40; set n(int v) { print('Nx.set $v'); } }
class UsesMx extends M1 with Mx, Nx {}
void main() {
  M1 m = UsesMx();
  print(m.k); m.k = 7; print(m.k);
  print(m.n + 2); m.n = 3; print(m.n);
}
