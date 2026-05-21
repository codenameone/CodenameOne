/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.debug.proxy;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CLI entry point for the on-device-debug proxy.
 *
 * Usage: {@code java -jar cn1-debug-proxy.jar --symbols=path/to/cn1-symbols.txt
 *         --device-port=55333 --jdwp-port=8000 [--no-jdwp]}
 *
 * - --symbols       : path to the sidecar emitted by the translator. Required.
 * - --device-port   : TCP port to listen on for the device. Default 55333.
 * - --jdwp-port     : TCP port to listen on for jdb / IDE. Default 8000.
 * - --no-jdwp       : skip the JDWP front-end; events are dumped to stdout.
 *                     Useful for shaking out the wire protocol before the
 *                     JDWP translation layer is ready.
 *
 * The proxy waits for both sides to connect, then forwards events from the
 * device to the JDWP listener (which translates them into JDWP events) and
 * commands from JDWP back to the device.
 */
public final class ProxyMain {

    public static void main(String[] args) throws Exception {
        String symbolsPath = null;
        int devicePort = 55333;
        int jdwpPort = 8000;
        boolean noJdwp = false;

        for (String a : args) {
            if (a.startsWith("--symbols=")) symbolsPath = a.substring("--symbols=".length());
            else if (a.startsWith("--device-port=")) devicePort = Integer.parseInt(a.substring("--device-port=".length()));
            else if (a.startsWith("--jdwp-port=")) jdwpPort = Integer.parseInt(a.substring("--jdwp-port=".length()));
            else if (a.equals("--no-jdwp")) noJdwp = true;
            else if (a.equals("--help") || a.equals("-h")) { printUsage(); return; }
            else {
                System.err.println("Unrecognised argument: " + a);
                printUsage();
                System.exit(2);
            }
        }
        if (symbolsPath == null) {
            System.err.println("--symbols=<path> is required");
            printUsage();
            System.exit(2);
        }

        SymbolTable symbols = SymbolTable.load(Paths.get(symbolsPath));
        System.out.println("Loaded " + symbols.allClasses().size() + " classes, "
                + symbols.allMethods().size() + " methods from " + symbolsPath);

        final DeviceConnection.DeviceListener listener;
        final JdwpServer jdwpServer;
        if (noJdwp) {
            listener = new LoggingListener(symbols);
            jdwpServer = null;
        } else {
            jdwpServer = new JdwpServer(jdwpPort, symbols);
            listener = jdwpServer;
            final JdwpServer js = jdwpServer;
            Thread t = new Thread(() -> {
                try {
                    js.acceptAndServe();
                } catch (Throwable e) {
                    System.err.println("[jdwp] " + e);
                    e.printStackTrace();
                }
            }, "cn1-debug-jdwp");
            t.setDaemon(true);
            t.start();
        }

        DeviceConnection dev = new DeviceConnection(devicePort, listener);
        if (jdwpServer != null) jdwpServer.setDevice(dev);
        try {
            dev.acceptAndServe();
        } catch (IOException e) {
            System.err.println("[device] " + e.getMessage());
        }
    }

    private static void printUsage() {
        System.err.println("Usage: cn1-debug-proxy --symbols=<path> [--device-port=<p>] [--jdwp-port=<p>] [--no-jdwp]");
    }
}
