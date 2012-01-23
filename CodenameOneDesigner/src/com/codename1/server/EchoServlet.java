/*
 * Copyright (c) 2012, Codename One. All rights reserved.
 */
package com.codename1.server;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet is used by the device to try to lookup the PC
 * if the device doesn't get a response from this servlet it 
 * assumes a direct connection is impossible and stops seeking
 * the PC to communicate directly with the cloud
 *
 * @author Shai Almog
 */
public class EchoServlet extends HttpServlet {
    public EchoServlet(){}

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter w = response.getWriter();
        w.print("CODENAMEONE");
        w.close();
    }
}
