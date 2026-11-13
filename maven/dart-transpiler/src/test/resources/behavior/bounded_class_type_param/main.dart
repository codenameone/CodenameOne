class Num<T extends num> { final T n; Num(this.n); T twice() => (n * 2) as T; }
void main() { print(Num<int>(5).twice() + 1); }
