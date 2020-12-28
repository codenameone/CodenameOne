/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.testnatives;

import com.codename1.system.NativeInterface;

/**
 *
 * @author shannah
 */
public interface MyNativeInterface extends NativeInterface {

    public double[] getDouble();
    public double[] setDoubles(double[] doubles);
    public int[] getInts();
    public int[] setInts(int[] ints);
    public byte[] getBytes();
    public byte[] setBytes(byte[] bytes);
    
}
