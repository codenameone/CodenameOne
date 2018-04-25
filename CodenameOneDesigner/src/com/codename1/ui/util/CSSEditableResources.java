/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui.util;

import com.codename1.ui.EditorTTFFont;
import com.codename1.ui.util.EditableResources;
import java.io.File;

/**
 *
 * @author shannah
 */
public class CSSEditableResources extends EditableResources {
    
    File resourceFile;
    
    public CSSEditableResources(File resourceFile) {
        this.resourceFile = resourceFile;
    }
    
    @Override
    com.codename1.ui.Font createTrueTypeFont(com.codename1.ui.Font f, String fontName, String fileName, float fontSize, int sizeSetting) {
        
        File fontFile = new File(resourceFile.getParentFile(), fileName);
        if(fontFile.exists()) {
            return new EditorTTFFont(fontFile, sizeSetting, fontSize, f);
        }
        return super.createTrueTypeFont(f, fontName, fileName, fontSize, sizeSetting);
    }
}
