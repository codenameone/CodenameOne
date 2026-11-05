// Dart's bitwise operators bind tighter than == and !=; Java's do not.
const int shift = 5;
const int mask = 31;

bool bitAt(List<int> words, int pos) =>
    words[pos >> shift] & (1 << (pos & mask)) != 0;

void main() {
  var words = [0x5, 0x80000000];
  var bits = <bool>[];
  for (var p in [0, 1, 2, 63, 62]) {
    bits.add(bitAt(words, p));
  }
  print(bits);
  var x = 6;
  print((x | 1) == 7);
  print(x ^ 2 == 4);
  print((x >> 1) & (x << 1) != 0);
}
