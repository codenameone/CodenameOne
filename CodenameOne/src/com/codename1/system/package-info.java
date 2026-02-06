/// Low level calls into the Codename One system, including
///     [support for making platform native API calls](https://www.codenameone.com/how-do-i---access-native-device-functionality-invoke-native-interfaces.html). Notice
///     that when we say "native" we do not mean C/C++ always but rather the platforms "native" environment. So in the
///     case of Android the Java code will be invoked with full access to the Android API's, in case of iOS an Objective-C
///     message would be sent and so forth.
///
///     Native interfaces are designed to only allow primitive types, Strings, arrays (single dimension only!) of primitives
///     and PeerComponent values. Any other type of parameter/return type is prohibited. However, once in the native layer
///     the native code can act freely and query the Java layer for additional information.
///
///     Furthermore, native methods should avoid features such as overloading, varargs (or any Java 5+ feature for that
///     matter)
///     to allow portability for languages that do not support such features (e.g. C).
///
///     Important! Do not rely on pass by reference/value behavior since they vary between platforms.
///
///     Implementing a native layer effectively means:
///
///
/// - Creating an interface that extends NativeInterface and only defines methods with the arguments/return
///         values declared in the previous paragraph.
///
///
/// - Creating the proper native implementation hierarchy based on the call conventions for every platform
///         within the native directory
///
///
///     E.g. to create a simple hello world interface do something like:
///
/// ```java
///             package com.my.code;
///             public interface MyNative extends NativeInteface {
///                   String helloWorld(String hi);
///             }
///
/// ```
///
///     Then to use that interface use MyNative my = (MyNative)NativeLookup.create(MyNative.class);
///
///     Notice that for this to work you must implement the native code on all supported platforms!
///
///     To implement the native code use the following convention. For Java based platforms (Android, RIM, J2ME):
///
///     Just create a Java class that resides in the same package as the NativeInterface you created
///     and bares the same name with Impl appended e.g.: MyNativeImpl. So for these platforms the code
///     would look something like this:
///
/// ```java
///             package com.my.code;
///             public class MyNativeImpl implements MyNative {
///                   public String helloWorld(String hi) {
///                        // code that can invoke Android/RIM/J2ME respectively
///                   }
///             }
///
/// ```
///
///     Notice that this code will only be compiled on the server build and is not compiled on the client.
///     These sources should be placed under the appropriate folder in the native directory and are sent to the
///     server for compilation.
///
///     For Objective-C, one would need to define a class matching the name of the package and the class name
///     combined where the "." elements are replaced by underscores. One would need to provide both a header and
///     an "m" file following this convention e.g.:
///
/// ```java
/// @interface com_my_code_MyNative : NSObject {
/// }
/// - (id)init;
/// - (NSString*)helloWorld:(NSString *)param1;
/// @end
///
/// ```
///
///     Notice that the parameters in Objective-C are named which has no equivalent in Java. That is why the native
///     method in Objective-C MUST follow the convention of naming the parameters "param1", "param2" etc. for all
///     the native method implementations. Java arrays are converted to NSData objects to allow features such as length
///     indication.
///
///     PeerComponent return values are automatically translated to the platform native peer as an expected return
///     value. E.g. for a native method such as this: PeerComponent createPeer();
///
///     Android native implementation would need: View createPeer();
///
///     While RIM would expect: Field createPeer()
///
///     The iphone would need to return a pointer to a view e.g.: - (UIView*)createPeer;
///     J2ME doesn't support native peers hence any method that returns a native peer would always return null.
package com.codename1.system;
