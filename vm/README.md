# ParparVM
## The safe simple & easy way to build native iOS apps in Java

<img align="right" src="http://codenameone.com/img/parpar.png" height="250">

ParparVM is the VM developed by Codename One to replace the defunct [XMLVM](http://xmlvm.org) with which it was 
originally built. We took our extensive experience both in JITs and native OS development and built something
that is both simple, conservative and performant. 

ParparVM was designed as a client side VM. It features a **concurrent GC** that doesn't block the current execution thread. 

## Usage
The ByteCodeTranslator and JavaAPI projects are designed as a NetBeans project although it should be possible to work with any Java IDE or ant directly. It requires asm 5.0.3 which you can find in the [cn1-binaries](http://github.com/codenameone/cn1-binaries) project.

You can run the translation process using:

```bash
java -jar ByteCodeTranslator.jar ios path_to_stub_class:path_to/vm/JavaAPI/build/classes;path_to_your_classes  dest_build_dir MainClassName com.package.name "Title For Project" "1.0" ios none
```

Once the translation process succeeds you should have a valid xcode project that you can run and use as usual. You will need a Mac for this to work.

The main class name (referred to as stub class) is expected to have a `public static void main(String[])` method and it is assumed to reside in the `com.package.name` directory  (figuratively, you need to replace `com.package.name` with your actual package passed to the translator).

## Why Another VM for iOS?

There are many VM's on the market and a few open source ones but none of the ones that translate to C are actively maintained. None of the other VM's have a non-blocking concurrent GC at this time. This is an important feature when dealing with UI's as we don't want GC stalls during animations.

[J2ObjC](https://github.com/google/j2objc) is an excellent tool for porting libraries to Objective-C but it isn't designed
to be a full scale VM (no GC etc.).

The other VM's (Avian & RoboVM) are pretty impressive technically since they compile directly to ARM/LLVM. 
That is a very problematic approach:

 - While this is an impressive feat Apple doesn't officially support this route. That means that with every transition 
 Apple makes (64bit, bitcode etc.) hurdles occur. This isn't some theoretical issue but something that has created 
 serious stumbling blocks to such projects.
 
 ParparVM had a seamless migration to iOS 9 (no code changes!) and had a relatively easy 64 bit transition!
 By comparison RoboVM's CEO [wrote this](https://groups.google.com/d/msg/robovm/OnE3moz3d-8/nba0ury5CwAJ) at the time:

> "Our work to add full support for iOS 9 in time for its public release was one of the most daunting challenges we’ve faced in our existence" 

To reiterate the point, ParparVM required no changes for this to work...

 - These VM's usually use the entire Android class libraries resulting in relatively large apps. They also take longer 
 to compile as a result. This can result in order of magnitude difference in size! 
 The problem is that in iOS we need to package at least 2 platforms (e.g. 32bit and 64bit or bitcode) and once an app is submitted to Apple the size increases significantly. A Codename One app can be 5mb during development and 10-13mb after submission. These tools often feature 100mb+ app sizes. 
 The problem with that is that Apple places some restrictions (such as OTA update policies) on larger apps. 
 
 - Xcode's tools can't be used to their full extent with such tools, with ParparVM you can just use Xcodes amazing
 profiler and related tools out of the box since all the code is C code
 
 - Hacking these tools requires some deep VM/ASM/LLVM knowledge. ParparVM is trivial by comparison and requires a bit 
 of Java bytecode knowledege and some C. 


Besides these advantages ParparVM also embeds the a concurrent GC logic directly into the code, this is very similar to
ARC in some aspects. 

## Performance

Since the Xcode C compiler is VERY fast the performance of ParparVM is pretty good. ParparVM's performance in "real life" situations is generally very good.

ParparVM doesn't support JNI and just invokes C code directly when needed which means there is no JNI overhead. This makes integrating native code trivial and easy. To further improve performance we rewrote some bottleneck code in C so it will perform as fast as possible.

## Java Level Support

We aimed the VM at Java 5 support and overlay the Java 8 support with retrolambda, it should work for some Java 8 syntax out of the box simply because of ASM's ability to parse newer class files. 

The API is relatively limited in scope to keep the size low, we occasionally add additional API's ideally with very concise implementations to avoid bringing over the full JDK. Check out [this post](https://www.codenameone.com/blog/why-we-dont-support-the-full-java-api.html) explaining why we don't think supporting the entire JDK makes sense.

## Relation To Codename One

ParparVM is used by Codename One internally, it's open source and we have no intention to change that. It is used in Codename One's Mac build servers (so Codename One proper doesn't require a Mac) and also used by the offline builder tool.

Parpar has no dependency on Codename One that we know of but some might exist inadvertantly since Codename One is the only target we have. If such a dependency exists and you can think of an elegant way to remove it please feel free to submit a pull request.

## Support

We try to answer all [questions tagged codenameone on stackoverflow](http://stackoverflow.com/tags/codenameone) but since these questions might be esoteric it's possible that we won't be able to answer some of them. The Codename One discussion group is generally aimed at Codename One development and not at using the source code/native. Since these are advanced topics they might dillute the discussion there.

## Areas Of Interest

#### Performance
We would like to improve the performance of the VM further while keeping source/binary size down and reducing compilation time (tall order). This can be accomplished by:

  - Additional optimization options - stack elimination, method init elimination.
  - Exception processing - currently we don't rely on CPU code for exception detection. We can rely on some processor specific behavior to implement null pointer, array index out of bounds etc.
  - Better dead code elimination and better inlining logic
  - Better handling of interfaces which have a big overhead

#### Crash handling
Currently VM crashes arn't graceful, it should be pretty easy to extract VM state and log it to a file that can be used on next launch. Since most of the code related to the stack tracking is in C it should be accessible and easy to log this on a crash.

#### Ports
When we started this work we envisioned a C# compilation target as well. However, we eventually decided to go with iKVM for the Windows 10 port. It might be interesting to port to other platforms and embedded platforms.

## The Name

Parpar is a butterfly in Hebrew.

Initially when we worked at Sun and produced LWUIT we wanted to call it Morpho (butterfly I think in Spanish) but marketing shot it down.
Unfortunately MorphoVM is already taken so we decided to go with ParparVM instead.
