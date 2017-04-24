/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */

package com.codename1.charts.util;

import com.codename1.io.Log;
import com.codename1.l10n.L10NManager;
import com.codename1.l10n.ParseException;
import com.codename1.ui.Display;


/**
 *
 * @author shannah
 * @deprecated this is an internal implementation class
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
        try {
            String t = com.codename1.util.StringUtil.replaceAll(format, ",", "");
            t = com.codename1.util.StringUtil.replaceAll(t, " ", "");
            return Double.parseDouble(t);
        } catch(Exception err) {
            try {
                double val = L10NManager.getInstance().parseDouble(format);
                return val;
            } catch(Exception err2) {
                try {
                    double v2 = L10NManager.getInstance().parseCurrency(format);
                    return v2;
                } catch(Exception err3) {
                    Log.e(err3);
                    return 0;
                }
            }
        }
    }
    
}
