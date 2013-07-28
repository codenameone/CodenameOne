/*
 * Copyright (c) 2012, Steve Hannah/Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.javascript;

/**
* <p>The JSFunction interface allows you to implement functions in Java that can
* be called from Javascript. You can assign any JSFunction object to be a member
* method of an existing JSObject via the {@link JSObject#set(String,Object) JSObject.set()} method. Then the function
* can be called from javascript just like any other Javascript method. JSFunction
* methods are called asynchronously from Javascript to prevent deadlocks. If you
* require a return value to Javascript, you can do that by passing a callback
* function which is called by the JSFunction with some parameters.</p>
*
* <p>The following example, adds a camera object to the Javascript environment
* that has a capture() method, which can be used to capture images using the
* device's camera:</p>
*
* <code><pre>
* // Create a new Javascript object "camera"
* final JSObject camera = (JSObject)ctx.get("{}");
*
* // Create a capture() method on the camera object
* // as a JSFunction callback.
* camera.set("capture", new JSFunction(){
*
* public void apply(JSObject self, final Object[] args) {
* Display.getInstance().capturePhoto(new ActionListener(){
*
* public void actionPerformed(ActionEvent evt) {
*
* String imagePath = (String)evt.getSource();
*
* // Get the callback function that was provided
* // from javascript
* JSObject callback = (JSObject)args[0];
*
* ctx.call(
* callback, // The function
* camera, // The "this" object
* new Object[]{"file://"+imagePath} // Parameters
* );
* }
*
* });
* }
*
* });
*
*
* // Add the camera object to the top-level window object
* ctx.set("window.camera", camera);
*
*
* </pre></code>
* <p>We can then capture photos directly from Javascript using a function similar to the following:</p>
* <code><pre>
* camera.capture(function(url){
* if ( url == null ){
* // No image was captured
* return;
* }
*
* // Fetch the preview &lt;img&gt; tag.
* var image = document.getElementById('preview-image');
* // Set the preview URL to the image that was taken.
* image.src = url;
* });
* </pre></code>
* @author shannah
*/
public interface JSFunction {
    public void apply(JSObject self, Object[] args);
}