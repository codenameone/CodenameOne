---
title: Live CSS Update
slug: live-css-update
url: /blog/live-css-update/
original_url: https://www.codenameone.com/blog/live-css-update.html
aliases:
- /blog/live-css-update.html
date: '2018-05-14'
author: Shai Almog
---

![Header Image](/blog/live-css-update/new-features-6.jpg)

Up until last Friday CSS support has been a second class citizen, with the release of Codename One 4.2 we’re making CSS a core feature. We also improved the builtin CSS support extensively!  
Changes you make to a CSS file will instantly reflect in the simulator. You don’t need to compile or do anything special. When you launch the simulator we open a CSS watcher thread that automatically recompiles & deploys the CSS whenever you save.

Installation is also **much** simpler. You can click the CSS entry in Codename One Settings & activate CSS. That’s it!

__ |  Migration doesn’t migrate your theme! You would need to redo the theme in CSS   
---|---  
  
__ |  Disabling CSS after enabling it might have some issues, if you experience that remove the entry `codename1.cssTheme` from `codenameone_settings.properties`  
---|---  
  
If you have a project that uses the older CSS support the settings app is smart enough to recognize that and offer to migrate to the new CSS support.

![The CSS Option in Codename One Settings](/blog/live-css-update/css-in-codenameone-settings.jpg)

Figure 1. The CSS Option in Codename One Settings

__ |  These changes might need an update to the `build.xml` file. Make sure to update it when you save the file   
---|---  
  
### Other Improvements in CSS

Other than these great new features CSS has improved by leaps and bounds. One of the biggest benefits of the new CSS processing logic is speed. It’s **much** faster. The trick behind that is simplification of the process for resource file generation.

The CSS plugin occasionally uses the webkit browser from JavaFX to generate an image of the CSS style. E.g. if a complex gradient is used the CSS processor just fires up webkit and grabs a multi-image screenshot of this style.

This is powerful as it allows for complex CSS styling, but it has many pitfalls such as slower compilation, lower fidelity & larger resources.

The newer CSS version works with some of our new border types such as round border. But the bigger improvement is that it doesn’t launch the browser window unless you actually need it. This results in faster compilation times for the CSS.

### What’s Still Missing

The biggest piece we need to do is migrate the documentation to the developer guide and update this everywhere.

Another big missing piece with CSS support is localization. It’s not a CSS feature but rather something we would expect to have within the generated resource file. So java properties files would be implicitly added to the resource file during CSS compile. Personally I think we can make localization **much** easier by detecting unlocalized strings in the simulator & automatically adding them to the resource bundle.

We need some more demos & tutorials that cover CSS and ideally we would want this exposed in the “new project wizard”.

If we could automate the conversion of res files to CSS it would be great for things like this as we could instantly make all of our demos work both ways.

### The Big Picture

The obvious question here is: are we replacing the designer tool with CSS?

Not yet.

Though this is something I’m personally conflicted with. The designer tool is showing its age so it makes sense to minimize its usage. I don’t think we’ll deprecate the designer soon as it’s still convenient to work with.

Personally I find the designer more convenient probably due to habit. However, CSS has a few big advantages, for me personally the biggest advantages is documentation. I did the Uber clone app using the designer tool and while that was pretty easy to implement, the tutorial became untenable…​  
I had to grab screenshots of every UI setting and if I wanted to revise something later I had to redo the screenshots. This was sometimes complicated as it required reverting existing changes to make the shots real. Since CSS is based on code it’s far easier to walk through the CSS changes I made like I would do with any other block of source code. That’s why the Facebook Clone uses CSS and it’s indeed far more convenient in that sense.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — May 15, 2018 at 2:56 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23383))

> Thank you very much 🙂
>
> «If we could automate the conversion of res files to CSS it would be great for things like this […]». This should be great!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Durank** — May 17, 2018 at 8:55 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23938))

> Durank says:
>
> I think so that codename one must create a migration css res to new css
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Francesco Galgani** — June 10, 2018 at 9:17 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23926))

> Francesco Galgani says:
>
> Is this the only available documentation at the moment? [https://github.com/shannah/…](<https://github.com/shannah/cn1-css/wiki>)  
> Is it complete about “Fonts”, “Images”, “Supported CSS Selectors”, “Supported Properties”?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — June 11, 2018 at 5:03 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23806))

> Shai Almog says:
>
> The wiki there goes pretty deep but I agree we need to update that. When we go to the 5.0 code freeze we’ll try to pull these docs into the developer guide so we have a better/unified reference for this.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 1, 2018 at 6:24 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-21646))

> Denis says:
>
> Hi Shai,
>
> I can’t enable CSS in Codename One Properries (version 4.31), it says “failed to activate CSS plugin due to IO error. See the console log for details”, but console log is empty, and I even don’t know where to start troubleshooting this, can you please advise ?
>
> Thanks,  
> Denis
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — August 2, 2018 at 5:54 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23718))

> Shai Almog says:
>
> Hi,  
> console might be confusing. It means the console where the settings app is running. To check the output go to your home directory then enter the “.codenameone” directory. In it execute “java -jar guibuilder_1.jar -settings path-to-project/[codenameone_settings.proper…](<http://codenameone_settings.properties>)”.
>
> This should launch settings. Reproduce the issue and see the logs in that console. Make sure you are running under Oracle JDK 8 when doing that.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 2, 2018 at 6:25 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24032))

> Denis says:
>
> Hi,
>
> there is no guibuilder_1.jar, but there is guibuilder.jar, launched as you advised and got following error while trying to enable CSS:
>
> [EDT] 0:0:0,1 – Codename One revisions: ab02d7476d1486e3babf14a0b26a5a5205672439  
> [EDT] 0:0:0,2 – Exception: java.io.IOException – CSS Activation failed. build.xml file is missing the -pre-compile target which is where CSS compilation needs to be inserted.  
> java.io.IOException: CSS Activation failed. build.xml file is missing the -pre-compile target which is where CSS compilation needs to be inserted.  
> at com.codename1.apps.config.Settings.activateCSS([Settings.java](<http://Settings.java>):2468)  
> at com.codename1.apps.config.Settings.lambda$createCSSSettings$105([Settings.java](<http://Settings.java>):2705)  
> at com.codename1.ui.util.EventDispatcher.fireActionEvent([EventDispatcher.java](<http://EventDispatcher.java>):349)  
> at com.codename1.ui.Button.fireActionEvent([Button.java](<http://Button.java>):570)  
> at com.codename1.ui.Button.released([Button.java](<http://Button.java>):604)  
> at com.codename1.ui.Button.pointerReleased([Button.java](<http://Button.java>):708)  
> at com.codename1.ui.Form.pointerReleased([Form.java](<http://Form.java>):3259)  
> at com.codename1.ui.Component.pointerReleased([Component.java](<http://Component.java>):4288)  
> at com.codename1.ui.Display.handleEvent([Display.java](<http://Display.java>):2061)  
> at com.codename1.ui.Display.edtLoopImpl([Display.java](<http://Display.java>):1043)  
> at com.codename1.ui.Display.mainEDTLoop([Display.java](<http://Display.java>):961)  
> at [com.codename1.ui.RunnableWr…](<http://com.codename1.ui.RunnableWrapper.run)([RunnableWrapper.java](http://RunnableWrapper.java)>:120)  
> at [com.codename1.impl.Codename…](<http://com.codename1.impl.CodenameOneThread.run)([CodenameOneThread.java](http://CodenameOneThread.java)>:176)
>
> There is indeed nothing about “pre-compile” in build.xml in projects directory, how to fix that ? is it some build-hint that I need to add to project ?
>
> thanks !  
> Denis
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — August 2, 2018 at 6:35 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23933))

> Shai Almog says:
>
> Hi,  
> when you press save in Codename One Settings it should offer to update the build.xml file for you.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 2, 2018 at 6:50 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23940))

> Denis says:
>
> it’s doesn’t, can I do that manually ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — August 2, 2018 at 12:29 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23917))

> Shai Almog says:
>
> Maybe try flipping a switch back and forth in basic then press the save button on the top right. It should make that offer.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 2, 2018 at 12:52 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24036))

> Denis says:
>
> I don’t really see any switch there, I have changed version number in Basics and then pressed Save button, as I usually do before uploading to Google Play (alpha test), but that doesn’t helped
>
> [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/0f3d58ee881feb930089d71a1dcd7fe73ef3d80579fa29fbba6d455cf6147cec.jpg>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — August 2, 2018 at 6:35 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23621))

> Shai Almog says:
>
> It should work. I’m not sure what is causing it not to push the XML update. We’ll try to push out a fix to the CSS switch process for tomorrow that would hopefully workaround this issue.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 3, 2018 at 5:53 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23948))

> Denis says:
>
> thanks Shai, just for test I have created new codenameone project using “Hello World Vusial” template and “business” theme and tried to enable CSS for it, faced the same issue, I was worried if it’s problem particularly with my project, but looks like it’s general
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — August 3, 2018 at 1:14 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24005))

> Shai Almog says:
>
> Don’t use the visual template, it’s designed for the old GUI builder. That might be related to the problem you are experiencing.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 3, 2018 at 2:15 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-21537))

> Denis says:
>
> I see no way to bypass template while creating new project, please see the screenshot  
> [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/c65001bc822da45b8a2d2a23b0bf3bf187675bad83ad0eb5ff9399250056bffd.jpg>)
>
> My app has dynamically built UI (it’s a casual game), so it doesn’t require any template at all and I can move entire code to new project easily, if this will solve the issue …
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — August 4, 2018 at 8:48 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23908))

> Shai Almog says:
>
> That isn’t the visual template. It’s fine but I would suggest using native as the theme and bare bones as the template.  
> Is this resolved after the update to 4.33? You can update via Basic -> Update Project Libs.  
> Notice there is no newer version of the plugin. Only settings are updated.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 4, 2018 at 12:36 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23899))

> Denis says:
>
> just tried to update Project Libs, I was not aware that they weren’t updated during plugin update, but it stuck on following error:
>
> Exception in thread “main” java.io.FileNotFoundException: C:UsersDenis.codenameone[CodenameOne_SRC.zip](<http://CodenameOne_SRC.zip>)  
> at com.codename1.apps.updater.UpdateCodenameOne.runUpdate([UpdateCodenameOne.java](<http://UpdateCodenameOne.java>):228)  
> at com.codename1.apps.updater.UpdateCodenameOne.main([UpdateCodenameOne.java](<http://UpdateCodenameOne.java>):310)
>
> there is indeed no such zip file, but there is .jar instead CodenameOne_SRC.jar
>
> may be it makes sense to do a clean installation of CodenameOne ?  
> I see .codenameone, .cn1Settings and .cn1 under my user folder, is there any others ?
>
> p.s. CSS still doesn’t work, I have just checked
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — August 5, 2018 at 4:53 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23701))

> Shai Almog says:
>
> Odd, it shouldn’t fail on that. It might be an eclipse specific issue. Please file an issue on that and we’ll have a look: [http://github.com/codenameo…](<http://github.com/codenameone/CodenameOne/issues/>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 5, 2018 at 9:35 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23832))

> Denis says:
>
> I have already reinstalled Eclipse (Photon) and CodenameOne (4.3), also created new project using Native theme and Bare bones template, and looks like update went good, please take a look at screenshot  
> [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/2f29c7dda2f2669861215bcb40b5fd52c2c4e8eb6c6baa7d931f8df3b70538cc.jpg>)  
> The only thing that looks strange for me is that there is still this hour like icon that appeared after start of update  
> [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/969c2102152d8f420bc62eb490e9d212cb7d41bdb9a94f4d632eb9bdd9cf3efa.jpg>)
>
> But unfortunately this doesn’t helped with CSS, looks like the same issue (save button also doesn’t offer to update build.xml):
>
> [EDT] 0:0:0,0 – Codename One revisions: a9dfcb988f51cab1b4ccc43190b9ad9034474b18
>
> [EDT] 0:0:0,1 – Exception: java.io.IOException – CSS Activation failed. build.xml file is missing the -pre-compile target which is where CSS compilation needs to be inserted.  
> java.io.IOException: CSS Activation failed. build.xml file is missing the -pre-compile target which is where CSS compilation needs to be inserted.  
> at com.codename1.apps.config.Settings.activateCSS([Settings.java](<http://Settings.java>):2487)  
> at com.codename1.apps.config.Settings.activateCSS([Settings.java](<http://Settings.java>):2484)  
> at com.codename1.apps.config.Settings.lambda$createCSSSettings$106([Settings.java](<http://Settings.java>):2729)  
> at com.codename1.ui.util.EventDispatcher.fireActionEvent([EventDispatcher.java](<http://EventDispatcher.java>):349)  
> at com.codename1.ui.Button.fireActionEvent([Button.java](<http://Button.java>):570)  
> at com.codename1.ui.Button.released([Button.java](<http://Button.java>):604)  
> at com.codename1.ui.Button.pointerReleased([Button.java](<http://Button.java>):708)  
> at com.codename1.ui.Form.pointerReleased([Form.java](<http://Form.java>):3259)  
> at com.codename1.ui.Component.pointerReleased([Component.java](<http://Component.java>):4288)  
> at com.codename1.ui.Display.handleEvent([Display.java](<http://Display.java>):2065)  
> at com.codename1.ui.Display.edtLoopImpl([Display.java](<http://Display.java>):1043)  
> at com.codename1.ui.Display.mainEDTLoop([Display.java](<http://Display.java>):961)  
> at [com.codename1.ui.RunnableWr…](<http://com.codename1.ui.RunnableWrapper.run)([RunnableWrapper.java](http://RunnableWrapper.java)>:120)  
> at [com.codename1.impl.Codename…](<http://com.codename1.impl.CodenameOneThread.run)([CodenameOneThread.java](http://CodenameOneThread.java)>:176)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — August 6, 2018 at 4:16 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23740))

> Shai Almog says:
>
> It’s probably a bug in the Eclipse support, we’ll need to look into it so I suggest filing an issue. That hour icon is just a shortcut to the update button in the basic section. We should probably label them…
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 6, 2018 at 12:50 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24023))

> Denis says:
>
> done.
>
> [https://github.com/codename…](<https://github.com/codenameone/CodenameOne/issues/2492>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 6, 2018 at 8:16 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24050))

> Denis says:
>
> Shai,  
> Can you please help me with following, there is includeNativeBool theme constant in newly created project, and l like what it does, slider and dialogs looks much better, but looks like it now ignores globalToobarBool and hideLeftSideMenuBool, because now app is not in fullscreen mode  
> if I disable or remove includeNativeBool constant app becomes fullscreen again (there are also codename1.arg.android.statusbar_hidden=true and codename1.arg.ios.statusbar_hidden=true build hints)  
> So my question is, is it possible to keep includeNativeBool enabled and at the same time make app fullscreen ?
>
> thanks,  
> Denis
>
> p.s. I also noticed that some UTF symbols are not displaying while includeNativeBool is enabled, for example “→” symbol, looks like there is a different font ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — August 7, 2018 at 5:54 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23784))

> Shai Almog says:
>
> Your main class defines global toolbar explicitly. I would suggest leaving that.
>
> If you want to show a full screen form override the method:
>
> protected boolean shouldPaintStatusBar() {  
> return false;  
> }
>
> Don’t set a title to the form (or set it to “”) and don’t add commands.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 7, 2018 at 6:51 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23962))

> Denis says:
>
> thanks Shai, but this doesn’t worked, override shouldPaintStatusBar to return false, commented all form related commands, including setTitlle (also tried to set it to “”) toolbar is still there (in emulator, haven’t sent to real device yet), removed toolbar related theme constants and build hints, the same, any ideas ?)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — August 8, 2018 at 4:43 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24054))

> Shai Almog says:
>
> Sorry I forgot to mention you also need to remove the padding from the Toolbar. It has some default padding that leaves it visible you can do that with this simple trick as Container has no padding/margin: fullScreen.getToolbar().setUIID(“Container”);
>
> Full sample:
>
> Form fullScreen = new Form(“”, new BorderLayout()) {  
> [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/d294ce1fce8a9085744ce424c3283624bd2004dc52ea58e72acf701ae6baeda6.png>) @Override  
> protected boolean shouldPaintStatusBar() {  
> return false;  
> }  
> };  
> fullScreen.getToolbar().setUIID(“Container”);  
> SpanLabel fullScreenLabel = new SpanLabel(“Takes up the whole screen!”);  
> Style s = fullScreenLabel.getAllStyles();  
> s.setBgColor(0xff00);  
> s.setBgTransparency(0xff);  
> s.setMargin(0, 0, 0, 0);
>
> fullScreen.add(CENTER, fullScreenLabel);  
> [fullScreen.show](<http://fullScreen.show)(>);
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 8, 2018 at 6:38 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23809))

> Denis says:
>
> thanks Shai, but ))) getToolbar() returns null, although it’s there or I confuse something ?
>
> [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/45f9dde0902cf6f4b9a7ea046b188f443c5bdba714eeb7c93b65574cb5cbe796.jpg>)
>
> here is the code:
>
> public MenuForm(Resources theme, Game game) {  
> super(new TableLayout(3, 1));  
> TableLayout mainTableLayout = (TableLayout) getLayout();
>
> // Form parameters  
> getToolbar().setUIID(“Container”);  
> //setTitle(“”);  
> //setScrollable(false);  
> //getAllStyles().setBgImage(theme.getImage(“background.jpg”));  
> //getAllStyles().setBackgroundType(Style.BACKGROUND_IMAGE_ALIGNED_CENTER);
>
> I also tried this:
>
> MenuForm menuForm = new MenuForm(theme, this);  
> menuForm.getToolbar().setUIID(“Container”);  
> [menuForm.show](<http://menuForm.show)(>);
>
> both lead to this
>
> java.lang.NullPointerException  
> at com.manyukhin.words.MenuForm.<init>([MenuForm.java](<http://MenuForm.java>):58)  
> at com.manyukhin.words.Game.start([Game.java](<http://Game.java>):133)  
> at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)  
> at sun.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)  
> at sun.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)  
> at java.lang.reflect.Method.invoke(Unknown Source)  
> at com.codename1.impl.javase.Executor$1$[1.run](<http://1.run)([Executor.java](http://Executor.java)>:140)  
> at com.codename1.ui.Display.processSerialCalls([Display.java](<http://Display.java>):1129)  
> at com.codename1.ui.Display.mainEDTLoop([Display.java](<http://Display.java>):924)  
> at [com.codename1.ui.RunnableWr…](<http://com.codename1.ui.RunnableWrapper.run)([RunnableWrapper.java](http://RunnableWrapper.java)>:120)  
> at [com.codename1.impl.Codename…](<http://com.codename1.impl.CodenameOneThread.run)([CodenameOneThread.java](http://CodenameOneThread.java)>:176)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — August 9, 2018 at 7:33 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23994))

> Shai Almog says:
>
> I suggest you don’t disable the global toolbar. If you’d keep it the code above should work fine.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 9, 2018 at 8:05 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23858))

> Denis says:
>
> do you mean by theme constants or build hints, I removed them all  
> and as you can see on screenshot in my previous message, toolbar is there
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — August 10, 2018 at 4:56 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23558))

> Shai Almog says:
>
> The default code for a new project enables the toolbar. No theme constant etc. See the toolbar code here: [https://www.codenameone.com…](<https://www.codenameone.com/blog/new-default-code.html>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 10, 2018 at 5:59 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23760))

> Denis says:
>
> many thanks Shai, I missed that global toolbar configuration in main app class, was looking at Form code )) interesting that app stays in fullscreen mode even without overridden shouldPaintStatusBar, just with toolbar margins/paddings set to zeros in theme file, but I will keep both, just letting you know
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 10, 2018 at 7:41 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23783))

> Denis says:
>
> the only thing left is the issue with some unicode symbols support when includeNativeBool is enabled, for example “→” symbol, is there a way to switch to font that used when includeNativeBool is off ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — August 11, 2018 at 3:57 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24012))

> Shai Almog says:
>
> I would strongly suggest you don’t do that. The font that “works” is the system font as opposed to the native: font. But it looks bad and is less flexible both on the device and on the simulator.
>
> The thing is that these issues might not happen on the device. The simulator uses the downloadable Roboto font or builtin helvetica both of which have limits. However, on the device their behavior and supported character range is better. You should be able to use Emojiis and everything when working on the device.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 11, 2018 at 6:30 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24018))

> Denis says:
>
> Thanks Shai, I will try it on device as you suggest, as soon as I will be able to login to Dashboard, I have issues with that for some reason, talking with your support now
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 15, 2018 at 7:39 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24047))

> Denis says:
>
> Hi Shai, you were right, on real device character support is good, but notification bar is not covered by app, the same in emulator, but I thought that it’s just part of emulator skin
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — August 16, 2018 at 4:50 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23988))

> Shai Almog says:
>
> On which device? On Android it won’t be covered on iOS you need to style the status bar appropriately but I’m guessing here without a screenshot.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 16, 2018 at 10:28 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24060))

> Denis says:
>
> yes, Android (sorry, forgot to mention)  
> [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/a7dae9ec39f5683e726c46873afb393fe560579d59e88713fefeb449d18cdfec.png>)
>
> I see that AdMobManager (admobfullscreen.cn1lib) is able to show full-screen ads  
> [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/9bd3eb439ec1572d95cde237c38cd99a11ff5f6e29ecaa7f08d0cc2cbb958053.png>)
>
> so there should be some way, it’s good to have little bit more space ))
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — August 17, 2018 at 3:40 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-23918))

> Shai Almog says:
>
> There is an experimental feature to hide the status bar here: [https://www.codenameone.com…](<https://www.codenameone.com/blog/new-skins-san-francisco-font.html>)  
> Notice that the ad you are seeing is full screen but it also doesn’t show the battery/clock etc. which is pretty problematic. It might have issues with notche UI’s.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Denis** — August 17, 2018 at 7:19 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24038))

> Denis says:
>
> thanks Shai, I am developing casual game, and full-screen in context of games is exactly what this ads extension does, hide everything (toolbar, status bar) and use as much as screen space as possible, besides that, even if app itself is not full-screen, ads will be shown in full-screen mode anyway, which creates inconsistent experience, app will jump to full-screen and back, I anyway will test that on real device and see what’s better, thanks
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **ThomasH99** — January 29, 2021 at 9:29 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24388))

> ThomasH99 says:
>
> The live update of CSS has stopped working for me since some time (not sure exactly when since haven’t edited the CSS for months). Was it disabled? If not, any suggestions for how I might fix this?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — January 30, 2021 at 6:11 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24389))

> Shai Almog says:
>
> No. It should work. Look in the simulator console, does it print out anything about CSS when launching?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **ThomasH99** — January 30, 2021 at 9:02 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24390))

> ThomasH99 says:
>
> I get a load of messages like this:  
> The class com.codename1.ui.plaf.CSSBorder$ColorStop in file /Users/user/NetBeansProjects/CodenameOne/Ports/JavaSE/build/classes/com/codename1/ui/plaf/CSSBorder$ColorStop.class is out of date due to com.codename1.ui.plaf.CSSBorder$ColorStop but has not been deleted because its source file could not be determined
>
> Then I get this on app start (the simulator does read the css on launch, but apparently not afterwards): 
>
> Updating merge file /Users/user/NetBeansProjects/MyApp/css/theme.css.merged  
> Input: /Users/user/NetBeansProjects/MyApp/css/theme.css  
> Output: /Users/user/NetBeansProjects/MyApp/src/theme.res  
> Acquiring lock on CSS checksums file /Users/user/NetBeansProjects/MyApp/.cn1_css_checksums…  
> Lock obtained  
> Releasing lock  
> CSS file successfully compiled. /Users/user/NetBeansProjects/MyApp/src/theme.res
>
> When I edit and save the css file while the Simulator is running, I used to get an output trace (getting the lock, etc), but now nothing seems to happen. 
>
> This is on a Mac.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **Shai Almog** — January 31, 2021 at 2:26 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24391))

> Shai Almog says:
>
> So you don’t see something like “CSS> Changed detected in /Users/shai/temp/TestApp/css/theme.css. Recompiling” ?  
> Did you change something about the project?  
> Did you activate CSS via preferences?  
> If you create a new project, update it (using Codename One Settings) and activate CSS. Does the problem still happen?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flive-css-update.html)


### **ThomasH99** — February 2, 2021 at 5:54 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24395))

> ThomasH99 says:
>
> No, I don’t see that text anymore, only when I compile the project, not each time I change the css file. For a new project it works correctly. I’ve also checked that css is activated in the CN1 settings. I’ve made a lot of changes to the project, so difficult to trace it back. What kind of changes could break the css? At least, when this feature is not working anymore, you realize how nice it is 😉


### **Shai Almog** — February 3, 2021 at 3:17 am ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24397))

> Shai Almog says:
>
> Check if codenameone_settings.properties has the csswatcher.enabled=true entry. Also look at the CSS file itself and the related generated files. Make sure they don’t have some weird permissions or restrictions that might be blocking the CSS compiler.


### **ThomasH99** — February 4, 2021 at 6:55 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24398))

> ThomasH99 says:
>
> I tried adding the setting csswatcher.enabled=true, but still not loading. This setting is also not present in the HelloWorld example that works correctly, so it doesn’t seem to be necessary. And since the css file is loaded correctly each time the project is recompiled, I guess there shouldn’t any issues in the file itself or the generated files… I also tried updating the cn1libs, copying over the CodeNameOneBuildClient.jar from HelloWorld and deleting the files theme.css.checksums, theme.css.merged and theme.css.merged.checksums, but also no change. I’m using Netbeans 12.2 – could that have an impact? WIth CN1 Preferences editor is 6.5.


### **Steve Hannah** — February 4, 2021 at 7:11 pm ([permalink](https://www.codenameone.com/blog/live-css-update.html#comment-24399))

> Steve Hannah says:
>
> > I tried adding the setting csswatcher.enabled=true, but still not loading.
>
> @ThomasH99 Can you file and issue in the issue tracker, and share your codenameone_settings.properties file so I can take a look at the settings.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
