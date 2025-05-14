/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.builders.util;


/**
 * Callback for IO updates from a buffered input/output stream
 *
 * @author Shai Almog
 */
public interface IOProgressListener {
    /**
     * Indicates the number of bytes that were read/written to/from the source stream
     *
     * @param source the source stream which can be either an input stream or an output stream
     * @param bytes the number of bytes read or written
     */
    public void ioStreamUpdate(Object source, int bytes);
}
