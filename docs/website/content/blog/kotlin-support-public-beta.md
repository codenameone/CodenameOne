---
title: Kotlin Support Public Beta
slug: kotlin-support-public-beta
url: /blog/kotlin-support-public-beta/
original_url: https://www.codenameone.com/blog/kotlin-support-public-beta.html
aliases:
- /blog/kotlin-support-public-beta.html
date: '2017-07-10'
author: Shai Almog
---

![Header Image](/blog/kotlin-support-public-beta/kotlin_800x320.png)

We were prepared for an uphill effort in terms of getting Kotlin up and running…​ Turns out that getting the basic support out of the door was much easier than expected with a few expected caveats that will hopefully be acceptable moving forward. Steve did pretty much all of the work on this, I asked him to write a post where he explains the challenges as this isn’t the first time he ported a JVM language to Codename One.

I’ll try to cover the user experience of using Kotlin. But let’s start with the caveats:

  * You need to use IntelliJ/IDEA – I don’t think that’s a big sacrifice as it’s an excellent IDE with great Kotlin support. Doing this for other IDE’s would be a bit more challenging

  * You will need the latest 3.7.2 plugin we released the other day

  * You will need to install the Kotlin support libraries from the extension manager tool in Codename One Preferences

  * Don’t use the project conversion tools or accept the warning that the project isn’t a Kotlin project. We do our own build process

  * Warnings and errors aren’t listed correctly and a build that claimed to have 2 errors actually passed…​

  * This will increase your jar size by roughly 730kb which might make it harder for free tier users

Having said all that we got a couple of apps working on iOS, Android and on the web (using the JavaScript port) without a problem. This is still a beta thought so we might have missed some functionality.

### Hello World

Ideally as we will refine this we will have a Kotlin option within the new project wizard that will already include all of the necessary pieces. But for now you can take the following steps.

Open your Codename One project with the 3.7.2 plugin or newer.

In the right click menu select Codename One → Codename One Preferences.

Select Extensions type in `kotlin` and install. Then right click the project select Codename One → Refresh Libs.

#### Conversion

The hello world Java source file looks like this (removed some comments and whitespace):
    
    
    public class MyApplication {
        private Form current;
        private Resources theme;
    
        public void init(Object context) {
            theme = UIManager.initFirstTheme("/theme");
            Toolbar.setGlobalToolbar(true);
            Log.bindCrashProtection(true);
        }
    
        public void start() {
            if(current != null){
                current.show();
                return;
            }
            Form hi = new Form("Hi World", BoxLayout.y());
            hi.add(new Label("Hi World"));
            hi.show();
        }
    
        public void stop() {
            current = getCurrentForm();
            if(current instanceof Dialog) {
                ((Dialog)current).dispose();
                current = getCurrentForm();
            }
        }
    
        public void destroy() {
        }
    }

When I select that file and select the menu option Code → Convert Java file to Kotlin File I get this:
    
    
    class MyApplication {
        private var current: Form? = null
        private var theme: Resources? = null
    
        fun init(context: Any) {
            theme = UIManager.initFirstTheme("/theme")
            Toolbar.setGlobalToolbar(true)
            Log.bindCrashProtection(true)
        }
    
        fun start() {
            if (current != null) {
                current!!.show()
                return
            }
            val hi = Form("Hi World", BoxLayout.y())
            hi.add(Label("Hi World"))
            hi.show()
        }
    
        fun stop() {
            current = getCurrentForm()
            if (current is Dialog) {
                (current as Dialog).dispose()
                current = getCurrentForm()
            }
        }
    
        fun destroy() {
        }
    }

That’s pretty familiar. The problem is that there are two bugs in the automatic conversion…​ That is the code for Kotlin behaves differently from standard Java.

The first problem is that Kotlin classes are final unless declared otherwise so we need to add the open keyword before the class declaration as such:
    
    
    open class MyApplication

This is essential as the build server will fail with weird errors related to instanceof so pay attention…​ We’ll try to make this fail on the simulator in the future.

The second problem is that arguments are non-null by default. The `init` method might have a null argument which isn’t used…​ So this fails with an exception. The solution is to add a question mark to the end of the call: `fun init(context: Any?)`.

So the full working sample is:
    
    
    open class MyApplication {
        private var current: Form? = null
        private var theme: Resources? = null
        fun init(context: Any?) {
            theme = UIManager.initFirstTheme("/theme")
            Toolbar.setGlobalToolbar(true)
            Log.bindCrashProtection(true)
        }
    
        fun start() {
            if (current != null) {
                current!!.show()
                return
            }
            val hi = Form("Hi World", BoxLayout.y())
            hi.add(Label("Hi World"))
            hi.show()
        }
    
        fun stop() {
            current = getCurrentForm()
            if (current is Dialog) {
                (current as Dialog).dispose()
                current = getCurrentForm()
            }
        }
    
        fun destroy() {
        }
    }

### Where do we go from Here?

That’s up to you. Our GUI builder still works with Java files, we can probably get it to work with Kotlin files too and improve the wizard to include support for new Kotlin projects.

We can add support for NetBeans & Eclipse. We can work on adapting our documentation to include Kotlin samples everywhere, the sky is the limit…​

We can do lots of things but to do those we need feedback and traction. Feedback helps us know what’s important to you, the people who use our product. Traction brings in new users and helps us promote what we are doing. If you care about this, let your friends know. They might ask for further Kotlin features and we’ll increase our investment in Kotlin.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Tom Tantisalidchai** — July 11, 2017 at 12:14 pm ([permalink](https://www.codenameone.com/blog/kotlin-support-public-beta.html#comment-23443))

> Tom Tantisalidchai says:
>
> Thanks for the awesome work! It’d be great if we can easily use Kotlin’s coroutines library from [https://github.com/Kotlin/k…](<https://github.com/Kotlin/kotlinx.coroutines>)
>



### **salah Alhaddabi** — July 11, 2017 at 5:46 pm ([permalink](https://www.codenameone.com/blog/kotlin-support-public-beta.html#comment-24150))

> salah Alhaddabi says:
>
> Does this mean you will no longer support java and use kotlin instead??? This would be a pity as Java was the main reason i am using CN1…..
>



### **shannah78** — July 11, 2017 at 5:54 pm ([permalink](https://www.codenameone.com/blog/kotlin-support-public-beta.html#comment-24141))

> shannah78 says:
>
> No. We still support, Java. I’m fairly confident that we will always support Java as, like you said, that is the main reason why many developers are using CN1 in the first place. Adding support for Kotlin just provides the option of using Kotlin in your projects as well.
>



### **Shai Almog** — July 12, 2017 at 5:07 am ([permalink](https://www.codenameone.com/blog/kotlin-support-public-beta.html#comment-24218))

> Shai Almog says:
>
> Thanks for the feedback. I’m pretty sure this can be ported to a cn1lib although I’m not sure how well cn1libs work with Kotlin. Steve?
>



### **shannah78** — July 12, 2017 at 6:06 pm ([permalink](https://www.codenameone.com/blog/kotlin-support-public-beta.html#comment-23702))

> shannah78 says:
>
> cn1libs will work fine with Kotlin.  
> I have taken a quick look at this library though and there are a few red flags that may limit its usefulness. For example, the async/yield stuff it appears to have special providers for JavaFX and Swing. Really that portion is just doing the same thing as invokeAndBlock, which CN1 has had for years (since the beginning), so I question the value of that portion of the library in the CN1 context.
>



### **Albert Gao** — October 8, 2017 at 9:12 pm ([permalink](https://www.codenameone.com/blog/kotlin-support-public-beta.html#comment-23722))

> Albert Gao says:
>
> Great news! The Kotlin is the reason we are currently evaluating the codenameOne as a cross platform solution. Although I have a little program on how to consume the lib that are written in Kotlin, any help would be great. I use this repo and it works: [https://github.com/shannah/…](<https://github.com/shannah/codenameone-kotlin>) , but don’t know how to add another kotlin lib like kotlin coroutine and kotlin reflection. Any help would be thankful.
>



### **Shai Almog** — October 9, 2017 at 4:38 am ([permalink](https://www.codenameone.com/blog/kotlin-support-public-beta.html#comment-23553))

> Shai Almog says:
>
> I think you should be able to wrap them as cn1libs
>



### **Thomas** — March 2, 2018 at 3:04 am ([permalink](https://www.codenameone.com/blog/kotlin-support-public-beta.html#comment-23959))

> Thomas says:
>
> Is the kotlin api fully supported or is it just a subset of its classes like for java (for example can I import io.socket.client.Socket in a kotlin class in codenameone and this would work out of the box? or like for [java.io](<http://java.io>) some kotlin class aren’t supported?)  
> Also, is it possible to mix some kotlin files with some java ones in the same codenameone project?
>



### **Shai Almog** — March 2, 2018 at 4:52 am ([permalink](https://www.codenameone.com/blog/kotlin-support-public-beta.html#comment-23698))

> Shai Almog says:
>
> You can mix Java & Kotlin files like any other project.
>
> No you can’t access the full Java SE API, you have almost the same restrictions as the Java version has. Notice that projects like Kotlin Native have even worse restrictions…
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
