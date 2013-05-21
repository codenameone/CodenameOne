/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.android;

import android.content.Intent;

/**
 *
 * @author Chen
 */
public class IntentResult {
    private int requestCode;
    private int resultCode;
    private Intent data;

    public IntentResult(int requestCode, int resultCode, Intent data) {
        this.requestCode = requestCode;
        this.resultCode = resultCode;
        this.data = data;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public Intent getData() {
        return data;
    }

    public int getResultCode() {
        return resultCode;
    }
    
    
    
}
