/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.codename1.charts.util;

import com.codename1.l10n.L10NManager;
import com.codename1.l10n.ParseException;
import com.codename1.ui.Display;


/**
 *
 * @author shannah
 */
public class NumberFormat {
    
    static NumberFormat instance = new NumberFormat();
    
    private L10NManager l10n(){
        return Display.getInstance().getLocalizationManager();
    }
    
    public static NumberFormat getNumberInstance() {
        return instance;
    }

    public String format(double label) {
        return l10n().format(label);
    }

    public void setMaximumFractionDigits(int i) {
        
    }

    

    public double parseDouble(String format) throws ParseException {
        format = com.codename1.util.StringUtil.replaceAll(format, ",", "");
        return Double.parseDouble(format);
    }
    
}
