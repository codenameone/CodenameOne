/*
 *  This code originates from the bouncy castle library and didn't have a copyright header
 */

package com.codename1.util;

/// Class representing a simple version of a big decimal. A
/// `BigDecimal` is basically a
/// `BigInteger` with a few digits on the right of
/// the decimal point. The number of (binary) digits on the right of the decimal
/// point is called the `scale` of the `BigDecimal`.
/// Unlike in `BigDecimal`, the scale is not adjusted
/// automatically, but must be set manually. All `BigDecimal`s
/// taking part in the same arithmetic operation must have equal scale. The
/// result of a multiplication of two `BigDecimal`s returns a
/// `BigDecimal` with double scale.
public class BigDecimal {
    //private final BigInteger bigInt;
    //private final int scale;

    TBigDecimal peer;

    /// Constructor for `BigDecimal`. The value of the
    /// constructed `BigDecimal` equals `bigInt / 10scale`.
    ///
    /// #### Parameters
    ///
    /// - `bigInt`: The `bigInt` value parameter.
    ///
    /// - `scale`: The scale of the constructed `BigDecimal`.
    public BigDecimal(BigInteger bigInt, int scale) {
        peer = new TBigDecimal(bigInt.peer, scale);
    }

    private BigDecimal(BigDecimal limBigDec) {
        peer = new TBigDecimal(limBigDec.toString());
    }

    private BigDecimal(TBigDecimal peer) {
        this.peer = peer;
    }

    /// Returns a `BigDecimal` with value `value / 2scale / 10scale`
    ///
    /// #### Parameters
    ///
    /// - `value`: @param value The value of the `BigDecimal` to be
    /// created.
    ///
    /// - `scale`: @param scale The scale of the `BigDecimal` to be
    /// created.
    ///
    /// #### Returns
    ///
    /// The such created `BigDecimal`.
    ///
    /// #### Deprecated
    ///
    /// @deprecated This method is not part of the JDK's `BigDecimal` class and its presence is historical,
    /// as the first implementation of Codename One's BigDecimal class was ported from BouncyCastle, which is different
    /// than the JDK's BigDecimal class in that is optimizes binary arithmetic. The implementation of this method
    /// is counter-intuitive since it performs a bitwise left shift on `value` before scaling it.  Use `int)`
    /// instead if you just want to convert a `BigInteger` into a `BigDecimal`.  **Do not rely on this method
    /// as it will be removed in a future version of Codename One.**.
    public static BigDecimal getInstance(BigInteger value, int scale) {
        return new BigDecimal(value.shiftLeft(scale), scale);
    }

    public BigDecimal adjustScale(int newScale) {
        return new BigDecimal(peer.setScale(newScale));
    }

    public BigDecimal add(BigDecimal b) {
        return new BigDecimal(peer.add(b.peer));
    }

    public BigDecimal add(BigInteger b) {
        return new BigDecimal(peer.add(new TBigDecimal(b.peer, 0)));
    }

    public BigDecimal negate() {
        return new BigDecimal(peer.negate());
    }

    public BigDecimal subtract(BigDecimal b) {
        return new BigDecimal(peer.subtract(b.peer));
    }

    public BigDecimal subtract(BigInteger b) {
        return new BigDecimal(peer.subtract(new TBigDecimal(b.peer, 0)));
    }

    public BigDecimal multiply(BigDecimal b) {
        return new BigDecimal(peer.multiply(b.peer));
    }

    public BigDecimal multiply(BigInteger b) {
        return new BigDecimal(peer.multiply(new TBigDecimal(b.peer, 0)));
    }

    public BigDecimal divide(BigDecimal b) {
        return new BigDecimal(peer.divide(b.peer));
    }

    public BigDecimal divide(BigInteger b) {
        return new BigDecimal(peer.divide(new TBigDecimal(b.peer, 0)));
    }

    public BigDecimal shiftLeft(int n) {
        throw new RuntimeException("Not implemented yet");
    }

    public int compareTo(BigDecimal val) {
        return peer.compareTo(val.peer);
    }

    public int compareTo(BigInteger val) {
        return peer.compareTo(new TBigDecimal(val.peer, 0));
    }

    public BigInteger floor() {
        BigInteger out = new BigInteger(peer.toBigInteger());
        if (peer.signum() < 0) {
            return out.subtract(new BigInteger("1", 0));
        }
        return out;
    }

    public BigInteger round() {
        BigInteger out = new BigInteger(peer.toBigInteger());
        BigDecimal outD = new BigDecimal(out, 0);

        BigInteger next = peer.signum() < 0 ? out.subtract(BigInteger.ONE) : out.add(BigInteger.ONE);
        BigDecimal nextD = new BigDecimal(next, 0);

        BigDecimal diffThis = new BigDecimal(outD.peer.abs().subtract(peer.abs()).abs());
        BigDecimal diffNext = new BigDecimal(outD.peer.abs().subtract(nextD.peer.abs()).abs());

        return diffThis.compareTo(diffNext) > 0 ? out : next;

    }

    public int intValue() {
        return floor().intValue();
    }

    public long longValue() {
        return floor().longValue();
    }

    /* NON-J2ME compliant.
public double doubleValue()
{
  return Double.valueOf(toString()).doubleValue();
}

public float floatValue()
{
  return Float.valueOf(toString()).floatValue();
}
 */
    public int getScale() {
        return peer.scale();
    }

    @Override
    public String toString() {
        return peer.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof BigDecimal)) {
            return false;
        }

        BigDecimal other = (BigDecimal) o;
        return peer.equals(other.peer);
    }

    @Override
    public int hashCode() {
        return peer.hashCode();
    }

}
