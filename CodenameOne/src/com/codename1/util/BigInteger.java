/*
 *  This code originates from the bouncy castle library and didn't have a copyright header
 */

package com.codename1.util;

import java.util.Random;

/**
 * A simplified version of big integer from the bouncy castle implementation
 */
public class BigInteger
{
    
    public static final BigInteger ZERO = new BigInteger(0, new int[0]);
    public static final BigInteger ONE = valueOf(1);

    TBigInteger peer;
    
    private BigInteger()
    {
        peer = new TBigInteger(0, new int[0]);
    }

    private BigInteger(int signum, int[] mag)
    {
        peer = new TBigInteger(signum, mag);
    }

    public BigInteger(String sval) throws NumberFormatException
    {
        this(sval, 10);
    }

    public BigInteger(String sval, int rdx) throws NumberFormatException
    {
       peer = new TBigInteger(sval, rdx);
    }

    public BigInteger(byte[] bval) throws NumberFormatException
    {
       peer = new TBigInteger(bval);
    }

    
    BigInteger(TBigInteger peer) {
        this.peer = peer;
    }

    public BigInteger(int sign, byte[] mag) throws NumberFormatException
    {
        peer = new TBigInteger(sign, mag);
    }

    public BigInteger(int numBits, Random rnd) throws IllegalArgumentException
    {
        peer = new TBigInteger(numBits, rnd);
        
    }

    private static final int BITS_PER_BYTE = 8;
    private static final int BYTES_PER_INT = 4;

    

    private static final byte[] rndMask = {(byte)255, 127, 63, 31, 15, 7, 3, 1};

    public BigInteger(int bitLength, int certainty, Random rnd) throws ArithmeticException
    {
        peer = new TBigInteger(bitLength, certainty, rnd);
    }

    public BigInteger abs()
    {
        return (peer.sign >= 0) ? this : this.negate();
    }



    public BigInteger add(BigInteger val) throws ArithmeticException
    {
        return new BigInteger(peer.add(val.peer));
    }

    

    public BigInteger and(
        BigInteger value)
    {
        return new BigInteger(peer.and(value.peer));
    }

    public BigInteger andNot(
        BigInteger value)
    {
        return and(value.not());
    }

    public int bitCount()
    {
        return peer.bitCount();
    }

    private final static byte[] bitCounts = {0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4, 1,
        2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4,
        4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3,
        4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 2, 3, 3, 4, 3, 4, 4, 5,
        3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 1, 2, 2, 3, 2,
        3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 2, 3,
        3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6,
        7, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6,
        5, 6, 6, 7, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 4, 5, 5, 6, 5, 6, 6, 7, 5,
        6, 6, 7, 6, 7, 7, 8};

    

    public int bitLength()
    {
        return peer.bitLength();
    }

    //
    // bitLen(val) is the number of bits in val.
    //
    static int bitLen(int w)
    {
        // Binary search - decision tree (5 tests, rarely 6)
        return (w < 1 << 15 ? (w < 1 << 7
                ? (w < 1 << 3 ? (w < 1 << 1
                        ? (w < 1 << 0 ? (w < 0 ? 32 : 0) : 1)
                        : (w < 1 << 2 ? 2 : 3)) : (w < 1 << 5
                        ? (w < 1 << 4 ? 4 : 5)
                        : (w < 1 << 6 ? 6 : 7)))
                : (w < 1 << 11
                        ? (w < 1 << 9 ? (w < 1 << 8 ? 8 : 9) : (w < 1 << 10 ? 10 : 11))
                        : (w < 1 << 13 ? (w < 1 << 12 ? 12 : 13) : (w < 1 << 14 ? 14 : 15)))) : (w < 1 << 23 ? (w < 1 << 19
                ? (w < 1 << 17 ? (w < 1 << 16 ? 16 : 17) : (w < 1 << 18 ? 18 : 19))
                : (w < 1 << 21 ? (w < 1 << 20 ? 20 : 21) : (w < 1 << 22 ? 22 : 23))) : (w < 1 << 27
                ? (w < 1 << 25 ? (w < 1 << 24 ? 24 : 25) : (w < 1 << 26 ? 26 : 27))
                : (w < 1 << 29 ? (w < 1 << 28 ? 28 : 29) : (w < 1 << 30 ? 30 : 31)))));
    }

    

    public int compareTo(Object o)
    {
        if (o instanceof BigInteger) {
            return compareTo((BigInteger)o);
        }
        throw new IllegalArgumentException("BigInteger can only be compared to other BigIntegers");
    }

  
    public int compareTo(BigInteger val)
    {
        return peer.compareTo(val.peer);
    }


    public BigInteger divide(BigInteger val) throws ArithmeticException
    {
        return new BigInteger(peer.divide(val.peer));
    }

    public BigInteger[] divideAndRemainder(BigInteger val) throws ArithmeticException
    {
        TBigInteger[] presults = peer.divideAndRemainder(val.peer);
        BigInteger[] out = new BigInteger[presults.length];
        for (int i=0; i<presults.length; i++) {
            out[i] = new BigInteger(presults[i]);
        }
        return out;
    }

    public boolean equals(Object val)
    {
        if (val == this)
            return true;

        if (!(val instanceof BigInteger))
            return false;
        BigInteger biggie = (BigInteger)val;
        return peer.equals(biggie.peer);
    }

    public BigInteger gcd(BigInteger val)
    {
        return new BigInteger(peer.gcd(val.peer));
    }

    public int hashCode()
    {
        return peer.hashCode();
    }

    public int intValue()
    {
        return peer.intValue();
    }
    
    public byte byteValue()
    {
        return (byte)intValue();
    }

    /**
     * return whether or not a BigInteger is probably prime with a
     * probability of 1 - (1/2)**certainty.
     * <p>
     * From Knuth Vol 2, pg 395.
     */
    public boolean isProbablePrime(int certainty)
    {
        return peer.isProbablePrime(certainty);
    }

    public long longValue()
    {
        return peer.longValue();
    }

    public BigInteger max(BigInteger val)
    {
        return (compareTo(val) > 0) ? this : val;
    }

    public BigInteger min(BigInteger val)
    {
        return (compareTo(val) < 0) ? this : val;
    }

    public BigInteger mod(BigInteger m) throws ArithmeticException
    {
        return new BigInteger(peer.mod(m.peer));
    }

    public BigInteger modInverse(BigInteger m) throws ArithmeticException
    {
        return new BigInteger(peer.modInverse(m.peer));
    }

    

    public BigInteger modPow(BigInteger exponent, BigInteger m) throws ArithmeticException
    {
        return new BigInteger(peer.modPow(exponent.peer, m.peer));
    }



    public BigInteger multiply(BigInteger val)
    {
        return new BigInteger(peer.multiply(val.peer));
    }

    public BigInteger negate()
    {
        return new BigInteger(peer.negate());
    }

    public BigInteger not()
    {
        return add(ONE).negate();
    }

    public BigInteger pow(int exp) throws ArithmeticException
    {
        return new BigInteger(peer.pow(exp));
    }

    public static BigInteger probablePrime(
        int bitLength,
        Random random)
    {
        return new BigInteger(TBigInteger.probablePrime(bitLength, random));
    }

    public BigInteger remainder(BigInteger n) throws ArithmeticException
    {
        return new BigInteger(peer.remainder(n.peer));
    }

    

    public BigInteger shiftLeft(int n)
    {
        return new BigInteger(peer.shiftLeft(n));
    }


    public BigInteger shiftRight(int n)
    {
        return new BigInteger(peer.shiftRight(n));
    }

    public int signum()
    {
        return peer.signum();
    }

    public BigInteger subtract(BigInteger val)
    {
        return new BigInteger(peer.subtract(val.peer));
    }

    public byte[] toByteArray()
    {
        return peer.toByteArray();
    }

    public BigInteger xor(BigInteger val) 
    {
        return new BigInteger(peer.xor(val.peer));
    }

    public BigInteger or(
        BigInteger value)
    {
        return new BigInteger(peer.or(value.peer));
    }
    
    public BigInteger setBit(int n) 
        throws ArithmeticException 
    {
        return new BigInteger(peer.setBit(n));
    }
    
    public BigInteger clearBit(int n) 
        throws ArithmeticException 
    {
       return new BigInteger(peer.clearBit(n));
    }

    public BigInteger flipBit(int n) 
        throws ArithmeticException 
    {
        return new BigInteger(peer.flipBit(n));
    }

    
        
    public String toString()
    {
        return peer.toString();
    }

    public String toString(int rdx)
    {
        return peer.toString(rdx);

    }
    
    public static BigInteger valueOf(long val)
    {
        return new BigInteger(TBigInteger.valueOf(val));
    }

    public int getLowestSetBit()
    {
        return peer.getLowestSetBit();
    }

    public boolean testBit(int n) 
        throws ArithmeticException
    {
       return peer.testBit(n);
    }
}
