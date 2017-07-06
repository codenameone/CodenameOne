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


import com.codename1.util.Callback;
import com.codename1.util.CallbackAdapter;
import com.codename1.util.StringUtil;
import com.codename1.util.SuccessCallback;

/**
 * A Java Wrapper around a Javascript object. In Javascript there are only a few
 * different types:  Number, String, Boolean, Null, Undefined, and Object.
 * 
 * Arrays and functions are objects also.
 * 
 * <p>A JSObject is associated with a particular {@link JavascriptContext} and it is backed
 * by a Javascript object inside the javascript context.  This object acts as a 
 * proxy to call methods and access properties of the actual javascript object.</p>
 * 
 * <p>All return values for Javascript calls in the {@link JavascriptContext} will be 
 * converted to the appropriate Java type.  Javascript objects will automatically
 * be wrapped in a JSObject proxy.</p>
 * 
 * <h5>Getting and Setting Properties</h5>
 * 
 * <p>JSObject provides {@link #get(String)} and {@link #set(String,Object)} methods to get and set properties on the
 * object. E.g.</p>
 * <code><pre>
 * obj.set("name", "Steve");
 * obj.set("age", 12);
 * String name = (String)obj.get("name");  // Steve
 * Double age = (Double)obj.get("age");    // 12.0
 * </pre></code>
 * 
 * <h5>Typed Getters</h5>
 * <p>The return value of {@link #get(String)} will be one of Double, String, Boolean, or JSObject
 * depending on the type of Javascript value that is being returned.   {@link #get(String)} requires
 * you to cast the return value to the correct type, which is a bit a pain.  Luckily,
 * JSObject provides a set of typed getter methods that will automatically cast to 
 * particular types:</p>
 * 
 * <table border="1"><tbody>
 *  <tr>
 *      <td>getInt()</td><td>Returns int</td>
 *  </tr>
 *  <tr>
 *      <td>getString()</td><td>Returns String</td>
 *  </tr>
 *  <tr>
 *      <td>getDouble()</td><td>Returns double</td>
 *  </tr>
 *  <tr>
 *      <td>getObject()</td><td>Returns JSObject</td>
 *  </tr>
 *  <tr>
 *      <td>getBoolean()</td><td>Returns boolean</td>
 * </tr>
 * </tbody>
 * </table>
 * 
 * <h5>Indexed Properties</h5>
 * <p>JSObject can wrap Javascript arrays also.  You can retrieve the indexed
 * properties of the array using indexed versions of {@link #get(int)} and {@link #set(int,Object)} (i.e. they
 * take an int as their first parameter instead of a String).  There are also
 * typed versions of the indexed {@link #get(int)} method to allow directly returning 
 * values of the correct type without having to type cast</p>
 * 
 * <h6>Example, looping through array</h6>
 * <code><pre>
 * JSObject colors = ctx.get("['red','green','blue']");
 * int len = colors.getInt("length");
 * for ( int i=0; i&lt; len; i++ ){
 *     System.out.println("Color "+i+" is "+colors.getString(i));
 * }
 * </pre></code>
 * 
 * <h5>Calling Object Methods</h5>
 * 
 * <p>JSObject allows you to call Javascript object methods on the wrapped
 * Javascript object via the its {@link #call(String,Object[])} method.  It takes the name of the 
 * method (i.e. property name that stores the function), and an array of 
 * parameters to pass to the method.</p>
 * 
 * <code><pre>
 * JSObject document = (JSObject)context.get("document");
 * 
 * // Call document.writeln("Hello world");
 * document.call("writeln", new Object[]{"Hello world"});
 * </pre></code>
 * 
 * <h5>Calling Wrapped Functions</h5>
 * 
 * <p>Since, in Javascript, functions are objects, it is possible to wrap a function
 * with a JSObject object also.  You can then call the function using the alternate
 * version of the {@link #call(JSObject,Object[])} method.</p>
 * 
 * <code><pre>
 * JSObject window = (JSObject)context.get("window");
 *     // reference to the window object so that we can pass it as the context
 *     // of the function call.
 * JSObject myfunc = (JSObject)context.get("function(a,b){ return a+b}");
 * Double result = (Double)myfunc.call(window, new Object[]{new Integer(1), new Integer(2)});
 * </pre></code>
 * 
 * <h5>Calling Java Methods from Javascript</h5>
 * 
 * <p>The {@link JSFunction} interface allows you to implement functions in Java that can
 * be called from Javascript.  You can assign any {@link JSFunction} object to be a member
 * method of an existing JSObject via the {@link #set(String,Object) JSObject.set()} method.  Then the function
 * can be called from javascript just like any other Javascript method.  {@link JSFunction}
 * methods are called asynchronously from Javascript to prevent deadlocks.  If you
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
 *     public void apply(JSObject self, final Object[] args) {
 *         Display.getInstance().capturePhoto(new ActionListener(){
 * 
 *             public void actionPerformed(ActionEvent evt) {
 *                 
 *                 String imagePath = (String)evt.getSource();
 *                 
 *                 // Get the callback function that was provided
 *                 // from javascript
 *                 JSObject callback = (JSObject)args[0];
 *                 
 *                 ctx.call(
 *                         callback, // The function
 *                         camera,   // The "this" object
 *                         new Object[]{"file://"+imagePath}  // Parameters
 *                 );
 *             }
 *             
 *         });
 *     }
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
 *     if ( url == null ){
 *         // No image was captured
 *         return;
 *     }
 * 
 *     // Fetch the preview &lt;img&gt; tag.
 *     var image = document.getElementById('preview-image');
 *     // Set the preview URL to the image that was taken.
 *     image.src = url;
 * });
 * </pre></code>
 * 
 * @author shannah
 */
public class JSObject {
    
    /**
     * The Javascript context that this object belongs to.
     */
    private JavascriptContext context;
    
    /**
     * The ID of this object.  This is the ID within the Javascript lookup table
     * that stores a reference to the actual Javascript object.
     */
    int objectId = 0;
    
    /**
     * Javascript register variable 1.
     */
    private static final String R1 = "ca_weblite_codename1_js_JSObject_R1";
    
    /**
     * Javascript register variable 2.
     */
    private static final String R2 = "ca_weblite_codename1_js_JSObject_R2";
    
    /**
     * The key, within a Javascript object that stores the ID of an object. 
     * When a JSObject is initially created for a javascript object, an ID 
     * is generated and used to store a reference to the javascript object
     * in the Javascript lookup table - and this ID is stored in the object
     * itself.  This "ID_KEY" variable is the name of the property that stores
     * this ID within the object.
     */
    static final String ID_KEY = "ca_weblite_codename1_js_JSObject_ID";
    static final String PROP_REFCOUNT = "ca_weblite_codename1_js_JSObject_REFCOUNT"; 
    
    /**
     * Constructor for a JSObject.
     * 
     * <h5>Example</h5>
     * 
     * <code><pre>
     * // Create a JavascriptContext for a browser component
     * JavascriptContext ctx = new JavascriptContext(browserComponent);
     * 
     * // Get reference to the window object
     * JSObject window = new JSObject(ctx, "window");
     * 
     * // This is equivalent to
     * window = (JSObject)ctx.get("window");
     * 
     * </pre></code>
     * @param context The javascript context in which this object is being created.
     * 
     * @param expr A javascript expression that resolves to an Javascript Object.
     */
    public JSObject(JavascriptContext context, String expr) {
        this.context = context;
        synchronized (context.browser){
            String escaped = StringUtil.replaceAll(expr, "\\", "\\\\");
            escaped = StringUtil.replaceAll(escaped, "'", "\\'");
            exec(R1+"=eval('"+escaped+"')");
            String type = exec("typeof("+R1+")");
            if ( !"object".equals(type) && !"function".equals(type)){
                throw new JSException("Attempt to create JSObject for expression "+expr+" which does not evaluate to an object.");
            }
            
            String lt = context.jsLookupTable;
            String js = "var id = "+R1+"."+ID_KEY+"; "+
                        "if (typeof(id)=='undefined' || typeof("+lt+"[id]) == 'undefined' || "+lt+"[id]."+ID_KEY+"!=id){"+
                            lt+".push("+R1+"); id="+lt+".indexOf("+R1+"); Object.defineProperty("+R1+",\""+ID_KEY+"\",{value:id, enumerable:false});"+
                            "Object.defineProperty("+R1+",\""+PROP_REFCOUNT+"\",{value:1, enumerable:false});"+
                        "} else { "+
                            R1+"."+PROP_REFCOUNT+"++;"+
                        "} id";
            
           
            String id = exec(js);
            this.objectId = Integer.parseInt(id);
            context.retain(this); 
        }
    }
    
    /**
     * Returns a member variable of the Javascript object.  
     * 
     * <p>E.g., suppose you have a Javascript object myCar whose JSON representation
     * is </p>
     * <code>
     * { make : 'Ford', model : 'Escort', 'year' : 1989}
     * </code>
     * 
     * <p>Then the JSObject proxy for this object could call:</p>
     * <code>
     * String model = (String)myCar.get("model");
     * </code>
     * 
     * <p>And this would return "Ford".</p>
     * 
     * <h5>Example</h5>
     * 
     * <code><pre>
     * JSObject document = (JSObject)ctx.get("document");
     * 
     * // Get document title
     * String title = document.get("title");
     * 
     * // Since the "title" property is a string, get() returns a String.
     * // We could equivalently use
     * 
     * title = document.getString("title");
     * 
     * // Get a grandchild property
     * Double titleLength = (Double)document.get("title.length");
     * 
     * // Since length is an integer, it is probably easier to use getInt()
     * int titleLengthInt = document.getInt("title.length");
     * 
     * // Get a child object
     * JSObject body = (JSObject)document.get("body");
     * 
     * // Since "body" is a Javascript object, it is probably easier to use
     * // getObject()
     * JSObject body2 = document.getObject("body");
     * 
     * // Get a method as a function object
     * JSObject open = (JSObject) document.get("open");
     * 
     * // Call the open() method, with document as "this"
     * open.call(document, new Object[]{}); // Takes no parameters
     * 
     * // Equivalently we could have called open via the document object
     * document.call("open", new Object[]{});
     * 
     * </code></pre>
     * 
     * @param key The name of the property to retrieve on this object.
     * @return The value of the property specified converted to the appropriate 
     * Java value.  See @ref JavascriptContext.get() for a table of the Javascript
     * to Java type conversions.
     */
    public Object get(String key){
        if ( key.indexOf("'") == 0 ){
            return context.get(toJSPointer()+"["+key+"]");
        } else {
            return context.get(toJSPointer()+"."+key);
        }
    }
    
    
    /**
     * Wrapper around get() to return a String.
     * @param key The name of the property in the object to retrieve.  Value of this
     * property must be a string.
     * @return The property value as a string.
     */
    public String getString(String key){
        return (String)get(key);
    }
    
    /**
     * Wrapper around get() to return an int
     * @param key The name of the property in the object to retrieve.  Value of this
     * property must be an integer.
     * @return  The property value as an integer.
     */
    public int getInt(String key){
        return ((Double)get(key)).intValue();
    }
    
    /**
     * Wrapper around get() to return a double.
     * 
     * @param key The name of the property in the object to retrieve. Value of this property
     * must be a number.
     * @return The property value as a double.
     */
    public double getDouble(String key){
        return ((Double)get(key)).doubleValue();
    }
    
    /**
     * Wrapper around get() to return a boolean.
     * @param key The name of the property in the object to retrieve.  Value of this property
     * must be a boolean.
     * @return The property value as a boolean.
     */
    public boolean getBoolean(String key){
        return ((Boolean)get(key)).booleanValue();
    }
    
    /**
     * Wrapper around the get() method to return a JSObject.
     * @param key The name of the property in the object to retrieve.  Value of this property
     * must be an Javascript object or function.
     * @return The property value as a JSObject.
     */
    public JSObject getObject(String key){
        return (JSObject)get(key);
    }
    
    
    /**
     * This method is useful only for JSObjects that encapsulate Javascript arrays.  It
     * provides a method to get indexed properties of the array.  E.g. to retrieve the 
     * 5th element of the wrapped array, you could use this method.
     * 
     * <h5>Example</h5>
     * <code><pre>
     * JSObject colors = ctx.get("['red','green','blue']");
     * String red = (String)colors.get(0);
     * String green = (String)colors.get(1);
     * String blue = (String)colors.get(2);
     * </code></pre>
     * 
     * <p>It may be more convenient to use the typed wrapper methods so that you 
     * don't have to typecast the values. E.g.</p>
     * 
     * <code><pre>
     * String red = colors.getString(0);
     * String green = colors.getString(1);
     * String blue = colors.getString(2);
     * </code></pre>
     * 
     * <h5>Example, looping through array</h5>
     * <code><pre>
     * JSObject colors = ctx.get("['red','green','blue']");
     * int len = colors.getInt("length");
     * for ( int i=0; i&lt; len; i++ ){
     *     System.out.println("Color "+i+" is "+colors.getString(i));
     * }
     * </pre></code>
     * 
     * @param index The index of the entry within the array to return.
     * @return The value of the specified indexed element.
     */
    public Object get(int index){
        return context.get(toJSPointer()+"["+index+"]");
    }
    
    /**
     * Wrapper for get(int) for indexed string values.
     * @param index The index within an Array object whose value to retrieve.
     * @return The value of the indexed element.  Must be a String.
     */
    public String getString(int index){
        return (String)get(index);
    }
    
    /**
     * Wrapper for get(int) for indexed int values.
     * 
     * @param index The index within the Array object whose value to retrieve.
     * @return The value of the indexed element.  Must be an integer.
     */
    public int getInt(int index){
        return ((Double)get(index)).intValue();
    }
    
    /**
     * Wrapper for get(int) for indexed double values.
     * @param index The index within the Array object whose value to retrieve.
     * @return The value of the indexed element. Must be a number.
     */
    public double getDouble(int index){
        return ((Double)get(index)).doubleValue();
    }
    
    /**
     * Wrapper for get(int) for indexed boolean values.
     * @param index The index within the Array object whose value to retrieve.
     * @return The value of the indexed element. Must be a boolean.
     */
    public boolean getBoolean(int index){
        return ((Boolean)get(index)).booleanValue();
    }
    
    /**
     * Wrapper for get(int) for indexed object values.
     *
     * @param index The index within the Array object whose value to retrieve.
     * @return The value of the indexed element.  Must be an Object. (e.g. can
     * be Object, Function, Array, or any other type of Javascript object).
     */
    public JSObject getObject(int index){
        return (JSObject)get(index);
    }
    
    
    /**
     * Sets a property on the underlying Javascript object. Equivalent of <code>this[key] = js;</code>.
     * @param key The name of the property to set on the current object.
     * @param js The value of the property.  This value should be provided as a 
     * Java value and it will be converted to the appropriate Javascript value.
     * See @ref JavascriptContext.set() for a conversion table of the Java to
     * Javascript type conversions.
     * @param async If this flag is set, then the call will be asynchronous (will not wait for command to complete before continuing execution).
     */
    public void set(String key, Object js, boolean async){
        if ( js instanceof JSFunction ){
            this.addCallback(key, (JSFunction)js, async);
            return;
        }
        if ( key.indexOf("'") == 0 ){
            key  = toJSPointer()+"["+key+"]";
        } else {
            key = toJSPointer()+"."+key;
        }
        
        context.set(key, js);
    }
    
    /**
     * Sets a property on this object.  This call is synchronous. Equivalent of <code>this[key] = value;</code>.
     * @param key The name of the property.
     * @param js A value for the property.  This may be a primitive type, a JSObject, or a JSFunction.
     * @see #set(java.lang.String, java.lang.Object, boolean) 
     */
    public void set(String key, Object js) {
        set(key, js, false);
    }
    
    /**
     * Sets a property on this object to an integer value. Equivalent of <code>this[key] = value;</code>.
     * @param key The property name to set.
     * @param value The value to assign to the property.
     * @param async True if you want this call to be asynchronous.
     */
    public void setInt(String key, int value, boolean async){
        set(key, new Double(value), async);
    }
    
    /**
     * Sets a property on this object to an integer value synchronously. Equivalent of <code>this[key] = value;</code>.
     * @param key The name of the property to set.
     * @param value The integer value of to set.
     */
    public void setInt(String key, int value) {
        set(key, value, false);
    }
    
    /**
     * Sets a property on this object to a double value. Equivalent of <code>this[key] = value;</code>.
     * @param key The name of the property to set
     * @param value The value to set.
     * @param async True if you want this call to be asynchronous.
     */
    public void setDouble(String key, double value, boolean async){
        set(key, new Double(value), async);
    }
    
    /**
     * Sets a property on this object to a double value synchronously. Equivalent of <code>this[key] = value;</code>.
     * @param key The name of the property to set
     * @param value The value to set.
     */
    public void setDouble(String key, double value) {
        set(key, value, false);
    }
    
    /**
     * Sets a property on this object to a boolean value. Equivalent of <code>this[key] = value;</code>.
     * @param key The name of the property to set
     * @param value The value to set.
     * @param async True if you want this call to be asynchronous.
     */
    public void setBoolean(String key, boolean value, boolean async){
        set(key, value ? Boolean.TRUE : Boolean.FALSE);
    }
    
    /**
     * Sets a property on this object to a boolean value synchronously.  Equivalent of <code>this[key] = value;</code>.
     * @param key The name of the property to set
     * @param value The value to set.
     */
    public void setBoolean(String key, boolean value) {
        setBoolean(key, value, false);
    }
    
    /**
     * Sets an indexed value on this object (assuming this object is a Javascript array).  Equivalent of <code>this[index] = js;</code>.
     * @param index The index to set.
     * @param js The object to set.  This may be a primitive type, a String, a JSObject, or a JSFunction.
     * @param async True to make this call asynchronously.
     */
    public void set(int index, Object js, boolean async){
        context.set(toJSPointer()+"["+index+"]", js, async);
    }
    
    /**
     * Sets an indexed value on this object synchronously (assuming this object is a Javascript array).  Equivalent of <code>this[index] = js;</code>.
     * @param index The index to set.
     * @param js The object to set.  This may be a primitive type, a String, a JSObject, or a JSFunction.
     */
    public void set(int index, Object js) {
        set(index, js, false);
    }
    
    /**
     * Sets an indexed value on this array to an integer.  Assumes this object is a Javascript array. Equivalent of <code>this[index] = value;</code>.
     * @param index The index within this array to set.
     * @param value The value to set.
     * @param async True to make this call asynchronous.
     */
    public void setInt(int index, int value, boolean async){
        set(index, new Double(value), async);
    }
    
    /**
     * Sets an indexed value on this array to an integer synchronously.  Assumes this object is a Javascript array. Equivalent of <code>this[index] = value;</code>.
     * @param index The index within this array to set.
     * @param value The value to set.
     */
    public void setInt(int index, int value) {
        set(index, value, false);
    }
    
    /**
     * Sets an indexed value on this array to a double.  Assumes this object is a Javascript array. Equivalent of <code>this[index] = value;</code>.
     * @param index The index within this array to set.
     * @param value The value to set.
     * @param async True to make this call asynchronous.
     */
    public void setDouble(int index, double value, boolean async){
        set(index, new Double(value), async);
    }
    
    /**
     * Sets an indexed value on this array to a double synchronously.  Assumes this object is a Javascript array. Equivalent of <code>this[index] = value;</code>.
     * @param index The index within this array to set.
     * @param value The value to set.
     */
    public void setDouble(int index, double value) {
        set(index, value, false);
    }
    
    /**
     * Sets an indexed value on this array to a boolean.  Assumes this object is a Javascript array. Equivalent of <code>this[index] = value;</code>.
     * @param index The index within this array to set.
     * @param value The value to set.
     * @param async True to make this call asynchronous.
     */
    public void setBoolean(int index, boolean value, boolean async){
        set(index, value ? Boolean.TRUE : Boolean.FALSE, async);
    }
    
    /**
     * Sets an indexed value on this array to a boolean synchronously.  Assumes this object is a Javascript array. Equivalent of <code>this[index] = value;</code>.
     * @param index The index within this array to set.
     * @param value The value to set.
     */
    public void setBoolean(int index, boolean value) {
        setBoolean(index, value, false);
    }

    /**
     * Returns a Javascript variable name for the underlying Javascript object.  This
     * refers to the object inside the JavascriptContext's lookup table.
     * @return 
     */
    public String toJSPointer() {
        return context.jsLookupTable+"["+objectId+"]";
    }
    
    /**
     * Convenience method.  A wrapper around BrowserComponent.executeAndReturnString()
     * @param js
     * @return 
     */
    private String exec(String js, boolean async){
        if (async) {
            context.browser.execute(js);
            return null;
        } else {
            return context.browser.executeAndReturnString(js);
        }
    }
    
    private String exec(String js) {
       return exec(js, false);
    }
    
    /**
     * Registers a {@link JSFunction} with the Javascript context so that it can handle
     * calls from Javascript.  This installs a Javascript proxy method that 
     * sends a message, via the BrowserNavigationCallback mechanism to the
     * JavascriptContext object so that the actual Java code in the JSFunction
     * will be called.
     * @param key The name of the property on the underlying Javascript object
     * where the method proxy will be added in Javascript.
     * @param func A JSFunction callback that will be called when the generated
     * Javascript proxy method is called.
     */
    void addCallback(String key, JSFunction func, boolean async){
        context.addCallback(this, key, func, async);
    }
    
    /**
     * Removes a previously added JSFunction callback from the object.
     * @param key The name of the property on the underlying Javascript object
     * that is to be deleted.
     */
    void removeCallback(String key, boolean async){
        context.removeCallback(this, key, async);
    }
    
    void removeCallback(String key) {
        removeCallback(key, false);
    }
    
    
    /**
     * Calls a method on the underlying Javascript object.
     * @param key The name of the method to call.
     * @param params Array of parameters to pass to the method.  These will be 
     * converted to corresponding Javascript types according to the translation 
     * table specified in {@link JavascriptContext#set(String,Object)}
     * @return The result of calling the method.  Javascript return values will
     * be converted to corresponding Java types according to the rules described
     * in {@link JavascriptContext#get(String)}
     */
    public Object call(String key, Object[] params){
        return context.call(toJSPointer()+"."+key, this, params);
    }
    
    /**
     * Calls a method on the underlying Javascript object asynchronously.
     * @param key The name of the method to call.
     * @param params Array of parameters to pass to the method.  These will be 
     * converted to corresponding Javascript types according to the translation 
     * table specified in {@link JavascriptContext#set(String,Object)}
     * @param callback Callback to be called when the method call is completed.
     */
    public void callAsync(String key, Object[] params, Callback callback) {
        context.callAsync(this, this, params, callback);
    }
    
    /**
     * Calls a method on the underlying Javascript object asynchronously.
     * @param key The name of the method to call.
     * @param params Array of parameters to pass to the method.  These will be 
     * converted to corresponding Javascript types according to the translation 
     * table specified in {@link JavascriptContext#set(String,Object)}
     * @param callback Callback to be called when the method call is completed.
     */
    public void callAsync(String key, Object[] params, final SuccessCallback callback) {
        this.callAsync(key, params, new CallbackAdapter() {

            @Override
            public void onSucess(Object value) {
                callback.onSucess(value);
            }
            
        });
    }
    
    /**
     * Calls a method on the underlying Javascript object synchronously with no arguments.
     * @param key The name of the method.
     * @return The return value of the method.  
     */
    public Object call(String key){
        return call(key, new Object[]{});
    }
    
    /**
     * Calls a method on the underlying Javascript object asynchronously with no arguments.
     * @param key The name of the method.
     * @param callback Callback to be called with the return value.
     */
    public void callAsync(String key, Callback callback) {
        callAsync(key, new Object[]{}, callback);
    }
    
    /**
     * Calls a method on the underlying Javascript object asynchronously with no arguments.
     * @param key The name of the method.
     * @param callback Callback to be called with the return value.
     */
    public void callAsync(String key, final SuccessCallback callback) {
        callAsync(key, new CallbackAdapter() {

            @Override
            public void onSucess(Object value) {
                callback.onSucess(value);
            }
            
        });
    }
    
    /**
     * Calls a method on the underlying Javascript object that returns an int.   Called synchronously with no arguments.
     * @param key The name of the method.
     * @return The return value of the method.
     */
    public int callInt(String key){
        Double d = (Double)call(key);
        return d.intValue();
    }
    
    /**
     * Calls a method on the underlying Javascript object that returns an int.  Call is asynchronous.  Return value or error is passed to the
     * provided callback.
     * @param key The name of the method.
     * @param callback Callback to handle the return value.
     */
    public void callIntAsync(String key, final Callback<Integer> callback) {
        callDoubleAsync(key, new Callback<Double>() {

            public void onSucess(Double value) {
                callback.onSucess(value.intValue());
            }

            public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                callback.onError(sender, err, errorCode, errorMessage);
            }
            
        });
    }
    
    /**
     * Calls a method on the underlying Javascript object that returns an int.  Call is asynchronous.  Return value or error is passed to the
     * provided callback.
     * @param key The name of the method.
     * @param callback Callback to handle the return value.
     */
    public void callIntAsync(String key, final SuccessCallback<Integer> callback) {
        callIntAsync(key, new CallbackAdapter<Integer>() {

            @Override
            public void onSucess(Integer value) {
                callback.onSucess(value);
            }
            
        });
    }
    
    /**
     * Calls a method on the underlying Javascript object that returns a double.   Called synchronously with no arguments.
     * @param key The name of the method.
     * @return The return value of the method.
     */
    public double callDouble(String key){
        Double d = (Double)call(key);
        return d.doubleValue();
    }
    
    /**
     * Calls a method on the underlying Javascript object that returns a double.  Call is asynchronous.  Return value or error is passed to the
     * provided callback.
     * @param key The name of the method.
     * @param callback Callback to handle the return value.
     */
    public void callDoubleAsync(String key, Callback<Double> callback) {
        callAsync(key, callback);
    }
    
    /**
     * Calls a method on the underlying Javascript object that returns a double.  Call is asynchronous.  Return value or error is passed to the
     * provided callback.
     * @param key The name of the method.
     * @param callback Callback to handle the return value.
     */
    public void callDoubleAsync(String key, final SuccessCallback<Double> callback) {
        callDoubleAsync(key, new CallbackAdapter<Double>() {

            @Override
            public void onSucess(Double value) {
                callback.onSucess(value);
            }
            
        });
    }
    
    /**
     * Calls a method on the underlying Javascript object that returns a String.   Called synchronously with no arguments.
     * @param key The name of the method.
     * @return The return value of the method.
     */
    public String callString(String key){
        return (String)call(key);
    }
    
    /**
     * Calls a method on the underlying Javascript object that returns a String.  Call is asynchronous.  Return value or error is passed to the
     * provided callback.
     * @param key The name of the method.
     * @param callback Callback to handle the return value.
     */
    public void callStringAsync(String key, Callback<String> callback) {
        callAsync(key, callback);
    }
    
    /**
     * Calls a method on the underlying Javascript object that returns a String.  Call is asynchronous.  Return value or error is passed to the
     * provided callback.
     * @param key The name of the method.
     * @param callback Callback to handle the return value.
     */
    public void callStringAsync(String key, final SuccessCallback<String> callback) {
        callStringAsync(key, new CallbackAdapter<String>() {

            @Override
            public void onSucess(String value) {
                callback.onSucess(value);
            }
        });
    }
    
    /**
     * Calls a method on the underlying Javascript object that returns a Javascript object.   Called synchronously with no arguments.
     * @param key The name of the method.
     * @return The return value of the method.
     */
    public JSObject callObject(String key){
        return (JSObject)call(key);
    }
    
    /**
     * Calls a method on the underlying Javascript object that returns a Javascript object.  Call is asynchronous.  Return value or error is passed to the
     * provided callback.
     * @param key The name of the method.
     * @param callback Callback to handle the return value.
     */
    public void callObjectAsync(String key, Callback<JSObject> callback) {
        callAsync(key, callback);
    }
    
    /**
     * Calls a method on the underlying Javascript object that returns a Javascript object.  Call is asynchronous.  Return value or error is passed to the
     * provided callback.
     * @param key The name of the method.
     * @param callback Callback to handle the return value.
     */
    public void callObjectAsync(String key, final SuccessCallback<JSObject> callback) {
        callObjectAsync(key, new CallbackAdapter<JSObject>() {

            @Override
            public void onSucess(JSObject value) {
                callback.onSucess(value);
            }
        });
    }
    
    /**
     * Calls the object as a function statically.  In this case "this" will
     * be window.
     * 
     * <p>E.g.</p>
     * <code><pre>
     * JSObject alert = (JSObject)ctx.get("window.alert");
     * alert.call(new Object[]{"An alert message"});
     * </pre></code>
     * <p>The above gets a reference to the alert() function (remember functions are
     * objects in Javascript).  Then it calls
     * it via Java, passing it a single string parameter.  This is equivalent 
     * to the following Javasript:</p>
     * <p><code><pre>
     * alert("An alert message");
     * </pre></code></p>
     * 
     * @param params The parameters to pass to the function.  These will be converted
     * to the appropriate Javascript type.
     * @return The result of the javascript function call converted to the appropriate
     * Java type.
     */
    public Object call(Object... params){
        JSObject window = (JSObject)context.get("window");
        return context.call(this, window, params);
    }
    
    /**
     * Calls the object as a function statically.  The call is made asynchronously  In this case "this" will
     * be window.
     * 
     * <p>E.g.</p>
     * <code><pre>
     * JSObject alert = (JSObject)ctx.get("window.alert");
     * alert.call(new Object[]{"An alert message"});
     * </pre></code>
     * <p>The above gets a reference to the alert() function (remember functions are
     * objects in Javascript).  Then it calls
     * it via Java, passing it a single string parameter.  This is equivalent 
     * to the following Javasript:</p>
     * <p><code><pre>
     * alert("An alert message");
     * </pre></code></p>
     * 
     * @param params The parameters to pass to the function.  These will be converted
     * to the appropriate Javascript type.
     * @param callback The result of the javascript function call converted to the appropriate
     * Java type and passed to the callback.
     */
    public void callAsync(Object[] params, Callback callback) {
        context.callAsync(this, context.getWindow(), params, callback);
    }
    
    /**
     * Calls the object as a function statically.  The call is made asynchronously  In this case "this" will
     * be window.
     * 
     * <p>E.g.</p>
     * <code><pre>
     * JSObject alert = (JSObject)ctx.get("window.alert");
     * alert.call(new Object[]{"An alert message"});
     * </pre></code>
     * <p>The above gets a reference to the alert() function (remember functions are
     * objects in Javascript).  Then it calls
     * it via Java, passing it a single string parameter.  This is equivalent 
     * to the following Javasript:</p>
     * <p><code><pre>
     * alert("An alert message");
     * </pre></code></p>
     * 
     * @param params The parameters to pass to the function.  These will be converted
     * to the appropriate Javascript type.
     * @param callback The result of the javascript function call converted to the appropriate
     * Java type and passed to the callback.
     */
    public void callAsync(Object[] params, final SuccessCallback callback) {
        callAsync(params, new CallbackAdapter() {

            @Override
            public void onSucess(Object value) {
                callback.onSucess(value);
            }
             
        });
    }

    /**
     * Returns the toString from the JavaScript object effectively the equivalent of {@code callString("toString")}
     * @return the String representation of the JavaScript object
     */
    @Override
    public String toString() {
        return callString("toString");
    }

    
}
