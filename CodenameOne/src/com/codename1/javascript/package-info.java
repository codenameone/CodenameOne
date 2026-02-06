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

/// **NOTE:** The `com.codename1.javascript` package is now deprecated. The preferred method of
///     Java/Javascript interop is to use `BrowserComponent#execute(java.lang.String)`, `com.codename1.util.SuccessCallback)`,
///     `BrowserComponent#executeAndWait(java.lang.String)`, etc.. as these work asynchronously (except in the
///     XXXAndWait() variants, which use invokeAndBlock() to make the calls synchronously.
///
/// The Codename One JS Bridge package includes classes that facilitate the
///     interaction between Java and Javascript in a Codename One application.
///     It allows both Java to Javascript communication and the reverse via a
///     mechanism similar to the one employed by Phone Gap/Apache Cordova.
///
/// Requirements
///
/// The bridge will only run on platforms that include a native browser component. This includes iOS,
///     Android, Windows, Desktop & JavaScript at this time.
///
/// Usage
///
/// The `com.codename1.javascript.JavascriptContext` class lays the foundation by enabling you to call
///     Javascript code directly from Java. It provides automatic type conversion
///     between Java and Javascript types as follows:
///
/// Java to Javascript
///
///
///
///         Java Type
///         Javascript Type
///
///
///
///         String
///         String
///
///
///         Double/Integer/Float/Long
///         Number
///
///
///         Boolean
///         Boolean
///
///
///         JSObject
///         Object
///
///
///         null
///         null
///
///
///         Other
///         Not Allowed
///
///
///
/// Javascript to Java
///
///
///
///         Javascript Type
///         Java Type
///
///
///
///
///         Number
///         Double
///
///
///         String
///         String
///
///
///         Boolean
///         Boolean
///
///
///         Object
///         JSObject
///
///
///         Function
///         JSObject
///
///
///         Array
///         JSObject
///
///
///         null
///         null
///
///
///         undefined
///         null
///
///
///
///     Note that this conversion table is more verbose than necessary, since Javascript functions
///     and arrays are, in fact Objects themselves, so those rows are redundant. All Javascript
///     objects are converted to `com.codename1.javascript.JSObject`s.
///
/// Getting Started
///
/// In order to start interacting with a Javascript environment, you need to create
///     a WebBrowser and load a page. Then inside the WebBrowser's onLoad() handler, you
///     can create a JavascriptContext on the internal BrowserComponent object:
///
/// Getting Values
///
/// The `com.codename1.javascript.JavascriptContext#get(String)` method is used to get values from Javascript.
///     It takes an arbitrary Javascript expression, executes it, and returns the
///     result, after converting it to the corresponding Java type. E.g. if the result
///     is a String, it will return a String and if it is a Number it will return a
///     java Double object. If the result is an Object, it will return a `com.codename1.javascript.JSObject`.
///
/// The following is a simple example that retrieves the document content, which
///     is a string:
///
/// If you run this example in the simulator, you'll see something like the following:
///
///
///
/// Note: You don't see the WebBrowser in the background here due to a bug in the simulator.
///     If you run this example on a device, you will see the WebBrowser component in the background.
///
/// Returning Numeric Values
///
/// As mentioned in the conversion table above, numeric values are automatically
///     converted to java.lang.Double objects. The following example, returns the width and height
///     properties of the window for use in Java.
///
/// The result, when run in the simulator would be something like:
///
/// Returning Objects
///
/// The previous examples involved only primitive return values. The `com.codename1.javascript.JavascriptContext`
///     abstraction,
///     in these cases, doesn't offer a whole lot of added-value over just using the
///     BrowserComponent.executeJavascriptAndReturnString()
///     method. The real value is when we are dealing with objects.
///
/// The following example obtains a reference to the `window` object and wraps it in a
///     proxy `com.codename1.javascript.JSObject` class so that we can work directly with the window object:
///
/// This code produces the exact same result as the previous example. The difference
///     is the intermediary step of wrapping the window object in a `com.codename1.javascript.JSObject`, and
///     obtaining the outerHeight and outerWidth properties directly via that proxy object.
///
/// You can obtain a `com.codename1.javascript.JSObject` proxy for any Javascript object, even ones that you create
///     on the fly. The following example creates an anonymous object with some keys and values
///     and uses a `com.codename1.javascript.JSObject` proxy to interact with this object from Java.
///
/// The result is as follows:
///
/// See [Working With Objects](#working-with-objects) for more information
///     about working with the `com.codename1.javascript.JSObject` class.
///
/// Returning Functions and Arrays
///
/// In Javascript, functions and arrays are just objects, so these are also encapsulated as `com.codename1.javascript.JSObject`
///     proxies. See [Working with Arrays](#working-with-arrays) and
///     [Workin with Functions](#working-with-functions) for more details on how to work
///     with these values via the `com.codename1.javascript.JSObject` interface.
///
/// Setting Values
///
/// Just as you can get values from Javascript using `com.codename1.javascript.JavascriptContext`'s get() method,
///     you can
///     also set values via `com.codename1.javascript.JavascriptContext#set(String,Object)`.
///
/// The following is a simple example that sets the location, and causes it to redirect to
///     a different page:
///
/// If you run this example, you should see your browser display the Codename One website after
///     a redirect.
///
/// Setting Object Values
///
/// The previous example showed us setting a primitive String value. You can do the same with other
///     primitives like numbers and booleans, but you can also set Object values using the set() method.
///
/// The following example creates an anonymous Javascript object, wraps it in a `com.codename1.javascript.JSObject`
///     proxy,
///     sets some values on it, then sets the object as a property of the top-level window object.
///
/// As a result, you should see the following content set as the body of the HTML page in the
///     WebBrowser. Note that we can refer to the "steve" object that we just set directly/globally
///     because the "window" object's properties are always available directly through the global
///     namespace in Javascript.
///
/// Working with Objects
///
/// Previously examples showed how to obtain a `com.codename1.javascript.JSObject` proxy to a Javascript object.
///     There are 4 ways to get a `com.codename1.javascript.JSObject`:
///
///
/// - Create an anonymous object:
/// `JSObject obj = (JSObject)ctx.get("{}");`
///
/// - Reference an existing object directly:
/// `JSObject obj = (JSObject)ctx.get("window.location");`
///
///
/// - As the result of a Javascript expression or function call:
///
/// `JSObject obj = (JSObject)ctx.get("document.getElementById('mydiv')")`
///
///
/// - Retrieve an Object member variable from another JSObject:
///
/// `JSObject obj = (JSObject)otherObj.get("aPropertyThatIsAnObject")`
///
///
/// `com.codename1.javascript.JSObject`s are essentially just proxies around a Javascript object. Any calls to
///     retrieve
///     properties from a `com.codename1.javascript.JSObject` are just sent directly to the Javascript context, and
///     the result
///     returned. The `com.codename1.javascript.JSObject` object doesn't store copies the javascript object's
///     properties. It just
///     retrieves them as they are required via the `com.codename1.javascript.JSObject#get(String)` method.
///
/// Getting Simple Values
///
/// You can always retrieve the properties of an object using the `com.codename1.javascript.JSObject#get(String)`
///     method. It takes the name of the property
///     as a parameter, and returns its value, converted to the appropriate Java type. (e.g. if it is a String,
///     it returns a String, if it is a number it returns a Double, and if it is an Object, it returns an object.
///
/// E.g.
///
/// `````java String name = (String)obj.get("name"); Double age = (Double)obj.get("age"); `````
///
///
///     Is equivalent to the following javascript:
///
///
///     `var name = obj.name; var age = obj.age;`
///
///
///     Assuming that the `obj` variable in Java is a proxy for the same obj variable
///     in the javascript example.
///
/// Getting Nested Object Values
///
/// Often, in Javascript, an object contains a heirarchy of nested child objects. E.g.
///
/// `````java var obj = { name : 'Steve', position : { x : 100, y : 105, z : -25 } } `````
///
/// In this case you may want to obtain the x coordinate of the nested position object. `JSObject`
///     allows
///     you to use the dot '.' notation for referencing sub-properties like this. E.g.
///
///     `````java Double x = (Double)obj.get("position.x") `````
///
/// This feature raises the issue of how, then, to access properties that contain a '.' in its name. E.g.
///
/// `````java var obj = { name : 'Steve', position : { x : 100, y : 105, z : -25 }, 'position.x' : 200 } `````
///
/// In this example there is a top-level property named 'position.x' as well as a property at the component address position.x.
///     This is a contrived example that is meant to be somewhat confusing in order to demonstrate how to differentiate
///     between requests for properties in the child object heirarchy and top-level properties that happen to
///     include a '.' in the property name.
///
/// We can force the retrieval of a top-level property by wrapping the key in single quotes:
///
/// `````java Double x1 = (Double)obj.get("'position.x'") `````
///
/// This would return
///     200
///     for the above example, whereas:
///
/// `````java Double x2 = (Double)obj.get("position.x") `````
///
/// Would return
///     100
///
/// .
///
/// Setting Object Values
///
/// The `com.codename1.javascript.JSObject#set(String,Object)` method works the same as the `com.codename1.javascript.JavascriptContext#set(String,Object)` method except that it treats the local
///     object as the root node. It allows you to easily set properties on the object. Values set here should
///     be provided using Java values as they will automatically be converted to the appropriate associated Javascript
///     type. If you are setting an Object as a value, then you'll need to set it as a `com.codename1.javascript.JSObject` and not a string
///     representation of the object. This is because Strings will just be converted to Javascript strings.
///
/// Properties set via the `com.codename1.javascript.JSObject#set(String,Object)` method modify the underlying
///     Javascript object directly so that the change
///     is immediately effective inside the javascript environment.
///
/// Just as with the `com.codename1.javascript.JSObject#get(String)` method, you can set the values of direct
///     properties or nested properties using
///     the dot '.' notation. And just like `com.codename1.javascript.JSObject#get(String)`, you can force setting a
///     direct property in cases where the property
///     name includes a '.', by wrapping the key inside single quotes.
///
/// E.g.:
///
/// `````java // Create a team object , and leave city null for now. JSObject blueJays = (JSObject)ctx.get("{name : 'Blue Jays', city : null}"); // Create a city object and leave country null for now. JSObject toronto = (JSObject)ctx.get("{name : 'Toronto', country : null}"); // Create a country object JSObject canada = (JSObject)ctx.get("{name : 'Canada'}"); // Set the team's city to toronto blueJays.set("city", toronto); // Set toronto's country to canada toronto.set("country", canada); // Retrieve the name of the country where the blue jays play String countryName = (String)blueJays.get("city.country.name"); // Should contain "Canada" // Change the name of Canada to "Canuck land" using nested // dot notation on the blueJays object. blueJays.set("city.country.name", "Canuck land"); String blueJaysCountry = (String)blueJays.get("city.country.name"); String torontoCountry = (String)tornoto.get("country.name"); String canadaName = (String)canada.get("name"); //Note that all of these should be equal and contain "Canuck land" `````
///
/// Calling Object Methods
///
/// The `com.codename1.javascript.JSObject#call(String,Object)` method allows you to call javascript methods that
///     are members of the underlying object. It arguments are passed as an
///     Object[] array. These will be automatically converted from the Java type to the corresponding
///     Javascript type. Java type conversion are the same as using the `com.codename1.javascript.JavascriptContext#set(String,Object)` method.
///
/// The following example shows an object with a simple `add()` method
///     that just adds two numbers together:
///
/// `````java JSObject obj = (JSObject)ctx.get("{ add : function(a,b){ return a+b;}}"); Double result = (Double)obj.call("add", new Object[]{new Integer(1), new Integer(3)} ); // Result should be 4.0 `````
///
/// Working with Arrays
///
/// In javascript, arrays are just objects that include a special ability to be iterated. You can use the alternate
///     version of `com.codename1.javascript.JSObject#get(int)` which takes an int as a parameter to
///     retrieve the elements of an array.
///
/// For example, consider the following javascript object:
///
/// `````java var obj = { name : 'Blue Jays', players : [ { name : 'George Bell', age : 31}, { name : 'Tony Fernandez', age : 34}, { name : 'Lloyd Moseby', age : 29} ] } `````
///
/// Then assuming we have a `com.codename1.javascript.JSObject` proxy for this object, we could loop through the
///     players
///     array and output the name and age of each player as follows:
///
/// `````java JObject players = (JObject)obj.get("players"); int len = ((Double)players.get("length")).intValue(); for ( int i=0; iCalling Java Methods from Javascript
///
/// So far, our examples have been limited to Java calling into Javascript. However, it may be
///     useful to be able to also go the other way: call java methods from Javascript. Some applications
///     of this might include:
///
///
/// - Capturing video, or using the camera on the phone
///
/// - Getting the phone location
///
/// - Accessing the file system or storage API
///
/// The Codename One JS bridge supports javascript to java method calling by way of the `com.codename1.javascript.JSFunction` interface
///     and the `com.codename1.javascript.JSObject#set(String,Object)` methods on the `com.codename1.javascript.JSObject` class. You can implement a `com.codename1.javascript.JSFunction`
///     object and register it as a callback with a `com.codename1.javascript.JSObject`, then you will be able to
///     execute this object's apply method via a Javascript proxy.
///
/// As an example, let's implement a simple logging function:
///
/// `````java JSObject logger = (JSObject)ctx.get("{}"); logger.set("log", new JSFunction(){ public void apply(JSObject self, Object[] args) { String msg = (String)args[0]; Log.p("[Javascript Logger] "+msg); } }); ctx.set("window.logger", logger); c.executeAndReturnString("logger.log('This is a test message');"); `````
///
/// If you execute this code in the simulator, you'll see the following output in the console:
///
/// ```java
/// [EDT] 0:0:0,0 - [Javascript Logger] This is a test message
/// ```
///
/// Running it on a device will yield similar output in the device log file.
///
/// Let's step through this code to see what is happening. First we create a new, empty javascript object
///     and wrap it in a JSObject proxy. Then we use the `com.codename1.javascript.JSObject`'s `com.codename1.javascript.JSObject#set(String,Object)` method to add an anonymous `com.codename1.javascript.JSFunction`
///     object to it with the propery of "log". This step registers a method proxy on the Javascript side that acts just
///     like a normal javascript method, but which actually triggers the `com.codename1.javascript.JSFunction`'s
///     `com.codename1.javascript.JSFunction#apply(JSObject,Object[])` method.
///
/// We then set this logger object to the global javascript scope by making it a direct child
///     of the window object. Finally we issue a Javascript method call to `logger.log()`. This
///     is what effectively calls the apply() method on our `com.codename1.javascript.JSFunction` object.
///
/// Caveats
///
/// **JSFunction callbacks are executed asynchronously so as to prevent deadlocks.** This means that you
///     cannot
///     return a value from this method using a return statement (hence the reason why the interface definition for `com.codename1.javascript.JSFunction#apply(JSObject,Object[])` is void.
///
/// **If you want to return a value back to Javascript, then you'll need to do it by providing a
///     callback function as one of the parameters, and call this callback method from inside the `com.codename1.javascript.JSFunction#apply(JSObject,Object[])`
///     method upon completion.**
///
/// Example: Passing a Javascript Callback to Your Callback
///
/// Since `com.codename1.javascript.JSFunction` callbacks are executed asynchronously, if you want to be able to
///     return a result back to Javascript, you will
///     need to do this via a Javascript callback. This is quite a common pattern in Javascript since it is single threaded
///     and relies
///     upon non-blocking patterns.
///
/// As an example, let's create a `com.codename1.javascript.JSFunction` callback that adds two numbers together and
///     returns the result to Javascript via a callback:
///
/// First we will create the `com.codename1.javascript.JSFunction` object to perform the addition, as follows:
///
/// `````java WebBrowser b = new WebBrowser(){ protected void onLoad(String url){ JSObject window = (JSObject)ctx.get("window"); window.set("addAsync", new JSFunction(){ public void apply(JSObject self, final Object[] args) { Double a = (Double)args[0]; Double b = (Double)args[1]; JSObject callback = (JSObject)args[2]; double result = a.doubleValue() + b.doubleValue(); callback.call(new Object[]{new Double(result)}); } }); } }; b.setURL("jar:///ca/weblite/codename1/tests/AddAsync.html"); `````
///
/// In this snippet, we start by obtaining a reference to the "window" object. We then add a method to this object named
///     "addAsync". This method is a `com.codename1.javascript.JSFunction`
///     object that we implement inline. The apply() method of the `com.codename1.javascript.JSFunction` object is the
///     Java method that will be executed when the addAsync method is called
///     from Javascript. In this case the addAsync method expects three parameters:
///
///
/// - The two numbers that are being added together.
///
/// - A Javascript callback method that will be executed when completed and passed the result of the addition
///         as a parameter.
///
///
/// Notice that all numerical arguments are converted to Java Double objects, and the callback function is converted to a
///     `com.codename1.javascript.JSObject` object. Also notice the use
///     of callback.call(), which just calls the callback as a function itself. With this variant of
///     the call() method,
///     the window object is used as this. Notice also that we pass the result inside an
///     Object[] array. This array will be expanded to
///     the direct javascript function parameters. (i.e. it will not pass an array as the parameter to the javascript
///     method, the array elements are extracted
///     and passed individually.
///
/// Now, let's look at the HTML page contents for our example:
///
/// `````java    Addition Example   Addition Example
///
///  +  =
///
///
///
/// Calculate
///
///     `````
///
/// Our HTML simply includes two text fields (to input the values to be added together), a button to initiate the
///     calculation,
///     and a <span> tag where the result will be placed when the calculation is complete.
///
/// Finally it includes the AddAsync.js Javascript file (which is placed in the same directory as the AddAsync.html file.
///     Its
///     contents are as follows:
///
/// `````java document .getElementById('calculate') .addEventListener('click', function(){ var aField = document.getElementById('input1'); var bField = document.getElementById('input2'); var a = parseFloat(aField.value); var b = parseFloat(bField.value); window.addAsync(a, b, function(result){ document.getElementById('result').innerHTML = result; }); }, true); `````
///
/// This script attaches an event handler to the calculate
///     button that gets the values from the two input fields and
///     passes it to the window.addAsync() method for calculation. The addAsync()
///     method is actually our java JSFunction that we implemented earlier.
///
///     One small word about the placement of these files: This example is taken from a class ca.weblite.codename1.tests.CodenameOneTests.
///     The AddAsync.html and AddAsync.js files are included in the same directory as the CodenameOneTests.java file (
///     i.e. /ca/weblite/codename1/tests). We used the WebBrowser's setURL() method to load the AddAsync.html
///     file from an
///     absolute path using jar: protocol. Currently this is the best way of loading local HTML files into
///     a WebBrowser object (i.e. use the jar: protocol and provide an absolute path).
///
/// The result of running this app is as follows:
///
/// Example: Exposing the Camera to Javascript
///
/// The following creates a Javascript function for taking photos on a mobile device. It involves a simple webpage with a
///     "Capture" button. When the user
///     clicks this button, it will dispatch a function call to CodenameOne to access the device's camera. After the user
///     takes a picture, CodenameOne will
///     execute a Javascript callback to add the picture to the webpage.
///
/// The HTML page source is as follows:
///
/// This loads the CameraExample.js script:
///
/// `````java document.getElementById('capture') .addEventListener('click', function(){ camera.capture(function(url){ if ( url == null ){ // No image was provided return; } var results = document.getElementById('results'); results.appendChild(document.createTextNode("testing")); var img = document.createElement('img'); img.setAttribute('src',url); img.setAttribute('width', '100'); img.setAttribute('height', '100'); results.appendChild(img); }) }, true); `````
///
/// The CameraExample.js script attaches a listener to the 'click' event of the "Capture" button which simply calls the
///     `camera.capture()` method,
///     which is actually a JSFunction that has been registered with the Javascript runtime. This actually calls into Java.
///
/// We pass a callback function into `camera.capture()` which will be executed upon successfully completion of
///     the camera. This is a common
///     programming pattern in Javascript. If a non-null URL is passed to this callback function, it is expected to be the
///     URL of the image (it will be
///     a local file URL.
///
/// The Java code that powers this example is as follows:
///
/// This example puts together most of the features of the CodenameOne-JS library.
///
///
/// - It creates a new JSObject from within Java code to serve as the camera object.
///
/// - It registers a JSFunction callback as a Javascript function which is added as a method to the camera object.
///
///
/// - It shows the use of the `call()` method of `JSObject` to call the callback function that
///         was
///         provided by Javascript from inside
///         the JSFunction's `apply()` method.
package com.codename1.javascript;
