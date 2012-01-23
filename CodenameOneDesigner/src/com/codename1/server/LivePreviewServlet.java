/*
 * Copyright (c) 2012, Codename One. All rights reserved.
 */
package com.codename1.server;

import com.codename1.designer.LivePreview;
import com.codename1.designer.ResourceEditorView;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet effectively returns the current resource file and settings from
 * the user for the device preview mode.
 *
 * @author Shai Almog
 */
public class LivePreviewServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/Codename-One-Designer");
        response.setStatus(HttpServletResponse.SC_OK);
        DataOutputStream d = new DataOutputStream(response.getOutputStream());
        
        String modified = request.getParameter("m");
        DataOutputStream o = new DataOutputStream(response.getOutputStream());
        String key = request.getParameter("k");
        if(!key.equals(LivePreview.getPreviewKey())) {
            o.writeInt(-1);
            return;
        }

        File loadedFile = ResourceEditorView.getLoadedFile();
        if(loadedFile == null || ("" + loadedFile.lastModified()).equals(modified)) {
            o.writeInt(-1);
            o.close();
            return;
        }

        if(("" + loadedFile.lastModified()).equals(modified)) {
            o.writeInt(0);
            o.close();
            return;
        }
        o.writeInt(1);
        o.writeUTF(LivePreview.getMainFormSelection());
        String theme = LivePreview.getThemeSelection();
        if(theme == null) {
            theme = "";
        }
        o.writeUTF(theme);
        o.writeUTF("" + loadedFile.lastModified());
        byte[] data = new byte[(int)loadedFile.length()];
        DataInputStream in = new DataInputStream(new FileInputStream(loadedFile));
        in.readFully(data);
        in.close();
        o.write(data);
        o.close();		
        System.out.println("Sent response to " + request.getHeader("User-Agent"));
        System.out.println("Host " + request.getHeader("Host"));
    }    
}
