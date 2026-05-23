/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.android;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import com.codename1.io.usb.UsbDevice;
import com.codename1.io.usb.UsbDeviceListener;
import com.codename1.ui.CN;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// USB host implementation for Android. Activity-scoped (uses
/// `AndroidImplementation.getContext()`); detach events flow via a single
/// shared `BroadcastReceiver`.
public final class AndroidUsb {
    private static final String TAG = "CN1Usb";
    private static final String ACTION_PERM = "com.codename1.usb.PERMISSION";

    private static final List<UsbDeviceListener> listeners
            = new ArrayList<UsbDeviceListener>();
    private static BroadcastReceiver attachReceiver;
    private static BroadcastReceiver permReceiver;

    private AndroidUsb() {
    }

    public static boolean isSupported() {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return false;
        return ctx.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_USB_HOST);
    }

    private static UsbManager usb() {
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return null;
        return (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
    }

    public static UsbDevice[] listDevices() {
        UsbManager um = usb();
        if (um == null) return new UsbDevice[0];
        Map<String, android.hardware.usb.UsbDevice> map = um.getDeviceList();
        UsbDevice[] out = new UsbDevice[map.size()];
        int i = 0;
        for (android.hardware.usb.UsbDevice d : map.values()) {
            out[i++] = wrap(d);
        }
        return out;
    }

    private static UsbDevice wrap(android.hardware.usb.UsbDevice d) {
        String mfr = null, prod = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mfr = d.getManufacturerName();
            prod = d.getProductName();
        }
        return new UsbDevice(d.getDeviceName(), d.getVendorId(),
                d.getProductId(), prod, mfr, d);
    }

    public static synchronized void addDeviceListener(UsbDeviceListener l) {
        if (listeners.contains(l)) return;
        listeners.add(l);
        ensureReceiverInstalled();
    }

    public static synchronized void removeDeviceListener(UsbDeviceListener l) {
        listeners.remove(l);
        if (listeners.isEmpty()) {
            uninstallReceiver();
        }
    }

    private static void ensureReceiverInstalled() {
        if (attachReceiver != null) return;
        Context ctx = AndroidImplementation.getContext();
        if (ctx == null) return;
        attachReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                android.hardware.usb.UsbDevice d = i.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (d == null) return;
                final UsbDevice wrapped = wrap(d);
                final boolean attached = UsbManager.ACTION_USB_DEVICE_ATTACHED
                        .equals(i.getAction());
                CN.callSerially(new Runnable() {
                    @Override public void run() {
                        UsbDeviceListener[] arr;
                        synchronized (AndroidUsb.class) {
                            arr = listeners.toArray(new UsbDeviceListener[listeners.size()]);
                        }
                        for (UsbDeviceListener l : arr) {
                            if (attached) l.onDeviceAttached(wrapped);
                            else l.onDeviceDetached(wrapped);
                        }
                    }
                });
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        f.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        ctx.registerReceiver(attachReceiver, f);
    }

    private static void uninstallReceiver() {
        Context ctx = AndroidImplementation.getContext();
        if (ctx != null && attachReceiver != null) {
            try { ctx.unregisterReceiver(attachReceiver); } catch (Throwable ignored) { }
        }
        attachReceiver = null;
    }

    public static void requestPermission(final UsbDevice device) {
        Context ctx = AndroidImplementation.getContext();
        UsbManager um = usb();
        if (ctx == null || um == null || device == null) return;
        if (permReceiver == null) {
            permReceiver = new BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent i) {
                    if (!ACTION_PERM.equals(i.getAction())) return;
                    android.hardware.usb.UsbDevice native_ =
                            i.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    final boolean granted = i.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    final UsbDevice wrapped = native_ != null ? wrap(native_) : device;
                    CN.callSerially(new Runnable() {
                        @Override public void run() {
                            UsbDeviceListener[] arr;
                            synchronized (AndroidUsb.class) {
                                arr = listeners.toArray(new UsbDeviceListener[listeners.size()]);
                            }
                            for (UsbDeviceListener l : arr) {
                                l.onPermissionResult(wrapped, granted);
                            }
                        }
                    });
                }
            };
            ctx.registerReceiver(permReceiver, new IntentFilter(ACTION_PERM));
        }
        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0,
                new Intent(ACTION_PERM), flags);
        um.requestPermission((android.hardware.usb.UsbDevice) device.getNativeDevice(), pi);
    }

    public static boolean hasPermission(UsbDevice device) {
        UsbManager um = usb();
        if (um == null || device == null) return false;
        return um.hasPermission((android.hardware.usb.UsbDevice) device.getNativeDevice());
    }

    public static InputStream openInputStream(UsbDevice device, int endpoint)
            throws IOException {
        return new UsbStream(device, endpoint, UsbConstants.USB_DIR_IN)
                .asInputStream();
    }

    public static OutputStream openOutputStream(UsbDevice device, int endpoint)
            throws IOException {
        return new UsbStream(device, endpoint, UsbConstants.USB_DIR_OUT)
                .asOutputStream();
    }

    /// Adapter that bridges an Android USB endpoint to a Java stream.
    /// Uses bulk transfers with a 5-second timeout.
    private static final class UsbStream {
        private final UsbDeviceConnection conn;
        private final UsbEndpoint endpoint;
        private final UsbInterface iface;

        UsbStream(UsbDevice device, int endpointAddr, int direction)
                throws IOException {
            UsbManager um = usb();
            if (um == null) throw new IOException("UsbManager unavailable");
            android.hardware.usb.UsbDevice native_ =
                    (android.hardware.usb.UsbDevice) device.getNativeDevice();
            if (native_ == null) throw new IOException("UsbDevice has no native handle");
            UsbInterface chosenIface = null;
            UsbEndpoint chosenEp = null;
            for (int i = 0; i < native_.getInterfaceCount() && chosenEp == null; i++) {
                UsbInterface ui = native_.getInterface(i);
                for (int j = 0; j < ui.getEndpointCount(); j++) {
                    UsbEndpoint ep = ui.getEndpoint(j);
                    if (ep.getAddress() == endpointAddr && ep.getDirection() == direction) {
                        chosenIface = ui;
                        chosenEp = ep;
                        break;
                    }
                }
            }
            if (chosenEp == null) {
                throw new IOException("Endpoint 0x" + Integer.toHexString(endpointAddr)
                        + " not found in direction " + direction);
            }
            this.conn = um.openDevice(native_);
            if (conn == null) throw new IOException("openDevice failed (no permission?)");
            this.iface = chosenIface;
            this.endpoint = chosenEp;
            if (!conn.claimInterface(iface, true)) {
                conn.close();
                throw new IOException("claimInterface failed");
            }
        }

        InputStream asInputStream() {
            return new InputStream() {
                private final byte[] one = new byte[1];
                @Override public int read() throws IOException {
                    int n = read(one, 0, 1);
                    return n <= 0 ? -1 : (one[0] & 0xFF);
                }
                @Override public int read(byte[] b, int off, int len) throws IOException {
                    byte[] buf = (off == 0) ? b : new byte[len];
                    int n = conn.bulkTransfer(endpoint, buf, len, 5000);
                    if (off != 0 && n > 0) System.arraycopy(buf, 0, b, off, n);
                    return n;
                }
                @Override public void close() {
                    try { conn.releaseInterface(iface); } catch (Throwable t) { /* ignore */ }
                    try { conn.close(); } catch (Throwable t) { /* ignore */ }
                }
            };
        }

        OutputStream asOutputStream() {
            return new OutputStream() {
                @Override public void write(int b) throws IOException {
                    write(new byte[]{(byte) b}, 0, 1);
                }
                @Override public void write(byte[] b, int off, int len) throws IOException {
                    byte[] buf = (off == 0) ? b : new byte[len];
                    if (off != 0) System.arraycopy(b, off, buf, 0, len);
                    int n = conn.bulkTransfer(endpoint, buf, len, 5000);
                    if (n < 0) throw new IOException("bulkTransfer write failed");
                }
                @Override public void close() {
                    try { conn.releaseInterface(iface); } catch (Throwable t) { /* ignore */ }
                    try { conn.close(); } catch (Throwable t) { /* ignore */ }
                }
            };
        }
    }
}
