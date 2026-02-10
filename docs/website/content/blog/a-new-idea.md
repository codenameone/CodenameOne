---
title: A New Idea
slug: a-new-idea
url: /blog/a-new-idea/
original_url: https://www.codenameone.com/blog/a-new-idea.html
aliases:
- /blog/a-new-idea.html
date: '2016-04-05'
author: Shai Almog
---

![Header Image](/blog/a-new-idea/a-new-idea.png)

Our current IntelliJ/IDEA plugin is seriously behind the times. In our recent survey it was very easy to spot  
IDEA developers who were significantly less satisfied with Codename One than the rest of the developer community.  
The IDEA plugin doesn’t include basic capabilities such as:

  * Java 8 Support

  * The New GUI builder

  * New Default application look/themes/icon

  * Builtin demo apps

  * The Generate Native Access functionality

  * The certificate wizard and a lot of the UI within the preferences

### A New Direction

The old plugin was developed by an excellent hacker who did a great job, but even the best code rots when  
it is unmaintained. Worse, Jetbrains made some big changes between version 12 and now. The old plugin couldn’t  
be compiled on the newer versions and exhibits odd bugs when running on newer versions of IDEA.

We decided to rewrite the plugin from the ground up and discard a lot of the legacy both in terms of the plugin  
and in terms of Codename One functionality.

The main goal of this rewrite is reduction in code so we can have a very lean plugin with as much shared code  
as possible. For that purpose the templates and builtin files are literally taken from the NetBeans plugin to facilitate  
as much reuse as possible and allow for one release cycle to encapsulate everything.

#### Java 8+ IntelliJ/IDEA 16+

The new plugin will only work on Java 8 or newer VM’s and will implicitly create Java 8 projects.

Supporting legacy project structures doesn’t make sense for the new plugin although you could manually set that  
in the generated project, this is probably unnecessary.

#### New Preferences

This might be the most controversial decision we’ve made with this plugin. Instead of using the native IDE  
menu we chose to use the right click menu for a lot of features including our own preferences UI:

![The preferences as well as other options are in the right click menu instead of the native IDE menu](/blog/a-new-idea/a-new-idea-menu.png)

Figure 1. The preferences as well as other options are in the right click menu instead of the native IDE menu

This launches a custom preferences UI written using Codename One that looks like this:

![Preferences UI written in Codename One](/blog/a-new-idea/a-new-idea-preferences.png)

Figure 2. Preferences UI written in Codename One

![Preferences UI under the basics section](/blog/a-new-idea/a-new-idea-preferences-details.png)

Figure 3. Preferences UI under the basics section

The value of writing the preferences UI using the Codename One API is that it makes the maintenance of this  
code far easier when compared to IDE specific code. A lot of features aren’t mapped to UI within the plugins  
at this time because it’s just too much of a hassle to do this 3x times for every OS/platform. Right now we  
have an internal debate on whether this should be the approach we take for all platforms, in the long run I think  
this would be superior to using the IDE native preferences UI.

The new preferences also allow us to integrate deeply with capabilities such as the iOS certificate wizard  
which has the best most fluent integration in IntelliJ/IDEA. Personally I think it looks modern and better than the  
IDE integrated approach.

#### The New GUI Builder

Support for the new GUI builder is builtin, as the builder is still in beta I’ll leave this as a somewhat “undocumented feature”  
until we finish that work. I would like to mention though that the integration injects code directly to the AST and  
dynamically updates it when the gui XML file is saved.

Normally the GUI builder updates the sources via the ant task but we could find no way to do this in IntelliJ that  
didn’t break basic things.

### Video

I’ve made a quick walkthru video of the new plugin, check it out:

### Migration & Availability

While we belive this should be maintained across versions we can’t be 100% sure about this. As with all rewrites  
regressions probably exist. We will release the new plugin this Friday as part of our standard Friday release cycle.

If you run into regressions we suggest creating a new project with the new plugin and migrating your code/settings  
by copying them to the new plugin. Be sure to let us know immediately if you run into such issues.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Therk** — April 13, 2016 at 11:42 pm ([permalink](https://www.codenameone.com/blog/a-new-idea.html#comment-22656))

> Thank you for improving the plugin. It is more intuitive even though it may not be consistent with other IntelliJ plugins. Unfortunately, I started to get a NullPointerException when setting up the Run configuration for a new sample application.  
> I am not sure the best way to report such issues, but the stack trace is:
>
> java.lang.NullPointerException
>
> at com.codename1.plugin.intellij.run.CodenameOneRunConfigurationEditor.initComponent([CodenameOneRunConfiguration…](<http://CodenameOneRunConfigurationEditor.java>):31)
>
> at com.codename1.plugin.intellij.run.CodenameOneRunConfigurationEditor.<init>([CodenameOneRunConfiguration…](<http://CodenameOneRunConfigurationEditor.java>):27)  
> at com.codename1.plugin.intellij.run.CodenameOneRunConfiguration.getConfigurationEditor([CodenameOneRunConfiguration…](<http://CodenameOneRunConfiguration.java>):225)  
> at com.intellij.execution.impl.ConfigurationSettingsEditor.<init>([ConfigurationSettingsEditor…](<http://ConfigurationSettingsEditor.java>):220)  
> at com.intellij.execution.impl.ConfigurationSettingsEditorWrapper.<init>([ConfigurationSettingsEditor…](<http://ConfigurationSettingsEditorWrapper.java>):67)  
> at com.intellij.execution.impl.SingleConfigurationConfigurable.<init>([SingleConfigurationConfigur…](<http://SingleConfigurationConfigurable.java>):65)  
> at com.intellij.execution.impl.SingleConfigurationConfigurable.editSettings([SingleConfigurationConfigur…](<http://SingleConfigurationConfigurable.java>):99)  
> at com.intellij.execution.impl.RunConfigurable.a([RunConfigurable.java](<http://RunConfigurable.java>):1090)  
> at com.intellij.execution.impl.RunConfigurable.createNewConfiguration([RunConfigurable.java](<http://RunConfigurable.java>):1123)  
> at com.intellij.execution.impl.RunConfigurable$MyToolbarAddAction$2.consume([RunConfigurable.java](<http://RunConfigurable.java>):1161)  
> at com.intellij.execution.impl.RunConfigurable$MyToolbarAddAction$2.consume([RunConfigurable.java](<http://RunConfigurable.java>):1158)  
> at com.intellij.execution.impl.NewRunConfigurationPopup$1.onChosen([NewRunConfigurationPopup.java](<http://NewRunConfigurationPopup.java>):82)  
> at com.intellij.execution.impl.NewRunConfigurationPopup$1.onChosen([NewRunConfigurationPopup.java](<http://NewRunConfigurationPopup.java>):48)  
> at com.intellij.ui.popup.list.ListPopupImpl.a([ListPopupImpl.java](<http://ListPopupImpl.java>):386)  
> at com.intellij.ui.popup.list.ListPopupImpl.handleSelect([ListPopupImpl.java](<http://ListPopupImpl.java>):346)  
> at com.intellij.ui.popup.list.ListPopupImpl$MyMouseListener.mouseReleased([ListPopupImpl.java](<http://ListPopupImpl.java>):476)  
> at java.awt.AWTEventMulticaster.mouseReleased([AWTEventMulticaster.java](<http://AWTEventMulticaster.java>):290)  
> at java.awt.Component.processMouseEvent([Component.java](<http://Component.java>):6535)  
> at javax.swing.JComponent.processMouseEvent([JComponent.java](<http://JComponent.java>):3324)  
> at com.intellij.ui.popup.list.ListPopupImpl$MyList.processMouseEvent([ListPopupImpl.java](<http://ListPopupImpl.java>):542)  
> at java.awt.Component.processEvent([Component.java](<http://Component.java>):6300)  
> at java.awt.Container.processEvent([Container.java](<http://Container.java>):2236)  
> at java.awt.Component.dispatchEventImpl([Component.java](<http://Component.java>):4891)  
> at java.awt.Container.dispatchEventImpl([Container.java](<http://Container.java>):2294)  
> at java.awt.Component.dispatchEvent([Component.java](<http://Component.java>):4713)  
> at java.awt.LightweightDispatcher.retargetMouseEvent([Container.java](<http://Container.java>):4888)  
> at java.awt.LightweightDispatcher.processMouseEvent([Container.java](<http://Container.java>):4525)  
> at java.awt.LightweightDispatcher.dispatchEvent([Container.java](<http://Container.java>):4466)  
> at java.awt.Container.dispatchEventImpl([Container.java](<http://Container.java>):2280)  
> at java.awt.Window.dispatchEventImpl([Window.java](<http://Window.java>):2750)  
> at java.awt.Component.dispatchEvent([Component.java](<http://Component.java>):4713)  
> at java.awt.EventQueue.dispatchEventImpl([EventQueue.java](<http://EventQueue.java>):758)  
> at java.awt.EventQueue.access$500([EventQueue.java](<http://EventQueue.java>):97)  
> at java.awt.EventQueue$[3.run](<http://3.run)([EventQueue.java](http://EventQueue.java)>:709)  
> at java.awt.EventQueue$[3.run](<http://3.run)([EventQueue.java](http://EventQueue.java)>:703)  
> at java.security.AccessController.doPrivileged(Native Method)  
> at java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege([ProtectionDomain.java](<http://ProtectionDomain.java>):76)  
> at java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege([ProtectionDomain.java](<http://ProtectionDomain.java>):86)  
> at java.awt.EventQueue$[4.run](<http://4.run)([EventQueue.java](http://EventQueue.java)>:731)  
> at java.awt.EventQueue$[4.run](<http://4.run)([EventQueue.java](http://EventQueue.java)>:729)  
> at java.security.AccessController.doPrivileged(Native Method)  
> at java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege([ProtectionDomain.java](<http://ProtectionDomain.java>):76)  
> at java.awt.EventQueue.dispatchEvent([EventQueue.java](<http://EventQueue.java>):728)  
> at com.intellij.ide.IdeEventQueue.h([IdeEventQueue.java](<http://IdeEventQueue.java>):857)  
> at com.intellij.ide.IdeEventQueue._dispatchEvent([IdeEventQueue.java](<http://IdeEventQueue.java>):654)  
> at com.intellij.ide.IdeEventQueue.dispatchEvent([IdeEventQueue.java](<http://IdeEventQueue.java>):386)  
> at java.awt.EventDispatchThread.pumpOneEventForFilters([EventDispatchThread.java](<http://EventDispatchThread.java>):201)  
> at java.awt.EventDispatchThread.pumpEventsForFilter([EventDispatchThread.java](<http://EventDispatchThread.java>):116)  
> at java.awt.EventDispatchThread.pumpEventsForFilter([EventDispatchThread.java](<http://EventDispatchThread.java>):109)  
> at java.awt.WaitDispatchSupport$[2.run](<http://2.run)([WaitDispatchSupport.java](http://WaitDispatchSupport.java)>:184)  
> at java.awt.WaitDispatchSupport$[4.run](<http://4.run)([WaitDispatchSupport.java](http://WaitDispatchSupport.java)>:229)  
> at java.awt.WaitDispatchSupport$[4.run](<http://4.run)([WaitDispatchSupport.java](http://WaitDispatchSupport.java)>:227)  
> at java.security.AccessController.doPrivileged(Native Method)  
> at java.awt.WaitDispatchSupport.enter([WaitDispatchSupport.java](<http://WaitDispatchSupport.java>):227)  
> at [java.awt.Dialog.show](<http://java.awt.Dialog.show)([Dialog.java](http://Dialog.java)>:1084)  
> at com.intellij.openapi.ui.impl.DialogWrapperPeerImpl$[MyDialog.show](<http://MyDialog.show)([DialogWrapperPeerImpl.java](http://DialogWrapperPeerImpl.java)>:792)  
> at [com.intellij.openapi.ui.imp…](<http://com.intellij.openapi.ui.impl.DialogWrapperPeerImpl.show)([DialogWrapperPeerImpl.java](http://DialogWrapperPeerImpl.java)>:465)  
> at com.intellij.openapi.ui.DialogWrapper.invokeShow([DialogWrapper.java](<http://DialogWrapper.java>):1661)  
> at [com.intellij.openapi.ui.Dia…](<http://com.intellij.openapi.ui.DialogWrapper.show)([DialogWrapper.java](http://DialogWrapper.java)>:1610)  
> at com.intellij.openapi.options.ex.SingleConfigurableEditor.access$001([SingleConfigurableEditor.java](<http://SingleConfigurableEditor.java>):45)  
> at com.intellij.openapi.options.ex.SingleConfigurableEditor$[1.run](<http://1.run)([SingleConfigurableEditor.java](http://SingleConfigurableEditor.java)>:130)  
> at com.intellij.openapi.project.DumbPermissionServiceImpl.allowStartingDumbModeInside([DumbPermissionServiceImpl.java](<http://DumbPermissionServiceImpl.java>):31)  
> at com.intellij.openapi.project.DumbService.allowStartingDumbModeInside([DumbService.java](<http://DumbService.java>):283)  
> at [com.intellij.openapi.option…](<http://com.intellij.openapi.options.ex.SingleConfigurableEditor.show)([SingleConfigurableEditor.java](http://SingleConfigurableEditor.java)>:127)  
> at com.intellij.execution.impl.EditConfigurationsDialog.access$001([EditConfigurationsDialog.java](<http://EditConfigurationsDialog.java>):33)  
> at com.intellij.execution.impl.EditConfigurationsDialog$[1.run](<http://1.run)([EditConfigurationsDialog.java](http://EditConfigurationsDialog.java)>:57)  
> at com.intellij.openapi.project.DumbPermissionServiceImpl.allowStartingDumbModeInside([DumbPermissionServiceImpl.java](<http://DumbPermissionServiceImpl.java>):37)  
> at com.intellij.openapi.project.DumbService.allowStartingDumbModeInside([DumbService.java](<http://DumbService.java>):283)  
> at [com.intellij.execution.impl…](<http://com.intellij.execution.impl.EditConfigurationsDialog.show)([EditConfigurationsDialog.java](http://EditConfigurationsDialog.java)>:54)  
> at com.intellij.openapi.ui.DialogWrapper.showAndGet([DialogWrapper.java](<http://DialogWrapper.java>):1625)  
> at com.intellij.execution.actions.ChooseRunConfigurationPopup$8.perform([ChooseRunConfigurationPopup…](<http://ChooseRunConfigurationPopup.java>):974)  
> at com.intellij.execution.actions.ChooseRunConfigurationPopup$ConfigurationListPopupStep$[2.run](<http://2.run)([ChooseRunConfigurationPopup>…](<http://ChooseRunConfigurationPopup.java>):500)  
> at java.awt.event.InvocationEvent.dispatch([InvocationEvent.java](<http://InvocationEvent.java>):311)  
> at java.awt.EventQueue.dispatchEventImpl([EventQueue.java](<http://EventQueue.java>):756)  
> at java.awt.EventQueue.access$500([EventQueue.java](<http://EventQueue.java>):97)  
> at java.awt.EventQueue$[3.run](<http://3.run)([EventQueue.java](http://EventQueue.java)>:709)  
> at java.awt.EventQueue$[3.run](<http://3.run)([EventQueue.java](http://EventQueue.java)>:703)  
> at java.security.AccessController.doPrivileged(Native Method)  
> at java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege([ProtectionDomain.java](<http://ProtectionDomain.java>):76)  
> at java.awt.EventQueue.dispatchEvent([EventQueue.java](<http://EventQueue.java>):726)  
> at com.intellij.ide.IdeEventQueue.h([IdeEventQueue.java](<http://IdeEventQueue.java>):857)  
> at com.intellij.ide.IdeEventQueue._dispatchEvent([IdeEventQueue.java](<http://IdeEventQueue.java>):658)  
> at com.intellij.ide.IdeEventQueue.dispatchEvent([IdeEventQueue.java](<http://IdeEventQueue.java>):386)  
> at java.awt.EventDispatchThread.pumpOneEventForFilters([EventDispatchThread.java](<http://EventDispatchThread.java>):201)  
> at java.awt.EventDispatchThread.pumpEventsForFilter([EventDispatchThread.java](<http://EventDispatchThread.java>):116)  
> at java.awt.EventDispatchThread.pumpEventsForHierarchy([EventDispatchThread.java](<http://EventDispatchThread.java>):105)  
> at java.awt.EventDispatchThread.pumpEvents([EventDispatchThread.java](<http://EventDispatchThread.java>):101)  
> at java.awt.EventDispatchThread.pumpEvents([EventDispatchThread.java](<http://EventDispatchThread.java>):93)  
> at [java.awt.EventDispatchThrea…](<http://java.awt.EventDispatchThread.run)([EventDispatchThread.java](http://EventDispatchThread.java)>:82)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fa-new-idea.html)


### **Therk** — April 14, 2016 at 12:21 am ([permalink](https://www.codenameone.com/blog/a-new-idea.html#comment-22484))

> A few suggestion on improving the IDEA plugin:  
> 1\. In the Codename One Preferences, tabbing between fields does not seem to work, requiring a mouse click into next fields. For example in the Login or iOS Certificate Wizard.  
> 2\. It is unclear if user is logged in. It would be good to show login email under Login icon if login was successful.  
> 3\. Adding certificate, revokes existing certificate. It prompts “Your iTunes account already has an iOS appstore certificate. Would you like to overwrite it?”. If I click ‘No’, then no certificate is used. It would also help to know that old certificate will be revoked in the message.  
> 4\. The iOS Certificate Wizard, tried to create certificate for each device.  
> 5\. In the device list of iOS Certificate Wizard, showing Identifier would be helpful.  
> 6\. It should allow to select existing App ID and name for an application.  
> 7\. Under Global Preferences and iOS Certificate Wizard, App ID and name should probably not be required, as I think Global Preferences are to be shared between other CodenameOne application.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fa-new-idea.html)


### **Shai Almog** — April 14, 2016 at 2:36 am ([permalink](https://www.codenameone.com/blog/a-new-idea.html#comment-22585))

> Thanks!  
> Those are great issues/RFE’s.  
> The right place to file them so they don’t get lost under our workload is the issue tracker at [http://github.com/codenameo…](<http://github.com/codenameone/CodenameOne/issues/>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fa-new-idea.html)


### **Shai Almog** — April 14, 2016 at 8:08 am ([permalink](https://www.codenameone.com/blog/a-new-idea.html#comment-22766))

> Shai Almog says:
>
> Looking a bit further on this:
>
> 4\. Are you sure about this? It just adds the device to the provisioning profile.  
> 6\. It should have the existing app id from your app which must match the package name of your project.  
> 7\. The global version of the wizard should allow you to customize the app id as it can be a * certificate but it can reside anywhere e.g. I can make a com.mycompany.* or just plain * as my default. This matters to the provisioning profile.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fa-new-idea.html)


### **Shai Almog** — April 14, 2016 at 8:14 am ([permalink](https://www.codenameone.com/blog/a-new-idea.html#comment-22774))

> Shai Almog says:
>
> How do you set the run configuration?  
> I see the problem but I can’t reproduce it to make sure the fix is correct.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fa-new-idea.html)


### **Eric Coolman** — April 28, 2016 at 9:50 pm ([permalink](https://www.codenameone.com/blog/a-new-idea.html#comment-22466))

> Eric Coolman says:
>
> Great work, and thanks! 🙂
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fa-new-idea.html)


### **James van Kessel** — May 20, 2016 at 3:56 pm ([permalink](https://www.codenameone.com/blog/a-new-idea.html#comment-22606))

> James van Kessel says:
>
> Hi Shai, For someone with an existing project, are there any warnings or cautions you’d give someone still using an older CN1 plugin (i am still on 3.1) before clicking “update Plugin”?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fa-new-idea.html)


### **Shai Almog** — May 21, 2016 at 3:51 am ([permalink](https://www.codenameone.com/blog/a-new-idea.html#comment-22611))

> Shai Almog says:
>
> If you update to the latest it will be the new plugin and there is no warning. Notice that on the plugin page at IDEA you can always download the older versions of the plugin if you need it while we fix a potential issue you might run into.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fa-new-idea.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
