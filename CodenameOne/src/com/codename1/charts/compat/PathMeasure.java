/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.codename1.charts.compat;

import com.codename1.ui.geom.GeneralPath;



/**
 *
 * @author shannah
 * @deprecated
 */
public class PathMeasure {

    GeneralPath path;
    boolean forceClosed = false;
    
    public PathMeasure(GeneralPath p, boolean b) {
        path = p;
        forceClosed = b;
    }
    
    public float getLength(){
        return 10;
    }

    public void getPosTan(int i, float[] coords, float[] tan) {
        
        //throw new RuntimeException("Tangents not implemented yet");
        
    }
    
}
