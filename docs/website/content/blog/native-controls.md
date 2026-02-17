---
title: Native Controls
slug: native-controls
url: /blog/native-controls/
original_url: https://www.codenameone.com/blog/native-controls.html
aliases:
- /blog/native-controls.html
date: '2019-01-28'
author: Shai Almog
---

![Header Image](/blog/native-controls/new-features-4.jpg)

We supported native widgets within the [native interface](/how-do-i---access-native-device-functionality-invoke-native-interfaces.html) feature since the first public beta. However, this requires native coding and isn’t always trivial. Normally you don’t really need to do that.

E.g. for text fields we implicitly convert the field to a native field as necessary.

However, if you wish to use the password manager of the device this won’t work. To solve this Steve introduced a new cn1lib: [cn1-native-controls](https://github.com/shannah/cn1-native-controls). This library wraps some native widgets and lets us use some features that might be tricky without them. Good use cases for this include the password managers which need a native text widget constantly in that sport. Another use case would be iOS’s SMS interception text field that can automatically intercept the next incoming SMS and set it to a special native text field.

There are currently two widgets in this library:

  * `NSelect` — which acts like a combo box

  * `NTextField` — which is practically a drop-in replacement for `TextField`

You can use the `NTextField` like this:
    
    
    hi.add("Text fields");
    hi.add("Username:");
    NTextField tf1 = new NTextField(TextField.USERNAME);
    System.out.println("Setting font to main light 15mm");
    tf1.getAllStyles().setFont(Font.createTrueTypeFont(Font.NATIVE_MAIN_LIGHT, 15f));
    System.out.println("Finished setting font");
    tf1.getAllStyles().setFgColor(0x003300);
    tf1.getAllStyles().setBgTransparency(255);
    tf1.getAllStyles().setBgColor(0xcccccc);
    tf1.getAllStyles().setAlignment(CENTER);
    hi.add(tf1);
    hi.add("Password:");
    NTextField tf2 = new NTextField(TextField.PASSWORD);
    hi.add(tf2);
    hi.add("Email:");
    NTextField emailField = new NTextField(TextField.EMAILADDR);
    hi.add(emailField);
    
    tf1.addActionListener(e->{
        //tf2.setText(tf1.getText());
    });
    tf1.addChangeListener(e->{
       result.setText(tf1.getText());
       hi.revalidateWithAnimationSafety();
    });
    tf2.addActionListener(e->{
        Log.p("Action listener fired on password field");
        result.setText(tf2.getText());
        hi.revalidateWithAnimationSafety();
    });
    tf2.addDoneListener(e->{
        Log.p("Done was clicked!!!");
    });
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Ch Hjelm** — February 1, 2019 at 12:30 pm ([permalink](/blog/native-controls/#comment-24110))

> Ch Hjelm says:
>
> Hi, I tried to use this (I use a local copy of CN1 source, but updated yesterday, I used NTextField in my code, run "refresh cn1lib files"), but I get this error message:
>
> ` error: cannot find symbol 
>
> public class NTextFieldNativeImpl implements com.codename1.nui.NTextFieldNative{ 
>
> symbol: class NTextFieldNative 
>
> location: package com.codename1.nui`
>
> When I look in the cn1 GitHub repository, I don’t see any ‘nui’ package…
>



### **shannah78** — February 1, 2019 at 1:01 pm ([permalink](/blog/native-controls/#comment-24101))

> shannah78 says:
>
> When you first install the lib, you may need to do a clean and build once before it will pick up the native classes.
>



### **Ch Hjelm** — February 1, 2019 at 7:06 pm ([permalink](/blog/native-controls/#comment-24003))

> Ch Hjelm says:
>
> Thanks Steve, I’ve done a clean build, but it doesn’t help. The library is in /lib as it should be, but when I run the "refresh cn1lib files" I get the below errors.
>
> `Compiling 2 source files to /Users/user1/NetBeansProjects/myapp/native/internal_tmp  
> /Users/user1/NetBeansProjects/myapp/lib/impl/native/javase/com/codename1/nui/NSelectNativeImpl.java:38: error: cannot find symbol  
> public class NSelectNativeImpl implements com.codename1.nui.NSelectNative{  
> symbol: class NSelectNative  
> location: package com.codename1.nui  
> /Users/user1/NetBeansProjects/myapp/lib/impl/native/javase/com/codename1/nui/NTextFieldNativeImpl.java:3: error: cannot find symbol  
> public class NTextFieldNativeImpl implements com.codename1.nui.NTextFieldNative{  
> symbol: class NTextFieldNative  
> location: package com.codename1.nui  
> /Users/user1/NetBeansProjects/myapp/lib/impl/native/javase/com/codename1/nui/NSelectNativeImpl.java:61: error: cannot find symbol  
> NSelect.fireSelectionChanged(index);  
> symbol: variable NSelect  
> 3 errors  
> /Users/user1/NetBeansProjects/myapp/build.xml:531: Compile failed; see the compiler error output for details.`
>
> I tried adding it to my Kitchensink example and there it works. Can my build files somehow be corrupted (and any tips on how to fix it – I’m no expert)?
>



### **Ch Hjelm** — February 2, 2019 at 9:57 am ([permalink](/blog/native-controls/#comment-24035))

> Ch Hjelm says:
>
> I tried a number of things, update the cn1 binaries, recompile my local copy of CN1 sources with Java 8, but nothing helped, I’m stuck with the above errors. Any help or suggestions for what I can investigate would be much appreciated 🙂
>



### **Shai Almog** — February 3, 2019 at 4:26 am ([permalink](/blog/native-controls/#comment-24077))

> Shai Almog says:
>
> What’s the version of the build.xml file? It’s on the top of the file.  
> Can you post a screenshot of your build classpath from the netbeans properties?
>



### **Ch Hjelm** — February 3, 2019 at 10:55 am ([permalink](/blog/native-controls/#comment-24104))

> Ch Hjelm says:
>
> I compared my class paths with the KitchenSink example, and realized I had deleted the lib/impl/, override/ and native/internal_tmp folders. I added them manually, and now it works. That reminded me of somewhere in the CN1 manual there’s a screenshot of the class path list in Netbeans saying don’t change this if you don’t know what you’re doing – I now realize why 🙂 Thanks a lot for pointing me in the right direction.
>



### **Ch Hjelm** — February 8, 2019 at 12:03 pm ([permalink](/blog/native-controls/#comment-24086))

> Ch Hjelm says:
>
> I would like to use the NTextField in the login screen, and when that screen is shown, automatically enter edit mode for the email field. With a normal TextField I can use Form.setEditOnShow(emailField), but it doesn’t accept the NTextField. Is there some way to achieve the same effect when using the NTextField?
>



### **Shai Almog** — February 9, 2019 at 6:00 am ([permalink](/blog/native-controls/#comment-23966))

> Shai Almog says:
>
> No you can’t use it like that. However, you can add an onShow listener and start editing at that point which should be equivalent.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
