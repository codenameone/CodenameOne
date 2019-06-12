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
package com.codename1.media;

import com.codename1.ui.CN;

/**
 *
 * @author shannah
 * @deprecated for internal use only.
 */
class RemoteControlCallback {
    private static final RemoteControlListener nullListener = new RemoteControlListener();
    private static RemoteControlListener l() {
        RemoteControlListener out = MediaManager.getRemoteControlListener();
        if (out == null) {
            out = nullListener;
        }
        return out;
    }
    
    static void play() {
        CN.callSerially(new Runnable() {
            public void run() {
                l().play();
            }
        });
        
    }
    public static void pause() {
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                l().pause();
            }
           
        });
        
    }
    
    public static void togglePlayPause() {
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                l().togglePlayPause();
            }
           
        });
        
    }
    
    public static void seekTo(final long pos) {
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                l().seekTo(pos);
            }
            
        });
        
    }
    
    public static void skipToNext() {
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                l().skipToNext();
            }
            
        });
    }
    
    public static void skipToPrevious() {
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                l().skipToPrevious();
            }
            
        });
    }
    
    public static void stop() {
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                l().stop();
            }
            
        });
    }
    
    public static void fastForward() {
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                l().fastForward();
            }
            
        });
    }
    
    public static void rewind() {
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                l().rewind();
            }
            
        });
    }
    
    public static boolean isPlaying() {
        return l().isPlaying();
    }
    
    public static void setVolume(final float leftVolume, final float rightVolume) {
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                l().setVolume(leftVolume, rightVolume);
            }
            
        });
        
    }
    
    public static MediaMetaData getMetaData() {
        return l().getMetaData();
    }
}
