---
title: JavaScript Port Cross Origin, Hints & Bugs
slug: javascript-port-cross-origin-hints-bugs
url: /blog/javascript-port-cross-origin-hints-bugs/
original_url: https://www.codenameone.com/blog/javascript-port-cross-origin-hints-bugs.html
aliases:
- /blog/javascript-port-cross-origin-hints-bugs.html
date: '2015-05-03'
author: Shai Almog
---

![Header Image](/blog/javascript-port-cross-origin-hints-bugs/html5-banner.jpg)

When building to the JavaScript target there are many build options and configurations. More importantly  
issues like cross origin need server side code that would be able to proxy such requests to make the  
client side code seamless… 

Steve wrote a rather detailed [appendix to the developer guide](/manual/appendix-javascript.html)  
covering all of those options from startup splash screen configuration to servlet proxy logic. The build also generates  
a ready to deploy WAR file which should make setting this up on any Java servlet container a nobrainer. 

During our work on the developer guide for 3.0 we updated the  
[build hints section](/manual/advanced-topics.html) with many  
previously undocumented hints and also updated the  
[theme constants section](/manual/advanced-theming.html#_theme_constants) in a similar way.  
Hopefully, this will allow us to keep them up to date more easily as we add them in the future. This week  
we also updated the Codename One Designer constants combo box with all the documented constants. 

#### Issues with the current release

The current plugin has two annoying bugs, the first of which is a regression in test execution which should be  
resolved and will be a part of the next update. However, the second is slightly more annoying…. 

It seems that our code for packaging the demos into the plugin had a bug where it didn’t override the build.xml  
with the latest/current build XML and it got a debug version of that file from my local version of the kitchen sink.  
If you create the kitchen sink demo using the current plugin keep in mind that this won’t work for sending an Android  
build. This should be fixed for the next plugin update and shouldn’t affect other plugins.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **KL** — May 8, 2015 at 12:38 pm ([permalink](/blog/javascript-port-cross-origin-hints-bugs/#comment-22337))

> KL says:
>
> Hi,
>
> I tried this Javascript port, it is excellence, you cannot imagine 95% of my android apps runnable without any modification.
>
> The current problem is the ConnectionRequest cannot do POST request. I have to adjust all my ConnectionRequest to GET.
>
> Second issue is, the “KeyListener” not working. The compiled JS apps just dont listen to the key pressed.
>
> `  
> f.addKeyListener(‘a’, new ActionListener() {  
> public void actionPerformed(ActionEvent evt) {  
> String strChar = getChar(evt.getKeyEvent());  
> System.out.println(“Key ” + evt.getKeyEvent() + ” pressed,getChar() = ” + strChar);  
> evt.consume();  
> }  
> }); 
>
> `
>
> Which is working in android/iphone emulator properly.
>
> Hope this issues makes help for those fantastic JS port developer.
>



### **shannah78** — May 8, 2015 at 4:55 pm ([permalink](/blog/javascript-port-cross-origin-hints-bugs/#comment-22208))

> shannah78 says:
>
> Are you running your app through the single-file preview on the build server, the ZIP distribution, or the WAR distribution. The “single-file” preview uses a simplified proxy script for use on App Engine that may have trouble with some requests… I haven’t tested it with POST yet. If you use the WAR distribution, it comes bundled with a better proxy that should handle POST (though haven’t actually tested that either — but it is based on a well-established Proxy servlet that claims to handle POST).
>
> I haven’t implemented key listeners on the form yet. Please post both of these issues (separately) into the issue tracker so I don’t forget to look into them when the time comes.
>



### **KL** — May 9, 2015 at 10:11 am ([permalink](/blog/javascript-port-cross-origin-hints-bugs/#comment-21613))

> KL says:
>
> I am using zip file, and unzip them to my origin server root, and browse directly to my server.  
> I have posted to issue tracker, separately.
>
> By the way, how soon this javascript port will be in BETA?
>
> Thank you shannah.
>



### **shannah78** — May 9, 2015 at 3:31 pm ([permalink](/blog/javascript-port-cross-origin-hints-bugs/#comment-22159))

> shannah78 says:
>
> Are the POST requests being sent to the same host as the index.html file is being served on, or a different server? If a different server, you’ll need to set a proxy servlet as described here [https://www.codenameone.com…](</manual/appendix-javascript/>)
>
> The Javascript port will be in beta by July.
>



### **KL** — May 10, 2015 at 12:56 pm ([permalink](/blog/javascript-port-cross-origin-hints-bugs/#comment-24197))

> KL says:
>
> Shannah, may I know which component that you already implement key listener? I try to use TextField to handle the key event, but it just fell not so direct as Form. Possible with Label?
>



### **Shai Almog** — May 10, 2015 at 2:11 pm ([permalink](/blog/javascript-port-cross-origin-hints-bugs/#comment-22169))

> Shai Almog says:
>
> Key events aren’t sent on Android (unless the Android device has a physical keyboard) or iOS since they are restricted to physical keys and not virtual keyboards.  
> To follow input in text fields you can use DataChangeListener.
>



### **KL** — May 10, 2015 at 3:09 pm ([permalink](/blog/javascript-port-cross-origin-hints-bugs/#comment-22120))

> KL says:
>
> Yes you are right, I try to use hardware keyboard to do data entry, which allow user additional option for data entry. It is working fine in android, but not in JS port, yet.
>
> Datachanger listener cannot detect ‘Enter’ key, I use with actionListener to detect enter key. But the problem is, I have to press “Enter” twice (first to end edit mode, second enter only trigger actionListener) then only can grab the “enter” key. This is confusing user. Somemore, after the first enter, the textfield cursor ‘drop’ to newline, even I set true to singleline edit, this make the entered words ‘disapear’..
>
> Can you advice any trick to overcome this issue? Or any other component can have keylistener, before shanna implement the keylistener on the form?
>
> Your advice is much appreciated.
>



### **shannah78** — May 11, 2015 at 5:01 am ([permalink](/blog/javascript-port-cross-origin-hints-bugs/#comment-22286))

> shannah78 says:
>
> I haven’t implemented key listeners at all yet. DataChangeListener should work, and is tested to work with a soft keyboard on android, on desktop, on iOS with both a soft keyboard and a hardware keyboard. I haven’t tested android with a hardware keyboard yet. There may be a bug there. Please file an issue on this in the issue tracker and post a minimal test case.
>



### **KL** — May 11, 2015 at 8:05 am ([permalink](/blog/javascript-port-cross-origin-hints-bugs/#comment-21938))

> KL says:
>
> Can you please advice where should I file yhe issue tracker? Any ETA of the BETA of this JS port?  
> Thanks!
>



### **Shai Almog** — May 11, 2015 at 2:35 pm ([permalink](/blog/javascript-port-cross-origin-hints-bugs/#comment-22137))

> Shai Almog says:
>
> Beta should be ready by June. See [https://github.com/codename…](<https://github.com/codenameone/CodenameOne/issues/>)
>



### **KL** — May 28, 2015 at 9:17 am ([permalink](/blog/javascript-port-cross-origin-hints-bugs/#comment-22170))

> KL says:
>
> Hi, Shai, I understand the javascript port will be updated on 1/JUN, may I know is the keylistener implemented currently? Can I try now?
>



### **Shai Almog** — May 28, 2015 at 6:53 pm ([permalink](/blog/javascript-port-cross-origin-hints-bugs/#comment-22409))

> Shai Almog says:
>
> We update the JavaScript port a few times a week. If an issue exists and was marked fixed it will take less than a week for it to reach production. I suggest you follow up in the issue tracker on github.
>



### **KL** — May 29, 2015 at 7:09 am ([permalink](/blog/javascript-port-cross-origin-hints-bugs/#comment-22287))

> KL says:
>
> Look like I wrongly post this issue to googlecode previously. Thats why this issue still there.
>
> I just post to github with the title “Javascipr port, form not support keylistener #1516”
>
> Just hope this issue can be fixed.
>
> Thank you.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
