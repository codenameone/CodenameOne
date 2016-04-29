/*
 *  This code originates from the bouncy castle library and didn't have a copyright header
 */

package com.codename1.util;

/**
 * Class representing a simple version of a big decimal. A
 * <code>BigDecimal</code> is basically a
 * {@link java.math.BigInteger BigInteger} with a few digits on the right of
 * the decimal point. The number of (binary) digits on the right of the decimal
 * point is called the <code>scale</code> of the <code>BigDecimal</code>.
 * Unlike in {@link java.math.BigDecimal BigDecimal}, the scale is not adjusted
 * automatically, but must be set manually. All <code>BigDecimal</code>s
 * taking part in the same arithmetic operation must have equal scale. The
 * result of a multiplication of two <code>BigDecimal</code>s returns a
 * <code>BigDecimal</code> with double scale.
 */
public class BigDecimal {
    private static final BigInteger ZERO = BigInteger.valueOf(0);
    private static final BigInteger ONE = BigInteger.valueOf(1);

    //private final BigInteger bigInt;
    //private final int scale;
    
    TBigDecimal peer;

    /**
     * Returns a <code>BigDecimal</code> with value <code>value / 2<sup>scale</sup> / 10<sup>scale</sup></code>
     * @param value The value of the <code>BigDecimal</code> to be
     * created. 
     * @param scale The scale of the <code>BigDecimal</code> to be
     * created. 
     * @return The such created <code>BigDecimal</code>.
     * @deprecated This method is not part of the JDK's <code>BigDecimal</code> class and its presence is historical, 
     * as the first implementation of Codename One's BigDecimal class was ported from BouncyCastle, which is different
     * than the JDK's BigDecimal class in that is optimizes binary arithmetic. The implementation of this method
     * is counter-intuitive since it performs a bitwise left shift on <code>value</code> before scaling it.  Use {@link #BigDecimal(com.codename1.util.BigInteger, int) }
     * instead if you just want to convert a <code>BigInteger</code> into a <code>BigDecimal</code>.  <strong>Do not rely on this method
     * as it will be removed in a future version of Codename One.</strong>.
     */
    public static BigDecimal getInstance(BigInteger value, int scale)
    {
        return new BigDecimal(value.shiftLeft(scale), scale);
    }

    /**
     * Constructor for <code>BigDecimal</code>. The value of the
     * constructed <code>BigDecimal</code> equals <code>bigInt / 
     * 10<sup>scale</sup></code>.
     * @param bigInt The <code>bigInt</code> value parameter.
     * @param scale The scale of the constructed <code>BigDecimal</code>.
     */
    public BigDecimal(BigInteger bigInt, int scale)
    {
        peer = new TBigDecimal(bigInt.peer, scale);
    }

    private BigDecimal(BigDecimal limBigDec)
    {
        peer = new TBigDecimal(limBigDec.toString());
    }

    private BigDecimal(TBigDecimal peer) {
        this.peer = peer;
    }

    public BigDecimal adjustScale(int newScale)
    {
        return new BigDecimal(peer.setScale(newScale));
    }

    public BigDecimal add(BigDecimal b)
    {
        return new BigDecimal(peer.add(b.peer));
    }

    public BigDecimal add(BigInteger b)
    {
        return new BigDecimal(peer.add(new TBigDecimal(b.peer, 0)));
    }

    public BigDecimal negate()
    {
        return new BigDecimal(peer.negate());
    }

    public BigDecimal subtract(BigDecimal b)
    {
        return new BigDecimal(peer.subtract(b.peer));
    }

    public BigDecimal subtract(BigInteger b)
    {
        return new BigDecimal(peer.subtract(new TBigDecimal(b.peer, 0)));
    }

    public BigDecimal multiply(BigDecimal b)
    {
        return new BigDecimal(peer.multiply(b.peer));
    }

    public BigDecimal multiply(BigInteger b)
    {
        return new BigDecimal(peer.multiply(new TBigDecimal(b.peer, 0)));
    }

    public BigDecimal divide(BigDecimal b)
    {
        return new BigDecimal(peer.divide(b.peer));
    }

    public BigDecimal divide(BigInteger b)
    {
        return new BigDecimal(peer.divide(new TBigDecimal(b.peer, 0)));
    }

    public BigDecimal shiftLeft(int n)
    {
        throw new RuntimeException("Not implemented yet");
    }

    public int compareTo(BigDecimal val)
    {
        return peer.compareTo(val.peer);
    }

    public int compareTo(BigInteger val)
    {
        return peer.compareTo(new TBigDecimal(val.peer, 0));
    }

    public BigInteger floor()
    {
        BigInteger out = new BigInteger(peer.toBigInteger());
        if (peer.signum() < 0) {
            return out.subtract(new BigInteger("1",0));
        }
        return out;
    }

    public BigInteger round()
    {
       BigInteger out = new BigInteger(peer.toBigInteger());
       BigDecimal outD = new BigDecimal(out, 0);
       
       BigInteger next = peer.signum() < 0 ? out.subtract(BigInteger.ONE) : out.add(BigInteger.ONE);
       BigDecimal nextD = new BigDecimal(next, 0);
       
       BigDecimal diffThis = new BigDecimal(outD.peer.abs().subtract(peer.abs()).abs());
       BigDecimal diffNext = new BigDecimal(outD.peer.abs().subtract(nextD.peer.abs()).abs());
       
       return diffThis.compareTo(diffNext) > 0 ? out : next;
       
    }

    public int intValue()
    {
        return floor().intValue();
    }
    
    public long longValue()
    {
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
    public int getScale()
    {
        return peer.scale();
    }

    public String toString()
    {
        return peer.toString();
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof BigDecimal))
        {
            return false;
        }

        BigDecimal other = (BigDecimal)o;
        return peer.equals(other.peer);
    }

    public int hashCode()
    {
        return peer.hashCode();
    }

}
