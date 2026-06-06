/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.windows;

import com.codename1.camera.CameraFacing;
import com.codename1.camera.CameraFrame;
import com.codename1.camera.CameraInfo;
import com.codename1.camera.CameraSessionOptions;
import com.codename1.camera.CapturedPhoto;
import com.codename1.camera.FlashMode;
import com.codename1.camera.FrameFormat;
import com.codename1.camera.FrameListener;
import com.codename1.camera.PhotoCaptureOptions;
import com.codename1.impl.CameraImpl;
import com.codename1.io.FileSystemStorage;
import com.codename1.ui.Display;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.geom.Dimension;
import com.codename1.util.AsyncResource;
import com.codename1.util.Base64;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Backend for the low-level {@code com.codename1.camera.Camera} API on the native
 * Windows port. The desktop port has no real capture device wired up yet, so this
 * is a SYNTHETIC backend: it advertises a back/front camera, delivers a fixed
 * synthetic frame (a real, decodable 320x240 JPEG embedded below) through the
 * frame-listener pipeline, and captures the same frame as a still photo. It
 * mirrors the simulator's JavaSECameraImpl (which generates synthetic frames via
 * AWT) but uses no java.awt / javax.imageio, which the ParparVM clean target does
 * not ship. A real Media Foundation webcam capture (the port already links MF for
 * media playback) is the intended future replacement.
 */
public class WindowsCameraImpl extends CameraImpl {

    private static final int FRAME_W = 320;
    private static final int FRAME_H = 240;

    /** A real 320x240 JPEG (synthetic gradient + label), base64-encoded. */
    private static final String SYNTHETIC_JPEG_B64 =
            "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAA4KCw0LCQ4NDA0QDw4RFiQXFhQUFiwgIRokNC43NjMuMjI6QVNGOj1OPjIySGJJTlZY" +
            "XV5dOEVmbWVabFNbXVn/2wBDAQ8QEBYTFioXFypZOzI7WVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZWVlZ" +
            "WVlZWVlZWVn/wAARCADwAUADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUF" +
            "BAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVW" +
            "V1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi" +
            "4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAEC" +
            "AxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVm" +
            "Z2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq" +
            "8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDgMUuKXFOxX0FzFMbilxTsUuKVzRMbilxTsUuKVy0xuKXFOxS4pXLTG4p2KXFLilc0TExS" +
            "4pcUuKVy0xMUuKdilxU3NExuKXFOxS4pXLTG4pcU7FLilctMbilxTsUuKVzRMTFLilxS4pXLTExS4pcUuKVzRMTFLinYpcUrlpjc" +
            "UuKdilxU3LTG4pcU7FLilc0TG4p2KXFLilctMTFLilxS4pXNExMUuKXFOxSuWmNxS4p2KXFK5aY3FLinYpcUrmiZy+KdilxS4r0r" +
            "n50mbnhrw6muR3kkl6tmlqFZmZNwwd2STkYxtrT/AOER0n/oarH8k/8Ai6XwcP8Ain/E3/Xr/wCySVyOK57ylJpStY1T0Ou/4RLS" +
            "f+hpsfyT/wCLo/4RLSf+hpsfyT/4uuTxS4p8s/5vyLTOs/4RLSf+hosvyT/4ul/4RPSf+hosvyT/AOLrk8UuKXLP+b8ikzq/+ET0" +
            "r/oaLL8k/wDi6X/hE9K/6Gey/JP/AIuuUxS4pcs/5vyLR1f/AAimlf8AQz2X5J/8XS/8IppX/Qz2X5J/8XXKYpcUuWX835Fq/c6r" +
            "/hFdK/6Gay/Jf/i6X/hFdL/6Gay/Jf8A4uuVxS4pcsv5vyLV+51X/CK6X/0M1n+S/wDxdH/CLaX/ANDLZ/kv/wAXXLYp2KXLL+Yt" +
            "KXc6j/hFtL/6GWz/ACX/AOLpf+EX0v8A6GWz/Jf/AIuuXxS4pWl/MUlLudP/AMIvpn/QyWf5L/8AF0v/AAi+mf8AQyWf5L/8XXMY" +
            "pcUrS/mLSl/MdP8A8Ixpn/Qx2f5L/wDF0f8ACMaZ/wBDHZ/kv/xdczilxStLuWoz/m/I6b/hGNM/6GO0/Jf/AIul/wCEZ03/AKGK" +
            "0/Jf/i65nFLilaXctRn/ADfgjpf+EZ03/oYrT8l/+Lpf+Ea03/oYrT8l/wDi65rFLii0u5SjP+f8EdL/AMI1pv8A0MNp+S//ABdH" +
            "/CNab/0MNp+S/wDxdc3inYpWl3LUKn8/4I6P/hG9N/6GG0/Jf/i6X/hG9O/6GC0/Jf8A4uucxS4pWfctQqfz/gjov+Ec07/oYLT8" +
            "l/8Ai6X/AIRzTv8AoYLX8l/+LrncUuKVn3LVOp/P+COi/wCEc07/AKD9r+S//F0v/CO6d/0H7X8l/wDi653FLilr3KVOr/z8/BHQ" +
            "/wDCO6f/ANB61/Jf/iqX/hHdP/6D1r+S/wDxVc9ilxS17lqnV/5+P7l/kdD/AMI9p/8A0HrX8l/+Kqvquhx2FhHdw3q3SPJsBVcD" +
            "oec5PpWPiuivB/xRVh/13P8AN6ltrqKXtaco3ndN22RzuKXFOxS4qrnopnL4pcU7FLivSufnaZ1ng8f8SDxL/wBev/sklcliuv8A" +
            "CA/4kPiT/r2/9lkrk8VjF+/L+uhrfRCYpcUuKXFaXGmJilxS4pcUrmiYmKXFOxS4pXLTG4pcU7FLilc0TG4pcU7FLipuWmNxTsUu" +
            "KXFK5aYmKXFLilxSuaJiYpcUuKdilctMbilxTsUuKVzRMbilxTsUuKVy0xuKXFOxS4pXLTG4p2KXFLipuaJiYpcUuKXFK5aYmKXF" +
            "OxS4pXNExuKXFOxS4pXLTG4pcU7FLilctMbiuhux/wAUXYf9dz/N6wcV0F2P+KNsf+ux/m9S2ZV3rD/F+jOfxS4pcUuKLnYmcxil" +
            "xTsUuK9K5+dpnVeER/xIfEn/AF7f+yyVymK6zwkP+JF4j/69v/ZZK5bFZRfvSNm9ENxS4p2KXFXcaY3FOxS4pcUrlpiYpcUuKXFK" +
            "5aYmKXFLinYpXNExuKXFOxS4qblpjcUuKdilxSuaJjcUuKdilxSuWmNxTsUuKXFK5aYmKXFLilxSuaJiYpcU7FLilctMbilxTsUu" +
            "KVzRMbilxTsUuKm5aY3FLinYpcUrlpiYpcUuKXFK5omJilxS4pcUrlpiYpcU7FLilc0TG4rfuh/xR9j/ANdj/N6w8Vu3Q/4pGy/6" +
            "7H+b1LZlWesPX9GYOKXFOxS4pXOtM5jFLilxS4r07n52mdR4TH/Ej8Rf9e3/ALLJXL4rqvCg/wCJJ4h/69v/AGV65fFZxfvSN2/d" +
            "QmKXFOxS4qrgmNxS4p2KXFK5aY3FLinYpcUrmiY3FOxS4pcUrlpiYpcUuKXFTctMTFLilxTsUrmiY3FLinYpcUrlpjcUuKdilxSu" +
            "aJjcUuKdilxSuWmJilxS4pcUrmiYmKXFLilxSuWmJilxTsUuKm5aY3FLinYpcUrmiY3FLinYpcUrlpjcU7FLilxSuWmJilxS4pcU" +
            "rmiYmK3Lof8AFJWX/XY/zesXFblyP+KUs/8Arsf5vSuZ1nrD1/RmHilxTsUuKm51pnL4pcU7FLivTufnSZ03hUf8STxB/wBe/wD7" +
            "K9cziun8Lj/iS6//ANe//sr1zWKzT95nQ37sRMUuKXFLiquJMTFLinYpcUrlpjcUuKdilxSuaJjcUuKdilxSuWmNxS4p2KXFTc0T" +
            "ExS4pcUuKVy0xMUuKXFLilctMTFLinYpcUrmiY3FLinYpcUrlpjcUuKdilxSuaJjcU7FLilxSuWmJilxS4pcVNy0xMUuKXFOxSua" +
            "JjcUuKdilxSuWmNxS4p2KXFK5omNxS4p2KXFK5aYmK2rkf8AFLWf/XU/zesfFbNwP+KYtP8Arqf5tSuZ1XrD1/RmNilxS4pcUrnW" +
            "mcxilxTsUuK9K5+dJnR+GB/xJte/69//AGV65vFdN4ZH/Em17/r3/wDZXrnMVCerOmT9yP8AXUbilxTsUuKdyUxuKdilxS4pXNEx" +
            "MUuKXFLilctMTFLinYpcUrlpjcUuKdilxU3NExuKXFOxS4pXLTG4pcU7FLilc0TExS4pcUuKVy0xMUuKXFOxSuWmNxS4p2KXFK5o" +
            "mNxS4p2KXFK5aY3FLinYpcVNzRMbinYpcUuKVy0xMUuKXFLilc0TExS4p2KXFK5aY3FLinYpcUrlpjcVsXA/4pm0/wCup/m1ZWK1" +
            "7gf8U3a/9dT/ADahMiq9Y+v+Zj4pcU7FLipudSZzGKXFLilxXpXPztM6Lw0P+JPrv/XD/wBleudxXSeGx/xKNc/64f8Asr1z2KhP" +
            "VnTJ+5H5/mNxVue0jhEiGfM8X30K4Gc4IBzyR9PWq+Ktz3Mc3mP5GJpcb3LZGepIGOCfr60Nii1Yku9M+zTXKiXekKblbbjf84Qj" +
            "rxgk/lUF1bLAISkolWVN+QuB94jH6VZk1Eyx3qNF/wAfDllO7/V5YMR05+6KrSy+ZHAu3Hkps69fmZv/AGaldmrcehZfTVSdYTMR" +
            "Ix2jcmAxxwQc8gnjPuKVdMJt5XMuJo1UmIr3IY4z67Vz+lRzXSvB5UUbRqX34L7gp54Xjgc+/QVI9+5lnlRdry3AnBznbjdx7/e/" +
            "SldlpxEjsYjOYpJnU+UJQVjB48veR1H0pFsle4tI0lO25xhiuCuXK9M+2adJeK9686Q7FMRiCbs4Hl7Ov60+DUZoGtNjOEgxlA5A" +
            "f5i39cUrlpoha3hjt4XeV98qFwojBA+Yjrn29Km+xQlY9k8hMkTygNEBwu7j73+z+tIbvdZR25Ew2KV+WXCn5iclce/r2pEutvlf" +
            "Jny4Xi69d27n/wAe/SlctNDZrWOISIZv30f3kK4GehAOeSPp60ptY1QB5tspTeFK8YxkDOepHt3FOnuI5vMfycTSffYtkZ6kgY4z" +
            "9fWla4jdAXh3ShNm4txjGAcY6ge9K5SaFhsPMW2PmYM0gQjb9zJIB9+hpTYbY7tzJxbkY4+/kgZ9uoP41LFqLpKpKs0SFCkZfhdp" +
            "H+B/OmtelrcxeXjMYQnPU5Xn8kApXNE0I1lCLyS3E8hMe/c3lj+EE8fN7UqWAkkULKdrx71JXBJJ2gEZ9f0qWTUS9y06iYMd+A02" +
            "Qu4EcccYz+lMN/LsXYzpNxvkD8tjOP5/pSuWmUcU7FSSsJJndV2BmJC56e1NxSuWmJilxS4pcUrlpiYpcUuKdipuWmNxS4p2KXFK" +
            "5omNxS4p2KXFK5aY3FLinYpcUrmiYmKXFLilxSuWmJilxS4pcUrlpiYrWnH/ABTtr/10P/s1ZmK1Zx/xT9t/10P/ALNQnuTUesfU" +
            "ycUuKdilxUXOlM5fFLinYpcV6dz87TN/w4P+JTrf/XD/ANlesDFdD4dH/Ep1r/rh/wCyvWBipvqzqm/ch8/zExS4pcU7FFyExuKX" +
            "FOxS4pXLTG4pcU7FLilctMbilxTsUuKVzRMbinYpcUuKm5aYmKXFLilxSuaJiYpcU7FLilctMbilxTsUuKVy0xuKXFOxS4pXNExu" +
            "KXFOxS4pXLTExS4pcUuKm5omJilxS4pcUrlpiYpcU7FLilctMbilxTsUuKVzRMbilxTsUuKVy0xuKXFOxS4pXNExMVpzD/iQW3/X" +
            "Q/8As1Z2K05h/wASK3/66H/2anF7kzesfUzMUuKXFOxWdzoTOXxS4p2KXFenc/O0zd8PD/iVaz/1w/8AZXrBxXQeHx/xK9Y/64/+" +
            "yvWFipudU3+7h8/zG4p2KXFLii5CYmKXFLilxSuWmJilxS4p2KVzRMbilxTsUuKVy0xuKXFOxS4qblpjcUuKdilxSuaJjcU7FLil" +
            "xSuWmJilxS4pcUrmiYmKXFOxS4pXLTG4pcU7FLilc0TG4pcU7FLilctMbilxTsUuKm5aYmKXFLilxSuaJiYpcUuKdilctMbilxTs" +
            "UuKVy0xuKXFOxS4pXNExuK0ph/xJLf8A66H/ANmqhitGUf8AEmg/3z/7NVRe4pv4fUzcU7FLilxWVzoTOYxS4pcU7Fenc/Okzb0E" +
            "f8SzV/8Arj/7K1YeK3dCH/Es1f8A64/+ytWLilc66j/dw+f5jcUuKdilxSuZpjcUuKdilxSuWmJilxS4pcUrmiYmKXFLilxSuWmJ" +
            "ilxTsUuKm5omNxS4p2KXFK5aY3FLinYpcUrlpjcUuKdilxSuaJiYpcUuKXFK5aYmKXFLinYpXNExuKXFOxS4pXLTG4pcU7FLipuW" +
            "mNxS4p2KXFK5omNxTsUuKXFK5aYmKXFLilxSuaJiYpcUuKdilctMbitCUf8AEng/3z/WqWKvSj/iUw/7/wDjVQe/oEnt6lDFLinY" +
            "pcVnc3TOXxTsUuKXFelc/OkzZ0Mf8S3Vv+uP9GrGxW3og/4l2q/9cv6NWNik2dlR/uqfz/MTFLinYpcUrmSY3FLinYpcUrmiY3FL" +
            "inYpcUrlpjcUuKdilxSuWmJilxS4pcVNzRMTFLilxTsUrlpjcUuKdilxSuaJjcUuKdilxSuWmNxS4p2KXFK5aY3FOxS4pcUrmiYm" +
            "KXFLilxU3LTExS4pcU7FK5omNxS4p2KXFK5aY3FLinYpcUrmiY3FLinYpcUrlpiYpcUuKXFK5aYmKuyD/iVw/wC//jVTFXZB/wAS" +
            "yL/e/wAauD39Bt7FLFLinYpcVjc2TOXxS4p2KXFenc/O0zX0Uf8AEu1T/rl/Rqx8Vs6MP+Jfqf8A1y/o1ZOKGzsqP91T9H+YmKXF" +
            "LilxU3MUxMUuKXFOxSuaJjcUuKdilxSuWmNxS4p2KXFK5omNxS4p2KXFTctMbinYpcUuKVzRMTFLilxS4pXLTExS4pcU7FK5aY3F" +
            "LinYpcUrmiY3FLinYpcUrlpjcUuKdilxU3LTExS4pcUuKVzRMTFLilxS4pXLTExS4p2KXFK5omNxS4p2KXFK5aY3FLinYpcUrlpj" +
            "cVccf8S6L/e/xqtirTj/AECP/e/xq6b0l6FN7FXFLilxS4rG5qmcxilxTsUuK9O5+dpmpo4/0DUv+uX9GrKxWlpd3Bax3CTo7rKA" +
            "ML6c57+9Tedo/wDz6Tfmf/iqe6PR5Y1KUFzJNX39fQyMUuK1vO0j/n1m/M//ABVL52kf8+s35n/4qlbzEqC/nj+P+Rk4p2K1fO0n" +
            "/n1m/M//ABVL52k/8+s35/8A2VK3mWqK/nX4/wCRlYpcVqebpX/PrL+f/wBlS+bpX/PtL+f/ANlSt5lKiv51+P8AkZeKXFanm6X/" +
            "AM+0v5//AGVHm6X/AM+0v5//AF6VvMtUl/Ovx/yMzFLitPzdL/59pfz/APr0vm6Z/wA+0v5//XpcvmWqS/mX9fIzMUuK0vN0z/n3" +
            "l/P/AOvS+Zpv/PvL+f8A9ely+Zapr+Zf18jNxS4rS8zTf+feX8//AK9Hmad/z7yfn/8AXpcvmilTX8yM7FOxWh5mnf8APvJ+f/16" +
            "XzNP/wCeEn5//Xpcvmi1BfzIz8UuKv8Amaf/AM8JPz/+vS+ZYf8APCT8/wD69Ll80Wof3kUMUuKv77D/AJ4Sfn/9ejfY/wDPCT8/" +
            "/r0uXzRaj5oo4pcVe32P/PGT8/8A69Lvsf8Ani/5/wD16XJ5opLzKOKXFXd9l/zxf8//AK9Lvsv+eL/n/wDXpci/mRaXmUsUuKu7" +
            "7P8A54v+f/16XfZ/88n/AD/+vRyL+ZFr1KeKXFW91p/zyf8AP/69LutP+eT/AJ//AF6XIv5kWipilxVvda/88n/P/wCvRutf+eTf" +
            "n/8AXpci/mRaZVxS4q1utv8Anm35/wD16Xdbf882/wA/jS9mv5kUmVcVZcf6FH/vf40u63/55t/n8aJJEaIIgIAOeaaSinqtir3K" +
            "+KXFOxS4rmubJnMYpcUuKXFenc/OkxMUuKXFOxSuWmNxS4p2KXFK5omNxS4p2KXFK5aY3FLinYpcUrmiY3FOxS4pcUrlpiYpcUuK" +
            "XFTc0TExS4p2KXFK5aY3FLinYpcUrlpjcUuKdilxSuaJjcUuKdilxSuWmJilxS4pcUrmiYmKXFLinYqblpjcUuKdilxSuWmNxS4p" +
            "2KXFK5omNxS4p2KXFK5aY3FOxS4pcUrlpiYpcUuKXFK5omJilxTsUuKVy0xuKXFOxS4qbmiZ/9k=";

    private static byte[] syntheticJpeg;

    /** Decoded lazily (not in a static initializer) because Base64.decode reads
     *  Display state, so it must run after Display.init -- which createCameraImpl
     *  and every caller here already guarantee. */
    private static synchronized byte[] syntheticJpeg() {
        if (syntheticJpeg == null) {
            try {
                syntheticJpeg = Base64.decode(SYNTHETIC_JPEG_B64.getBytes());
            } catch (Throwable t) {
                // Defensive: a non-empty buffer keeps getJpegBytes() non-null.
                byte[] fallback = new byte[512];
                fallback[0] = (byte) 0xFF;
                fallback[1] = (byte) 0xD8; // JPEG SOI marker
                syntheticJpeg = fallback;
            }
        }
        return syntheticJpeg;
    }

    private volatile FrameListener frameListener;
    private volatile int maxFps = 15;
    private volatile boolean running;
    private volatile boolean closed;
    private Thread frameThread;
    private long frameSeq;
    private int photoCounter;

    @Override
    public CameraInfo[] enumerateCameras() {
        Dimension[] sizes = new Dimension[] { new Dimension(FRAME_W, FRAME_H) };
        return new CameraInfo[] {
            new CameraInfo("win-synthetic-back", CameraFacing.BACK, sizes, sizes, false, true),
            new CameraInfo("win-synthetic-front", CameraFacing.FRONT, sizes, sizes, false, true)
        };
    }

    @Override
    public void open(String cameraId, CameraSessionOptions opts) throws IOException {
        closed = false;
    }

    @Override
    public PeerComponent createPreviewPeer() {
        // No live preview surface yet; a non-null, never-rendered placeholder peer
        // satisfies the API (callers add it to a form only when previewing).
        return new PeerComponent(null) {
        };
    }

    @Override
    public void takePhoto(PhotoCaptureOptions opts, final AsyncResource<CapturedPhoto> result) {
        final byte[] jpeg = syntheticJpeg();
        String path;
        try {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            path = fs.getAppHomePath() + "cn1-photo-" + (++photoCounter) + ".jpg";
            OutputStream os = fs.openOutputStream(path);
            try {
                os.write(jpeg);
            } finally {
                os.close();
            }
        } catch (Throwable t) {
            path = "cn1-synthetic-photo-" + photoCounter + ".jpg";
        }
        final String filePath = path;
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                result.complete(new CapturedPhoto(jpeg, filePath, FRAME_W, FRAME_H));
            }
        });
    }

    @Override
    public void startVideoRecording(String filePath, boolean audio) throws IOException {
        throw new IOException("Video recording is not supported by the synthetic Windows camera");
    }

    @Override
    public void stopVideoRecording(AsyncResource<String> result) {
        if (result != null) {
            result.error(new IllegalStateException("No video recording in progress"));
        }
    }

    @Override
    public synchronized void setFrameListener(FrameListener listener, FrameFormat format, int fps) {
        this.frameListener = listener;
        this.maxFps = Math.max(1, fps);
        if (listener != null) {
            startFrameLoop();
        } else {
            stopFrameLoop();
        }
    }

    private synchronized void startFrameLoop() {
        if (running || closed) {
            return;
        }
        running = true;
        // Note: Thread.setDaemon is absent from the ParparVM clean target's minimal
        // java.lang.Thread, so the loop must terminate on its own -- close()/pause()
        // clear `running` and the headless capture force-exits the process anyway.
        frameThread = new Thread(new Runnable() {
            public void run() {
                frameLoop();
            }
        }, "cn1-win-camera-frames");
        frameThread.start();
    }

    private synchronized void stopFrameLoop() {
        running = false;
    }

    private void frameLoop() {
        while (running && !closed) {
            FrameListener l = frameListener;
            if (l != null) {
                // Synchronous delivery means the next iteration only starts after
                // onFrame returns, so a slow consumer naturally drops frames (the
                // contract's "drop while the previous invocation is still running").
                CameraFrame frame = new CameraFrame(syntheticJpeg(), null,
                        FRAME_W, FRAME_H, 0, (frameSeq++) * 1000000L, FrameFormat.JPEG);
                try {
                    l.onFrame(frame);
                } catch (Throwable ignored) {
                }
            }
            try {
                Thread.sleep(Math.max(1, 1000 / Math.max(1, maxFps)));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public void setFlashMode(FlashMode mode) {
        // Synthetic camera has no flash; no-op.
    }

    @Override
    public void setZoom(float ratio) {
        // No optical/digital zoom on the synthetic frame; no-op.
    }

    @Override
    public void focus(float xNorm, float yNorm) {
        // Always in focus; no-op.
    }

    @Override
    public void pause() {
        stopFrameLoop();
    }

    @Override
    public void resume() {
        if (!closed && frameListener != null) {
            startFrameLoop();
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        running = false;
        frameListener = null;
    }
}
