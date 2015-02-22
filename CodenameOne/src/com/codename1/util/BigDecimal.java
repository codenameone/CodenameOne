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

    private final BigInteger bigInt;
    private final int scale;

    /**
     * Returns a <code>BigDecimal</code> representing the same numerical
     * value as <code>value</code>.
     * @param value The value of the <code>BigDecimal</code> to be
     * created. 
     * @param scale The scale of the <code>BigDecimal</code> to be
     * created. 
     * @return The such created <code>BigDecimal</code>.
     */
    public static BigDecimal getInstance(BigInteger value, int scale)
    {
        return new BigDecimal(value.shiftLeft(scale), scale);
    }

    /**
     * Constructor for <code>BigDecimal</code>. The value of the
     * constructed <code>BigDecimal</code> equals <code>bigInt / 
     * 2<sup>scale</sup></code>.
     * @param bigInt The <code>bigInt</code> value parameter.
     * @param scale The scale of the constructed <code>BigDecimal</code>.
     */
    public BigDecimal(BigInteger bigInt, int scale)
    {
        if (scale < 0)
        {
            throw new IllegalArgumentException("scale may not be negative");
        }

        this.bigInt = bigInt;
        this.scale = scale;
    }

    private BigDecimal(BigDecimal limBigDec)
    {
        bigInt = limBigDec.bigInt;
        scale = limBigDec.scale;
    }

    private void checkScale(BigDecimal b)
    {
        if (scale != b.scale)
        {
            throw new IllegalArgumentException("Only BigDecimal of " +
                "same scale allowed in arithmetic operations");
        }
    }

    public BigDecimal adjustScale(int newScale)
    {
        if (newScale < 0)
        {
            throw new IllegalArgumentException("scale may not be negative");
        }

        if (newScale == scale)
        {
            return new BigDecimal(this);
        }

        return new BigDecimal(bigInt.shiftLeft(newScale - scale),
                newScale);
    }

    public BigDecimal add(BigDecimal b)
    {
        checkScale(b);
        return new BigDecimal(bigInt.add(b.bigInt), scale);
    }

    public BigDecimal add(BigInteger b)
    {
        return new BigDecimal(bigInt.add(b.shiftLeft(scale)), scale);
    }

    public BigDecimal negate()
    {
        return new BigDecimal(bigInt.negate(), scale);
    }

    public BigDecimal subtract(BigDecimal b)
    {
        return add(b.negate());
    }

    public BigDecimal subtract(BigInteger b)
    {
        return new BigDecimal(bigInt.subtract(b.shiftLeft(scale)),
                scale);
    }

    public BigDecimal multiply(BigDecimal b)
    {
        checkScale(b);
        return new BigDecimal(bigInt.multiply(b.bigInt), scale + scale);
    }

    public BigDecimal multiply(BigInteger b)
    {
        return new BigDecimal(bigInt.multiply(b), scale);
    }

    public BigDecimal divide(BigDecimal b)
    {
        checkScale(b);
        BigInteger dividend = bigInt.shiftLeft(scale);
        return new BigDecimal(dividend.divide(b.bigInt), scale);
    }

    public BigDecimal divide(BigInteger b)
    {
        return new BigDecimal(bigInt.divide(b), scale);
    }

    public BigDecimal shiftLeft(int n)
    {
        return new BigDecimal(bigInt.shiftLeft(n), scale);
    }

    public int compareTo(BigDecimal val)
    {
        checkScale(val);
        return bigInt.compareTo(val.bigInt);
    }

    public int compareTo(BigInteger val)
    {
        return bigInt.compareTo(val.shiftLeft(scale));
    }

    public BigInteger floor()
    {
        return bigInt.shiftRight(scale);
    }

    public BigInteger round()
    {
        BigDecimal oneHalf = new BigDecimal(ONE, 1);
        return add(oneHalf.adjustScale(scale)).floor();
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
        return scale;
    }

    public String toString()
    {
        if (scale == 0)
        {
            return bigInt.toString();
        }

        BigInteger floorBigInt = floor();
        
        BigInteger fract = bigInt.subtract(floorBigInt.shiftLeft(scale));
        if (bigInt.signum() == -1)
        {
            fract = ONE.shiftLeft(scale).subtract(fract);
        }

        if ((floorBigInt.signum() == -1) && (!(fract.equals(ZERO))))
        {
            floorBigInt = floorBigInt.add(ONE);
        }
        String leftOfPoint = floorBigInt.toString();

        char[] fractCharArr = new char[scale];
        String fractStr = fract.toString(2);
        int fractLen = fractStr.length();
        int zeroes = scale - fractLen;
        for (int i = 0; i < zeroes; i++)
        {
            fractCharArr[i] = '0';
        }
        for (int j = 0; j < fractLen; j++)
        {
            fractCharArr[zeroes + j] = fractStr.charAt(j);
        }
        String rightOfPoint = new String(fractCharArr);

        StringBuffer sb = new StringBuffer(leftOfPoint);
        sb.append(".");
        sb.append(rightOfPoint);

        return sb.toString();
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
        return ((bigInt.equals(other.bigInt)) && (scale == other.scale));
    }

    public int hashCode()
    {
        return bigInt.hashCode() ^ scale;
    }

}
