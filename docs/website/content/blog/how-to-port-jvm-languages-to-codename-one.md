---
title: 'Tutorial: How to Add Support for other JVM Languages'
slug: how-to-port-jvm-languages-to-codename-one
url: /blog/how-to-port-jvm-languages-to-codename-one/
original_url: https://www.codenameone.com/blog/how-to-port-jvm-languages-to-codename-one.html
aliases:
- /blog/how-to-port-jvm-languages-to-codename-one.html
date: '2017-07-17'
author: Steve Hannah
---

![Header Image](/blog/how-to-port-jvm-languages-to-codename-one/kotlin_800x320.png)

As you may have already read, we have just added support for Kotlin in Codename One. In this post, I elaborate on some of the behind the scene work that was involved in bringing Kotlin to Codename One.

## What is a JVM Language?

A JVM Language is any programming language that can be compiled to byte-codes that will run on the JVM (Java Virtual Machine). Java was the original JVM language, but many others have sprung up over the years. [Kotlin](https://kotlinlang.org/), [Scala](https://www.scala-lang.org/), [Groovy](http://groovy-lang.org/), and [JRuby](http://jruby.org/) come to mind as well-established and mature languages, but there are [many others](https://en.wikipedia.org/wiki/List_of_JVM_languages).

## How Hard is it to Port a JVM Language to Codename One?

The difficulty of porting a particular language to Codename One will vary depending on such factors as:

  1. Does it require a runtime library?

     1. How complex is the runtime library? (E.g. Does it require classes that aren’t currently offered in Codename One’s subset of the java standard libraries?)

  2. Does it need reflection?

     1. Codename One doesn’t support reflection because it would result in a very large application size. If a JVM language requires reflection just to get off the ground then adding it to Codename one would be tricky.

  3. Does it perform any runtime byte-code manipulation?

     1. Some dynamic languages may perform byte-code manipulation at runtime. This is problematic on iOS (and possibly other platforms) which prohibits such runtime behaviour.

### Step 1: Assess the Language

The more similar a language, and its build outputs are to Java, the easier it will be to port (probably). Most JVM languages have two parts:

  1. A compiler, which compiles source files to JVM byte-code (usually as .class files).

  2. A runtime library.

Currently I’m only aware of one language (other than Java) that doesn’t require a runtime library, and that is [Mirah](http://www.mirah.org/).

__ |  Codename One also supports [Mirah](https://www.codenameone.com/blog/mirah-for-codename-one.html)  
---|---  
  
#### Assessing the Byte-Code

The first thing I do is take a look at the byte-code that is produced by the compiler. I use `javap` to print out a nice version.

Consider this sample Kotlin class:
    
    
    package com.codename1.hellokotlin2
    
    import com.codename1.ui.Button
    import com.codename1.ui.Form
    import com.codename1.ui.Label
    import com.codename1.ui.layouts.BoxLayout
    
    /**
     * Created by shannah on 2017-07-10.
     */
    class KotlinForm : Form {
    
        constructor() : super("Hello Kotlin", BoxLayout.y()) {
            val label = Label("Hello Kotlin")
            val clickMe = Button("Click Me")
            clickMe.addActionListener {
                label.setText("You Clicked Me");
                revalidate();
            }
    
            add(label).add(clickMe);
    
        }
    
    
    }

Let’s take a look at the bytecode that Kotlin produced for this class:
    
    
    $ javap -v com/codename1/hellokotlin2/KotlinForm.class
    Classfile /Users/shannah/IdeaProjects/HelloKotlin2/out/production/HelloKotlin2/com/codename1/hellokotlin2/KotlinForm.class
      Last modified 10-Jul-2017; size 1456 bytes
      MD5 checksum 1cb00f6e63b918bb5a9f146ca8b0b78e
      Compiled from "KotlinForm.kt"
    public final class com.codename1.hellokotlin2.KotlinForm extends com.codename1.ui.Form
      SourceFile: "KotlinForm.kt"
      InnerClasses:
           static final #31; //class com/codename1/hellokotlin2/KotlinForm$1
      RuntimeVisibleAnnotations:
        0: #56(#57=[I#58,I#58,I#59],#60=[I#58,I#61,I#58],#62=I#58,#63=[s#64],#65=[s#55,s#66,s#6,s#67])
      minor version: 0
      major version: 50
      flags: ACC_PUBLIC, ACC_FINAL, ACC_SUPER
    Constant pool:
       #1 = Utf8               com/codename1/hellokotlin2/KotlinForm
       #2 = Class              #1             //  com/codename1/hellokotlin2/KotlinForm
       #3 = Utf8               com/codename1/ui/Form
       #4 = Class              #3             //  com/codename1/ui/Form
       #5 = Utf8               <init>
       #6 = Utf8               ()V
       #7 = Utf8               Hello Kotlin
       #8 = String             #7             //  Hello Kotlin
       #9 = Utf8               com/codename1/ui/layouts/BoxLayout
      #10 = Class              #9             //  com/codename1/ui/layouts/BoxLayout
      #11 = Utf8               y
      #12 = Utf8               ()Lcom/codename1/ui/layouts/BoxLayout;
      #13 = NameAndType        #11:#12        //  y:()Lcom/codename1/ui/layouts/BoxLayout;
      #14 = Methodref          #10.#13        //  com/codename1/ui/layouts/BoxLayout.y:()Lcom/codename1/ui/layouts/BoxLayout;
      #15 = Utf8               com/codename1/ui/layouts/Layout
      #16 = Class              #15            //  com/codename1/ui/layouts/Layout
      #17 = Utf8               (Ljava/lang/String;Lcom/codename1/ui/layouts/Layout;)V
      #18 = NameAndType        #5:#17         //  "<init>":(Ljava/lang/String;Lcom/codename1/ui/layouts/Layout;)V
      #19 = Methodref          #4.#18         //  com/codename1/ui/Form."<init>":(Ljava/lang/String;Lcom/codename1/ui/layouts/Layout;)V
      #20 = Utf8               com/codename1/ui/Label
      #21 = Class              #20            //  com/codename1/ui/Label
      #22 = Utf8               (Ljava/lang/String;)V
      #23 = NameAndType        #5:#22         //  "<init>":(Ljava/lang/String;)V
      #24 = Methodref          #21.#23        //  com/codename1/ui/Label."<init>":(Ljava/lang/String;)V
      #25 = Utf8               com/codename1/ui/Button
      #26 = Class              #25            //  com/codename1/ui/Button
      #27 = Utf8               Click Me
      #28 = String             #27            //  Click Me
      #29 = Methodref          #26.#23        //  com/codename1/ui/Button."<init>":(Ljava/lang/String;)V
      #30 = Utf8               com/codename1/hellokotlin2/KotlinForm$1
      #31 = Class              #30            //  com/codename1/hellokotlin2/KotlinForm$1
      #32 = Utf8               (Lcom/codename1/hellokotlin2/KotlinForm;Lcom/codename1/ui/Label;)V
      #33 = NameAndType        #5:#32         //  "<init>":(Lcom/codename1/hellokotlin2/KotlinForm;Lcom/codename1/ui/Label;)V
      #34 = Methodref          #31.#33        //  com/codename1/hellokotlin2/KotlinForm$1."<init>":(Lcom/codename1/hellokotlin2/KotlinForm;Lcom/codename1/ui/Label;)V
      #35 = Utf8               com/codename1/ui/events/ActionListener
      #36 = Class              #35            //  com/codename1/ui/events/ActionListener
      #37 = Utf8               addActionListener
      #38 = Utf8               (Lcom/codename1/ui/events/ActionListener;)V
      #39 = NameAndType        #37:#38        //  addActionListener:(Lcom/codename1/ui/events/ActionListener;)V
      #40 = Methodref          #26.#39        //  com/codename1/ui/Button.addActionListener:(Lcom/codename1/ui/events/ActionListener;)V
      #41 = Utf8               com/codename1/ui/Component
      #42 = Class              #41            //  com/codename1/ui/Component
      #43 = Utf8               add
      #44 = Utf8               (Lcom/codename1/ui/Component;)Lcom/codename1/ui/Container;
      #45 = NameAndType        #43:#44        //  add:(Lcom/codename1/ui/Component;)Lcom/codename1/ui/Container;
      #46 = Methodref          #2.#45         //  com/codename1/hellokotlin2/KotlinForm.add:(Lcom/codename1/ui/Component;)Lcom/codename1/ui/Container;
      #47 = Utf8               com/codename1/ui/Container
      #48 = Class              #47            //  com/codename1/ui/Container
      #49 = Methodref          #48.#45        //  com/codename1/ui/Container.add:(Lcom/codename1/ui/Component;)Lcom/codename1/ui/Container;
      #50 = Utf8               clickMe
      #51 = Utf8               Lcom/codename1/ui/Button;
      #52 = Utf8               label
      #53 = Utf8               Lcom/codename1/ui/Label;
      #54 = Utf8               this
      #55 = Utf8               Lcom/codename1/hellokotlin2/KotlinForm;
      #56 = Utf8               Lkotlin/Metadata;
      #57 = Utf8               mv
      #58 = Integer            1
      #59 = Integer            6
      #60 = Utf8               bv
      #61 = Integer            0
      #62 = Utf8               k
      #63 = Utf8               d1
      #64 = Utf8
                               nn20¢¨
      #65 = Utf8               d2
      #66 = Utf8               Lcom/codename1/ui/Form;
      #67 = Utf8               HelloKotlin2
      #68 = Utf8               KotlinForm.kt
      #69 = Utf8               Code
      #70 = Utf8               LocalVariableTable
      #71 = Utf8               LineNumberTable
      #72 = Utf8               SourceFile
      #73 = Utf8               InnerClasses
      #74 = Utf8               RuntimeVisibleAnnotations
    {
      public com.codename1.hellokotlin2.KotlinForm();
        descriptor: ()V
        flags: ACC_PUBLIC
        Code:
          stack=5, locals=3, args_size=1
             0: aload_0
             1: ldc           #8                  // String Hello Kotlin
             3: invokestatic  #14                 // Method com/codename1/ui/layouts/BoxLayout.y:()Lcom/codename1/ui/layouts/BoxLayout;
             6: checkcast     #16                 // class com/codename1/ui/layouts/Layout
             9: invokespecial #19                 // Method com/codename1/ui/Form."<init>":(Ljava/lang/String;Lcom/codename1/ui/layouts/Layout;)V
            12: new           #21                 // class com/codename1/ui/Label
            15: dup
            16: ldc           #8                  // String Hello Kotlin
            18: invokespecial #24                 // Method com/codename1/ui/Label."<init>":(Ljava/lang/String;)V
            21: astore_1
            22: new           #26                 // class com/codename1/ui/Button
            25: dup
            26: ldc           #28                 // String Click Me
            28: invokespecial #29                 // Method com/codename1/ui/Button."<init>":(Ljava/lang/String;)V
            31: astore_2
            32: aload_2
            33: new           #31                 // class com/codename1/hellokotlin2/KotlinForm$1
            36: dup
            37: aload_0
            38: aload_1
            39: invokespecial #34                 // Method com/codename1/hellokotlin2/KotlinForm$1."<init>":(Lcom/codename1/hellokotlin2/KotlinForm;Lcom/codename1/ui/Label;)V
            42: checkcast     #36                 // class com/codename1/ui/events/ActionListener
            45: invokevirtual #40                 // Method com/codename1/ui/Button.addActionListener:(Lcom/codename1/ui/events/ActionListener;)V
            48: aload_0
            49: aload_1
            50: checkcast     #42                 // class com/codename1/ui/Component
            53: invokevirtual #46                 // Method add:(Lcom/codename1/ui/Component;)Lcom/codename1/ui/Container;
            56: aload_2
            57: checkcast     #42                 // class com/codename1/ui/Component
            60: invokevirtual #49                 // Method com/codename1/ui/Container.add:(Lcom/codename1/ui/Component;)Lcom/codename1/ui/Container;
            63: pop
            64: return
          LocalVariableTable:
            Start  Length  Slot  Name   Signature
               32      32     2 clickMe   Lcom/codename1/ui/Button;
               22      42     1 label   Lcom/codename1/ui/Label;
                0      65     0  this   Lcom/codename1/hellokotlin2/KotlinForm;
          LineNumberTable:
            line 13: 0
            line 14: 12
            line 15: 22
            line 16: 32
            line 21: 48
    }

That’s a big mess of stuff, but it’s pretty easy to pick through it when you know what you’re looking for. The layout of this output is pretty straight forward. The beginning shows that this is a class definition:
    
    
    public final class com.codename1.hellokotlin2.KotlinForm extends com.codename1.ui.Form

Even just comparing this line with the class definition from the source file we have learned something about the Kotlin compiler. It has made the class `final` by default. That observation shouldn’t affect our assessment here, but it is kind of interesting.

After the class definition, it shows the internal classes:
    
    
    InnerClasses:
         static final #31; //class com/codename1/hellokotlin2/KotlinForm$1

**The Constant Pool**

And the constants that are used in the class:
    
    
    Constant pool:
       #1 = Utf8               com/codename1/hellokotlin2/KotlinForm
       #2 = Class              #1             //  com/codename1/hellokotlin2/KotlinForm
       #3 = Utf8               com/codename1/ui/Form
       #4 = Class              #3             //  com/codename1/ui/Form
       #5 = Utf8               <init>
       #6 = Utf8               ()V
       #7 = Utf8               Hello Kotlin
       #8 = String             #7             //  Hello Kotlin
       #9 = Utf8               com/codename1/ui/layouts/BoxLayout
       ... etc...

The constant pool will consist of class names, and strings mostly. You’ll want to peruse this list to see if the compiler has added any classes that aren’t in the source code. In the example above, it looks like Kotlin is pretty faithful to the original source’s dependencies. It didn’t inject any classes that aren’t in the original source.

Even if the compiler does inject other dependencies into the bytecode, it might not be a problem. It is only a problem if those classes aren’t supported by Codename One. Keep your eyes peeled for anything in the `java.lang.reflect` package or unsolicited use of `java.net`, `java.nio`, or any other package that aren’t part of the Codename One standard library. If you’re not sure if a class or package is available in the Codename One standard library, check [the javadocs](https://www.codenameone.com/javadoc/).

**The ByteCode Instructions** :

After the constant pool, we see each of the methods of the class written out as a list of bytecode instructions. E.g.
    
    
    public com.codename1.hellokotlin2.KotlinForm();
      descriptor: ()V
      flags: ACC_PUBLIC
      Code:
        stack=5, locals=3, args_size=1
           0: aload_0
           1: ldc           #8                  // String Hello Kotlin
           3: invokestatic  #14                 // Method com/codename1/ui/layouts/BoxLayout.y:()Lcom/codename1/ui/layouts/BoxLayout;
           6: checkcast     #16                 // class com/codename1/ui/layouts/Layout
           9: invokespecial #19                 // Method com/codename1/ui/Form."<init>":(Ljava/lang/String;Lcom/codename1/ui/layouts/Layout;)V
          12: new           #21                 // class com/codename1/ui/Label
          15: dup
          16: ldc           #8                  // String Hello Kotlin
          etc...

In the above snippet, the first instruction is `aload_0` (which adds `this` to the stack). The 2nd instruction is `ldc`, (which loads constant #8 — the string “Hello Kotlin” to the stack). The 3rd instruction is `invokestatic` which calls the static method define by Constant #14 from the constant pool, with the two parameters that had just been added to the stack.

__ |  You don’t need to understand what all of these instructions do. You just need to look for instructions that may be problematic.   
---|---  
  
The only instruction that I **think** might be problematic is “invokedynamic”. All other instructions should work find in Codename One. (I don’t know for a fact that invokedynmic won’t work – I just suspect it might not work on some platforms).

**Summary of Byte-code Assessment**

So to summarize, the byte-code assessment phase, we’re basically just looking to make sure that the compiler doesn’t tend to add dependencies to parts of the JDK that Codename One doesn’t currently support. And we want to make sure that it doesn’t use invokedynamic.

If you find that the compiler does use invokedynamic or add references to classes that Codename One doesn’t support, don’t give up just yet. You might be able to create your own “porting” runtime library that will provide these dependencies at runtime.

#### Assessing the Runtime Library

The process for assessing the runtime library is pretty similar to the process for the bytecodes. You’ll want to get your hands on the language’s runtime library, and use `javap` to inspect the .class files. You’re looking for the same things as you were looking for in the compiler’s output: “invokedynamic” and classes that aren’t supported in Codename One.

### Step 2: Convert the Runtime Library into a CN1Lib

Once you have assessed the language and are optimistic that it is a good candidate for porting, you can proceed to port the runtime library into Codename One. Usually that language’s runtime library will be distributed in .jar format. You need to convert this into a cn1lib so that it can be used in a Codename One project. If you can get your hands on the source code for the runtime library then the best approach is to paste the source files into a Codename One Library project, and try to build it. This has the advantage that it will validate the source during compile to ensure that it doesn’t depend on any classes that Codename One doesn’t support.

If you can’t find the sources of the runtime library or they don’t seem to be easily “buildable”, then the next best thing is to just get the binary distribution’s jar file and convert it to a cn1lib. This is what I did for the [Kotlin runtime library](https://github.com/shannah/codenameone-kotlin).

This procedure exploits the fact that a cn1lib file is just a zip file with a specific file structure inside it. The cross-platform Java .class files are all contained inside a file named “main.zip”, inside the zip file. This is the only **mandatory** file that must be inside a cn1lib.

To make the library easier to use the cn1lib file can also contain a file named “stubs.zip” which includes stubs of the Java sources. When you build a cn1lib using a Codename One Library project, it will automatically generate stubs of the source so that the IDE will have access to nice things like Javadoc when using the library. The kotlin distribution includes a separate jar file with the runtime sources, named “kotlin-runtime-sources.jar”, so I used this as the “stubs”. It contains full sources, which isn’t necessary, but it also doesn’t hurt.

So now that I had my two jar files: kotlin-runtime.jar and kotlin-runtime-sources.jar, I created a new empty directory, and copied them inside. I renamed the jars “main.zip” and “stubs.zip” respectively. Then I zipped up the directory and renamed the zip file “kotlin-runtime.cn1lib”.

__ |  Building cn1libs manually in this way is a **very** bad habit, as it bypasses the API verification step that normally occurs when building a library project. It is possible, even likely, that the jar files that you convert depend on classes that aren’t in the Codename One library, so your library will fail at runtime in unexpected ways. The only reason I could do this with kotlin’s runtime (with some confidence) is because I already analyzed the bytecodes to ensure that they didn’t include anything problematic.   
---|---  
  
### Step 3: Hello World

For our “Hello World” test we will need to create a separate project in our JVM language and produce class files that we will **manually** copy into an appropriate location of our project. We’ll want to use the **normal** tools for the language and not worry about how it integrates with Codename One. For Kotlin, I just followed the getting started tutorial on the Kotlin site to create a new Kotlin project in IntelliJ. When I ported Mirah, I just used a text editor and the mirahc command-line compiler to create my Hello World class. The tools and process will depend on the language.

Here is the “hello world” I created in Kotlin:
    
    
    package com.mycompany.myapp
    
    class HelloKotlin {
    
        fun hello() {
            System.out.println("Hello from Kotlin");
        }
    }

After building this, I have a directory that contains “com/mycompany/myapp/HelloKotlin.class”.

It also produced a .jar file that contains this class.

I have found that the easiest way to integrate external code into a Codename One project, is just to wrap it as a cn1lib file and place it into my Codename One project’s lib directory. That way I don’t have to mess with any of the build files. So, using roughly the same procedure as I used to create the kotlin-runtime.cn1lib, I wrap my hellokotlin.jar as a cn1lib to produce “hellokotlin.cn1lib” and copy it to the “lib” directory of a Codename One project.

__ |  Remember to select “Codename One” → “Refresh CN1Libs” after placing the cn1lib in your lib directory or it won’t get picked up.   
---|---  
  
Finally, I call my library from the start() method of my app:
    
    
    HelloKotlin hello = new HelloKotlin();
    hello.hello();

If I run this in the Simulator, it should print “Hello from Kotlin” in the output console. If I get an error, then I dig in and try to figure out what went wrong using my standard debugging techniques. **EXPECT** an error on the first run. Hopefully it will just be a missing import or something simple.

### Step 4: A More Complex Hello World

In the case of Kotlin, the hello world example app would actually run without the runtime library because it was so simple. So it was necessary to add a more complex example to prove the need for the runtime library. It doesn’t matter what you do with your more complex example, as long as it doesn’t require classes that aren’t in Codename One.

If you want to use the Codename One inside your project, you should add the CodenameOne.jar (found inside any Codename One project) to your classpath so that it will compile.

### Step 5: Automation and Integration

At this point we already have a manual process for incorporating files built with our alternate language into a Codename One project. The process looks like:

  1. Use standard tools for your JVM language to write your code.

  2. Use the JVM language’s standard build tools (e.g. command-line compiler, etc..) to compile your code so that you have .class files (and optionally a .jar file).

  3. Wrap your .class files in a cn1lib.

  4. Add the cn1lib to the lib directory of a Codename One project.

  5. Use your library from the Codename One project.

When I first developed Mirah support I just automated this process using an [ANT script](https://github.com/shannah/CN1MirahNBM/blob/master/src/ca/weblite/codename1/mirah/build.xml). I also automatically generated some bootstrap code so that I could develop the whole app in Mirah and I woudn’t have to write any Java. However, I soon found that this level of integration has limitations.

For example, with this approach alone, I couldn’t have two-way dependencies between Java source and Mirah source. Yes, my Mirah code could use Java libraries (and it did depend on CodenameOne.jar), and my Java code could use my Mirah code. However, my Mirah **source** code could not depend on the Java **source** code in my project. This has to do with the order in which code is compiled. It’s a bit of a chicken and egg issue. If we are building a project that has Java source code and Mirah source code, we are using two different compilers: mirahc to compile the Mirah files, and javac to compile the Java files. If we are starting from a clean build, and we run mirahc first, then the .java files haven’t yet been compiled to .class files – and thus mirahc can’t **reference** them – and any mirah code that depends on those uncompiled Java classes will fail. If we compile the .java files first, then we have the opposite problem.

I worked around this problem in Mirah by writing [my own pseudo-compiler](https://github.com/shannah/mirah-ant/blob/master/src/ca/weblite/asm/JavaExtendedStubCompiler.java) that produced stub class files for the java source that would be referenced by mirahc when compiling the Mirah files. In this way I was able to have two-way dependencies between Java and Mirah in the same project.

Kotlin also supports two-way dependencies, probably using a similar mechanism.

#### How Seamless Can You Make It?

For both the Kotlin and Mirah support, I wanted integration to be seamless. I didn’t want users to have to create a separate project for their Kotlin/Mirah code. I wanted them to simply add a Kotlin/Mirah file into their project and have it **just work**. Achieving this level of integration in Kotlin was quite easy, since they provide an [ANT plugin](https://kotlinlang.org/docs/reference/using-ant.html) that essentially allowed me to just add one tag inside my `<javac/>` tags:
    
    
    <withKotlin/>

And it would automatically handle Kotlin and Java files together: Seamlessly. There are a few places in a Codename One’s build.xml file where we call “javac” so we just needed to inject these tags in those places. This injection is performed automatically by the Codename One IntelliJ plugin.

For Mirah, I developed my own [ANT plugins](https://github.com/shannah/mirah-ant) and [Netbeans module](https://github.com/shannah/mirah-nbm) that do something similar in Netbeans.

## Which Language Will Be Next?

Hacking on JVM byte-code can actually be a lot of fun. If you are interested in adding support for a language, we’d love to know about it. Until then, happy coding!
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Gareth Murfin** — July 18, 2017 at 1:19 pm ([permalink](https://www.codenameone.com/blog/how-to-port-jvm-languages-to-codename-one.html#comment-23526))

> Gareth Murfin says:
>
> Incredible work. So will the gui builder spit out kotlin too ? Also I think adding swift for iOS native dev would be awesome!..
>



### **Don't Bother** — July 18, 2017 at 5:07 pm ([permalink](https://www.codenameone.com/blog/how-to-port-jvm-languages-to-codename-one.html#comment-23307))

> Don't Bother says:
>
> Swift is already a native language for iOS. So I don’t really understand what you are asking for. And I have not heard about ability to complie swift to java bytecode.
>



### **shannah78** — July 18, 2017 at 6:35 pm ([permalink](https://www.codenameone.com/blog/how-to-port-jvm-languages-to-codename-one.html#comment-21522))

> shannah78 says:
>
> Not sure about the maturity of this, but here’s one. [https://github.com/brettwoo…](<https://github.com/brettwooldridge/jet>). If someone wants to try to add swift support to Codename One, it might make for an interesting exercise.
>
> This is not to be confused with using Swift to write native iOS code in a Codename one native interface. This is already possible (albiet a bit painful) by building a library separately using swift/xcode, and including the library in your ios/native lib – You only need to write a small amount of Objective-C to serve as the native interface itself.
>



### **Gareth Murfin** — July 19, 2017 at 2:19 am ([permalink](https://www.codenameone.com/blog/how-to-port-jvm-languages-to-codename-one.html#comment-23497))

> Gareth Murfin says:
>
> Oh thats interesting is there any links for that? I I want to write more native libs but I want to use swift.
>



### **Shai Almog** — July 19, 2017 at 5:08 am ([permalink](https://www.codenameone.com/blog/how-to-port-jvm-languages-to-codename-one.html#comment-23624))

> Shai Almog says:
>
> That’s a very different thing. The main blocker for this is integrating ARC with the GC. That should work in theory but doesn’t.
>



### **Adi J** — August 6, 2017 at 1:11 pm ([permalink](https://www.codenameone.com/blog/how-to-port-jvm-languages-to-codename-one.html#comment-21853))

> Adi J says:
>
> can I make app for tizen os along with android and ios by codename one.
>



### **Shai Almog** — August 7, 2017 at 5:23 am ([permalink](https://www.codenameone.com/blog/how-to-port-jvm-languages-to-codename-one.html#comment-23602))

> Shai Almog says:
>
> We don’t natively support tizen but we do support JavaScript in the enterprise tier which should work on Tizen.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
