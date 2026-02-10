---
title: Security Issues of Cross Platform Tools
slug: security-issues-cross-platform-tools
url: /blog/security-issues-cross-platform-tools/
original_url: https://www.codenameone.com/blog/security-issues-cross-platform-tools.html
aliases:
- /blog/security-issues-cross-platform-tools.html
date: '2020-08-06'
author: Shai Almog
---

![Header Image](/blog/security-issues-cross-platform-tools/security.jpg)

A couple of weeks ago I [answered a question on Quora](https://www.quora.com/Are-there-any-specific-security-issues-when-developing-cross-platform-banking-apps/answer/Shai-Almog?prompt_topic_bio=1) about the security of cross platform tools. I try to rise about my confirmation bias when discussing these things. I won’t discuss Codename One in this context or any other specific tool. Only general ideas.

Security depends a lot on the tools involved and their level of support for security features such as certificate pinning, storage/db encryption etc. Some tools also store the code of the app as plain text or obfuscated scripting code which is still fully readable, this can have a serious impact on security.

In fact this level of insecurity spawned a thriving cottage industry of repackaging. Where people unzip the application and repackage/sign it and upload it to the store under a different package name. Then use ads/payment to earn from the stolen app. This can be very profitable to them as the time gap until detection and takedown process can be pretty long.

### Reverse Engineering

Reverse engineering is possible no matter what tool you use. Cross platform tools can make this either easier or harder.

There are plenty of off the shelf tools to reverse engineer native apps. You can literally view the full UI design used by the developer and then search for the event handling code within the decompiled application. E.g. if you have a login form a hacker can find the login button, run the app and find out a lot about the process.

Here the cross platform tools divide into three distinct categories:

  * **Native GUI tools** — These are usually on par or worse than native apps when it comes to security. The native communication/layout is often visible via standard reverse engineering tools

  * **Web tools** — Cross platform tools that are based on web technologies are usually very easy to reverse engineer. To a level where a hacker can change JavaScript on the spot or even use web debugging tools to debug the app remotely

  * **Lightweight Tools** — Tools that render their own UI are usually more secure in that sense. Decompiler tools can’t always see some of these tools and find it really hard to deal with their UI. Such tools can be much harder to reverse engineer than native apps

#### Obfuscation

Obfuscation is the first line of defense against reverse engineering. It’s an essential tool to make reverse engineering harder.

Some tools and some common native 3rd party libraries, discourage obfuscation. A lot of tools limit the scope of obfuscation which is generally a bad thing to do.

### Tips

Things to ask your cross platform tool vendor:

  * Is my code visible in the final binary?

  * What level of obfuscation do I have here? Is there a separately obfuscated scripting language (e.g. javascript)?

  * Can code be injected remotely? This is sometimes presented as a “feature” where you can circumvent the appstore submission process. Apple made that illegal and removed such tools in the past

  * Do you support encrypted storage/DB?

  * Do you support certificate pinning?

  * Do you use custom socket communication and not the OS level connection (this is important as there might be a low level vulnerability in a custom implementation of SSL)?  
It’s more secure to use the OS native APIs when doing networking operations

  * I disable copy and paste?

  * Can I disable the OS screenshot feature in the task manager (this isn’t possible on all OS’s)?

  * Can I detect jailbroken devices?  
Notice that this isn’t always possible and is a bit flaky

  * Do you support biometric authentication primitives

  * Who do I contact when I find something and need help?
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — August 8, 2020 at 7:58 am ([permalink](https://www.codenameone.com/blog/security-issues-cross-platform-tools.html#comment-24312))

> [Francesco Galgani](https://lh6.googleusercontent.com/-4K0ax_DVJf4/AAAAAAAAAAI/AAAAAAAAAAA/AMZuuckEd1kcni0y8k6NMzNtxwOCEPatQQ/photo.jpg) says:
>
> Always remaining in a general discourse, my impression is that computer security is something non-existent, in the sense that no software or hardware can be said to be really secure. It’s usually a matter of compromises between being too paranoid or too relaxed.
>
> The more complex a system made up of hardware and software (and many commonly used devices are terribly complex, from smartphones to the latest car models, from personal computers to the latest TV models), the more likely it is that something will get out of the control of both designers and users, sometimes because of real mistakes, sometimes because of unintended or unforeseen circumstances in which such devices operate, and sometimes for simply unknown reasons. Errors are an essential and inevitable part of any computer system, therefore, in an absolute sense, there is no security.
>
> On a practical level, if while writing a software we think that our worst enemy can read its source code entirely and that, even in such a case, we do not put him in a position to do damage (or to do it easily), then the software is (maybe?) reasonably well designed in terms of security, but this does not exclude that there may be also important security problems. This applies to both open-source and cloused-source software.
>
> Another consideration, always at the design stage, is that we should think about security on the assumption that all electronic communications are interceptable and, in the case of our apps, all outgoing and incoming traffic can be sniffed. In this regard, I like Shai’s advice not to use passwords in mobile apps: in fact they are not needed and the fact of not using them solves the security problems related to password management in the first place. Now I’m also doing password-free login systems with activation via email or sms link.
>
> All this, however, is still not enough. As far as smartphones are concerned, it only takes five minutes, for someone with sufficient technical knowledge, to install invisible and pervasive spy programs that reveal every activity and movement of the victim, including the activities in the apps developed by us. So let’s be careful and don’t take anything for granted.
>
> By the way, in the specific case of the mobile app world, a fundamental part of security is how server-side software is developed and how the server is configured and protected. 
>
> On the subject of computer security, I wrote a short section in my thesis “The Age of Technological Persuasion and Education in the Use of Technology”, I refer to paragraph 3.11.2, p. 56, 57 and 58. The text is in Italian: <https://www.informatica-libera.net/content/era-della-persuasione-tecnologica>
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsecurity-issues-cross-platform-tools.html)


### **Javier Anton** — August 8, 2020 at 6:07 pm ([permalink](https://www.codenameone.com/blog/security-issues-cross-platform-tools.html#comment-24313))

> [Javier Anton](https://lh3.googleusercontent.com/a-/AAuE7mDRjHkEvAZNXquh9p7Oo00ey1yOwNzZ0SrFwD0IVA) says:
>
> Thanks Shai and Francesco. I got a specific question, sorry for being so direct in such a general discussion.  
> If the source code is obfuscated, how can the NativeLogs reader tell me the specific file name and code lines in stack traces?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsecurity-issues-cross-platform-tools.html)


### **Shai Almog** — August 9, 2020 at 1:36 am ([permalink](https://www.codenameone.com/blog/security-issues-cross-platform-tools.html#comment-24316))

> Shai Almog says:
>
> The default obfuscation settings from Google leave these things in place. So method names, variables etc. are all obfuscated and even the class names. But file names and line numbers are preserved so stack traces will make some sense. I think that’s a sensible default.
>
> Yes it makes the code a bit less obfuscated but there’s always a trade-off. When we obfuscate completely stacks become ambiguous and really hard to follow even with the map file. BTW if you don’t know about the mapping file: <https://www.codenameone.com/blog/tip-obfuscation-mapping-file.html>
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsecurity-issues-cross-platform-tools.html)


### **Shai Almog** — August 9, 2020 at 1:39 am ([permalink](https://www.codenameone.com/blog/security-issues-cross-platform-tools.html#comment-24314))

> Shai Almog says:
>
> Agreed. I usually give the multi-layered security analogy as an “onion”. It’s hard to block a truly motivated hacker in some tiers but you can make him cry on every damn layer.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsecurity-issues-cross-platform-tools.html)


### **Francesco Galgani** — August 9, 2020 at 10:38 am ([permalink](https://www.codenameone.com/blog/security-issues-cross-platform-tools.html#comment-24315))

> [Francesco Galgani](https://lh6.googleusercontent.com/-4K0ax_DVJf4/AAAAAAAAAAI/AAAAAAAAAAA/AMZuuckEd1kcni0y8k6NMzNtxwOCEPatQQ/photo.jpg) says:
>
> I’ll try to add something about your question, compared to what Shai has already written. First, the source code of NativeLog Reader is really simple, it’s the first cn1lib I published, you can read the code here: <https://github.com/jsfan3/CN1Libs-NativeLogsReader> As you can see, in the case of Android it simply returns the output of “logcat” (on some phones it requires that the usb debug mode has been activated, even without usb connection), in the case of iOS it redirects the “standard output” (stdout) and the “standard error” (stderr) to a file. However, if you invoke Log.sendLogAsync() after a Log.e(ex), again you will see the file name and line number that launched the captured exception.
>
> As for the obfuscation, here I published my own analysis of how easy or difficult it is to reverse engineer an app made with Codename One: <https://www.informatica-libera.net/content/%C3%A8-possibile-il-reverse-engineering-di-unapp-fatta-con-codename-one-risalire-al-codice>  
> The text is in Italian, however, if you search the page for the word “jadx”, you’ll see three snippets of code, one after the other: the first is the original source code written in Netbeans, the second is the result of the decompilation made when the apk is produced by Codename One in debug mode, the third is the result of the decompilation made when the apk is produced by Codename One in release mode (i.e. with ProGuard active): only in the latter case the code is almost unreadable. In both cases (debug mode and release mode), the comments, if present, are lost, while the text strings remain as they are (and therefore allow an attacker to look for something specific, unless the strings are obfuscated with Xor or other types of obfuscation). Note that the reverse engineering of Android applications made with Codename One is generally more difficult than “native” applications (those made with Android Studio, to understand) because Codename One does not use the XML format. 
>
> The article then continues by analyzing iOS: in this case I assert that decompiling is almost “mission impossible”.
>
> Finally, I examine the case of the web-apps made with Codename One, listing in detail the levels of obfuscation used, and then conclude that going back from the Javascript code produced by the Codename One build server to “usable” Java code is unrealistic (and I report an example to clarify any doubt).
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsecurity-issues-cross-platform-tools.html)


### **Javier Anton** — August 10, 2020 at 7:05 pm ([permalink](https://www.codenameone.com/blog/security-issues-cross-platform-tools.html#comment-24318))

> [Javier Anton](https://lh3.googleusercontent.com/a-/AAuE7mDRjHkEvAZNXquh9p7Oo00ey1yOwNzZ0SrFwD0IVA) says:
>
> Thank you both. My intuition told me that it must be something like what Shai said but it’s nice to have it confirmed. Really useful to see the resulting code in Francesco’s example
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsecurity-issues-cross-platform-tools.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
