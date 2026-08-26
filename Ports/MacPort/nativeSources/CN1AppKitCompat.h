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
#ifndef CN1AppKitCompat_h
#define CN1AppKitCompat_h
#import <TargetConditionals.h>
#if TARGET_OS_OSX

#import <AppKit/AppKit.h>
#import <CoreGraphics/CoreGraphics.h>

/// The AppKit side of the types the shared Apple drawing stack is written
/// against.
///
/// The measurement that justified sharing the Apple native layer counted files:
/// of the Objective-C behind IOSNative, roughly six lines in seven mention no
/// UIKit at all. That is true and it is not the whole story, because the
/// coupling that matters is in the *headers*. CN1Metalcompat.h declares
/// CN1MetalDrawString taking a UIFont and CN1MetalTextureFromUIImage taking a
/// UIImage, and GLUIImage is a UIImage wrapper -- so the drawing stack is
/// UIKit-typed at its interface even where its implementation is pure Metal and
/// CoreGraphics.
///
/// Bridging that is deliberately NOT a blanket typedef of UIImage to NSImage.
/// The two are not the same idea: NSImage is a resolution-independent container
/// of representations, and asking one for "its" bitmap is a question with more
/// than one answer. Every site is converted individually, and the currency
/// between them is CGImageRef, which is identical on both platforms.
///
/// This header holds the pieces of that bridge that are genuinely mechanical.
/// The rest is per-call-site work in the port.

/// The backing scale of the screen a view is on, or of the main screen when it
/// is not yet in a window. The AppKit answer to UIScreen.scale, except that it
/// is per-display rather than per-device: dragging a window between a Retina
/// and a non-Retina display changes it, which is why callers re-ask on
/// viewDidChangeBackingProperties rather than caching it once at startup.
CGFloat CN1AppKitBackingScale(NSView * _Nullable view);

/// A CGImage from an NSImage, at the image's own pixel size.
///
/// Goes through CGImageForProposedRect: rather than any of the shorter routes,
/// because that is the call that lets the image choose a representation for a
/// known size instead of guessing. Returns NULL rather than an empty image when
/// there is no usable representation, so callers can tell the difference.
CGImageRef _Nullable CN1AppKitCGImageFromNSImage(NSImage * _Nullable image);

/// An NSImage wrapping a CGImage at its pixel size, tagged so that AppKit does
/// not rescale it for the current display.
///
/// Autoreleased -- the caller does not own it. Same for every function here
/// that returns an NSImage.
NSImage * _Nullable CN1AppKitNSImageFromCGImage(CGImageRef _Nullable cgImage);

/// Reads a CGImage into a caller-supplied ARGB buffer, premultiplied, in the
/// byte order Codename One's int[] pixels use. Returns NO and leaves the buffer
/// untouched when the image cannot be drawn into that layout.
BOOL CN1AppKitReadARGB(CGImageRef _Nullable cgImage, unsigned int * _Nonnull argb,
                       int width, int height);

/// Builds an image from a Codename One ARGB raster. The framework hands out
/// pixels rather than platform images, so this is the one direction that has no
/// CGImage to start from.
NSImage * _Nullable CN1AppKitNSImageFromARGB(const unsigned int * _Nonnull argb,
                                             int width, int height);

#endif
#endif
