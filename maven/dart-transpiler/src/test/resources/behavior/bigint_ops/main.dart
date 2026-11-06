void show(String label, Object? v) {
  print('$label: $v');
}

void main() {
  final a = BigInt.parse('123456789012345678901234567890');
  final b = BigInt.parse('-987654321098765432109876543210');
  final c = BigInt.from(1000000007);
  show('a', a);
  show('b', b);
  show('add', a + b);
  show('sub', a - b);
  show('mul', a * b);
  show('tdiv', b ~/ a);
  show('mod', b % a);
  show('rem', b.remainder(a));
  show('neg', -a);
  show('and', a & b);
  show('or', a | b);
  show('xor', a ^ b);
  show('not', ~a);
  show('shl', a << 70);
  show('shr', b >> 33);
  show('shr small', BigInt.from(-1) >> 5);
  show('cmp', a.compareTo(b));
  show('lt', a < b);
  show('ge', a >= a);
  show('eq', a == BigInt.parse('123456789012345678901234567890'));
  show('hash eq', a.hashCode == BigInt.parse('123456789012345678901234567890').hashCode);
  show('abs', b.abs());
  show('sign', '${a.sign} ${b.sign} ${BigInt.zero.sign}');
  show('bitLength', '${a.bitLength} ${b.bitLength} ${BigInt.from(-1).bitLength} ${BigInt.from(255).bitLength}');
  show('even', '${a.isEven} ${a.isOdd} ${b.isNegative}');
  show('validInt', '${c.isValidInt} ${a.isValidInt}');
  show('toInt', '${c.toInt()} ${BigInt.from(-42).toInt()}');
  show('toDouble', a.toDouble());
  show('toDouble b', b.toDouble());
  show('div', a / c);
  show('hex', a.toRadixString(16));
  show('bin', BigInt.from(-10).toRadixString(2));
  show('b36', b.toRadixString(36));
  show('parse hex', BigInt.parse('ff', radix: 16));
  show('parse 0x', BigInt.parse('0xDEADBEEFCAFEBABE1234'));
  show('parse ws', BigInt.parse('  -00042  '));
  show('tryParse bad', BigInt.tryParse('12x'));
  show('tryParse ok', BigInt.tryParse('zz', radix: 36));
  show('from double', BigInt.from(1e20));
  show('from neg double', BigInt.from(-2.9));
  show('consts', '${BigInt.zero} ${BigInt.one} ${BigInt.two}');
  show('pow', BigInt.two.pow(100));
  show('pow3', BigInt.from(-3).pow(41));
  show('modPow', BigInt.from(4).modPow(BigInt.from(13), BigInt.from(497)));
  show('modPow big', a.modPow(BigInt.parse('65537'), c));
  show('modInverse', BigInt.from(3).modInverse(BigInt.from(11)));
  show('modInverse big', a.modInverse(c));
  show('gcd', a.gcd(b));
  show('gcd2', BigInt.from(-12).gcd(BigInt.from(18)));
  show('toUnsigned', b.toUnsigned(64));
  show('toSigned', BigInt.from(255).toSigned(8));
  show('toSigned2', a.toSigned(40));
  show('fact', fact(30));
  show('bigdiv', fact(40) ~/ fact(25));
  show('bigmod', fact(40) % BigInt.parse('98765432109876543211'));
  try {
    BigInt.parse('nope');
  } on FormatException {
    print('parse threw FormatException');
  }
  try {
    a ~/ BigInt.zero;
  } catch (e) {
    print('div zero threw');
  }
  try {
    BigInt.from(2).modInverse(BigInt.from(4));
  } catch (e) {
    print('modInverse threw');
  }
}

BigInt fact(int n) {
  var r = BigInt.one;
  for (var i = 2; i <= n; i++) {
    r = r * BigInt.from(i);
  }
  return r;
}
