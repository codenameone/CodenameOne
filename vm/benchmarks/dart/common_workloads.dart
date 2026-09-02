// Dart port of com.bench.CommonWorkloads, for head-to-head ParparVM vs Dart AOT.
//
// The point of this file is a LIKE-FOR-LIKE comparison, so it reproduces Java's
// semantics rather than writing idiomatic Dart:
//
//  * Java `int` is 32-bit and wraps. Dart's is 64-bit, so every 32-bit
//    expression is folded back with toSigned(32).
//  * Java `>>>` on an int shifts the 32-bit pattern. Dart's `>>>` operates on
//    64 bits, so the value is masked to 32 bits first.
//  * Java's String.hashCode is specified (s[0]*31^(n-1) + ...); Dart's is not,
//    and differs. It is reimplemented here so the checksum can match.
//
// Checksums are the contract: each workload returns a value that must equal the
// Java one exactly. A mismatch means the port is wrong and any ratio from it is
// meaningless -- the runner refuses to print ratios in that case.

import 'dart:math' as math;
import 'dart:typed_data';

const int _mask32 = 0xFFFFFFFF;

int _i32(int v) => v.toSigned(32);
int _ushr32(int v, int n) => (v & _mask32) >> n;

// ---- 1. integer arithmetic: dependent ALU chain ----
int intArithmetic() {
  int a = 0x12345678;
  int b = _i32(0x9E3779B9);
  int checksum = 0;
  for (int i = 0; i < 40000000; i++) {
    a = _i32(_i32(a * 1103515245 + 12345) ^ _ushr32(b, 3));
    b = _i32(_i32((b + a) * 5) - _i32(a << 7));
    checksum += (a ^ b) & 0xFFFF;
  }
  return checksum + a + b;
}

// ---- 2. long (64-bit) arithmetic: dependent chain ----
int longArithmetic() {
  int a = 0x0123456789ABCDEF;
  int b = -0x123456789;
  int checksum = 0;
  for (int i = 0; i < 30000000; i++) {
    a = (a * 6364136223846793005 + 1442695040888963407) ^ (b >>> 7);
    b = (b ^ (a << 13)) + (a >>> 11);
    checksum += (a + b) & 0xFF;
  }
  return checksum + a + b;
}

// ---- 3. floating point + transcendental ----
int mathTranscendental() {
  double acc = 1.0;
  double x = 0.5;
  for (int i = 0; i < 8000000; i++) {
    x = x + 0.000001 * (i & 1023);
    acc += math.sqrt(x) + math.sin(x) * math.cos(x) - math.sqrt(acc % 1000.0 + 1.0);
    if (acc > 1e12 || acc < -1e12) acc = acc % 1000.0;
  }
  final ByteData d = ByteData(8);
  d.setFloat64(0, acc);
  return d.getInt64(0);
}

// ---- 4. sequential array fill + reduce ----
final Int32List _seqArr = Int32List(8000000);
int arraySequential() {
  final Int32List arr = _seqArr;
  final int n = arr.length;
  int checksum = 0;
  int seed = _i32(0x9E3779B9);
  for (int i = 0; i < n; i++) {
    seed = _i32(seed * 1103515245 + 12345);
    arr[i] = seed;
  }
  for (int pass = 0; pass < 4; pass++) {
    int s = 0;
    for (int i = 0; i < n; i++) s += arr[i];
    checksum ^= s + pass;
  }
  return checksum;
}

// ---- 5. random-access gather ----
final Int32List _randArr = Int32List(4000000);
int arrayRandom() {
  final Int32List arr = _randArr;
  final int n = arr.length;
  for (int i = 0; i < n; i++) {
    arr[i] = ((i * 2654435761) >>> 8) > 0 ? _i32(i * 2654435761) : i;
  }
  int checksum = 0;
  int idx = 12345;
  for (int i = 0; i < 20000000; i++) {
    final int v = arr[(idx & 0x7fffffff) % n];
    checksum += v;
    idx = _i32(v ^ _i32(idx * 31 + 7));
  }
  return checksum;
}

// ---- 6. object allocation + GC churn ----
class _Node {
  final int v;
  final _Node? next;
  _Node(this.v, this.next);
}

int objectAllocation() {
  int checksum = 0;
  _Node? head;
  for (int i = 0; i < 8000000; i++) {
    head = _Node(i, head);
    if ((i & 511) == 0) {
      _Node? p = head;
      int steps = 0;
      while (p != null && steps < 48) {
        checksum += p.v;
        p = p.next;
        steps++;
      }
      head = null;
    }
  }
  return checksum;
}

// ---- non-escaping value object ----
class _Vec {
  final int x;
  final int y;
  _Vec(this.x, this.y);
  int getX() => x;
  int getY() => y;
}

int valueEscape() {
  int sum = 0;
  for (int i = 0; i < 8000000; i++) {
    final _Vec v = _Vec(i, i * 2);
    sum = (sum + v.getX() + v.getY()) & 0x3fffffff;
  }
  return sum;
}

// ---- 7. hash map churn ----
int hashMapChurn() {
  final Map<int, int> map = <int, int>{};
  int checksum = 0;
  const int window = 50000;
  for (int i = 0; i < 3000000; i++) {
    final int key = i & 0x3FFFF;
    final int? prev = map[key];
    map[key] = prev == null ? i : prev + i;
    if (prev != null) checksum += prev;
    if (map.length > window) map.clear();
  }
  return checksum + map.length;
}

// ---- 8. string building + hashing ----
/// Java's String.hashCode, which Dart does not guarantee.
int _javaHash(String s) {
  int h = 0;
  for (int i = 0; i < s.length; i++) {
    h = _i32(_i32(h * 31) + s.codeUnitAt(i));
  }
  return h;
}

final List<String?> _sbRing = List<String?>.filled(256, null);
int stringBuilding() {
  int checksum = 0;
  for (int i = 0; i < 400000; i++) {
    final StringBuffer sb = StringBuffer();
    sb.write('item-');
    sb.write(i);
    sb.write('-');
    sb.write(_i32(_i32(i * 31) ^ 0x55AA));
    sb.write(':');
    sb.write((i & 1) == 0 ? 'even' : 'odd');
    _sbRing[i & 255] = sb.toString();
    if ((i & 255) == 255) {
      for (int j = 0; j < 256; j++) {
        final String s = _sbRing[j]!;
        checksum += _javaHash(s) + s.length;
      }
    }
  }
  return checksum;
}

// ---- 9. recursion ----
int _fib(int n) => n < 2 ? n : _fib(n - 1) + _fib(n - 2);
int recursion() {
  int checksum = 0;
  for (int i = 0; i < 3; i++) checksum += _fib(35 + (i & 1));
  return checksum;
}

// ---- 10. quicksort ----
final Int32List _sortArr = Int32List(1500000);
void _quicksort(Int32List a, int lo, int hi) {
  while (lo < hi) {
    final int pivot = a[(lo + hi) >>> 1];
    int i = lo, j = hi;
    while (i <= j) {
      while (a[i] < pivot) i++;
      while (a[j] > pivot) j--;
      if (i <= j) {
        final int t = a[i];
        a[i] = a[j];
        a[j] = t;
        i++;
        j--;
      }
    }
    if (j - lo < hi - i) {
      _quicksort(a, lo, j);
      lo = i;
    } else {
      _quicksort(a, i, hi);
      hi = j;
    }
  }
}

int quicksortBench() {
  final Int32List a = _sortArr;
  final int n = a.length;
  int seed = _i32(0xCAFEBABE);
  for (int i = 0; i < n; i++) {
    seed = _i32(seed * 1103515245 + 12345);
    a[i] = seed;
  }
  _quicksort(a, 0, n - 1);
  int checksum = 0;
  for (int i = 0; i < n; i += 997) checksum += a[i] * (i + 1);
  for (int i = 1; i < n; i++) if (a[i - 1] > a[i]) checksum ^= 0xDEADBEEF;
  return checksum;
}

