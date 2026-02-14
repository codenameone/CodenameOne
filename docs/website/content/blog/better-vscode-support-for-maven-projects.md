---
title: Better VSCode Support for Maven Projects
slug: better-vscode-support-for-maven-projects
url: /blog/better-vscode-support-for-maven-projects/
original_url: https://www.codenameone.com/blog/better-vscode-support-for-maven-projects.html
aliases:
- /blog/better-vscode-support-for-maven-projects.html
date: '2022-03-11'
author: Steve Hannah
description: Our next update to the Codename One application archetype (7.0.59) includes
  some VSCode-specific config files to help improve integration for Maven Projects.
---

Our next update to the Codename One application archetype (7.0.59) includes some VSCode-specific config files to help improve integration for Maven Projects.

![VSCode configs - Codename One](/blog/better-vscode-support-for-maven-projects/VSCode-configs-Codename-One-1024x536.jpg)

Switching to Maven has been a good thing over all. Maven’s dependency management allows projects to work more consistently out of the box. No more missing dependencies, for the most part. Just clone the repo, build the project and go.

Maven’s dependency management and standard project structure results in one standard project structure that should work in any IDE that supports Maven. Unfortunately, each IDE still requires a little bit of fine-tuning to wire up our Maven goals to the IDEs’ menus.

For the initial transition, we focused most of our attention on the experience in IntelliJ and NetBeans. **IntelliJ**, because it is the most widely used, and **NetBeans**, because it provides (arguably) the best Maven integration, and therefore, didn’t require us to do very much.

We did our best with Eclipse, but will need to take a second pass at it, as it uses a very different model for its launch actions than NetBeans and IntelliJ.

We left VSCode users on their own to do the link-ups, for the most part, because they’re such a clever bunch, and we knew they’d be able to figure it out 🙂

Now that the dust has settled on the migration, we’re circling back to try to tighten it up. Our next update to the Codename One application archetype (7.0.59) includes some VSCode-specific config files to help improve integration.

### Opening Projects in VSCode

I’ll demonstrate this integration by creating a new Codename One Application project in [Codename One initializr](https://start.codenameone.com/).

Point your browser to **[https://start.codenameone.com](https://start.codenameone.com/)**

![](/blog/better-vscode-support-for-maven-projects/start-codenameone-com.png)

In the left panel, select the “Bare-bones Java App” template, enter a package and Main Class, and select “VSCode” in the **IDE** drop-down. Then press the **Download** button.

It will prompt you to save the project, which will then download as a zip file. Extract the zip file, then open the resulting project folder in VSCode.

The project window should look like the following screenshot:

![](/blog/better-vscode-support-for-maven-projects/vscode-window.png)

## NOTE

> You may be prompted to install the Java Extension Pack in one of the notices in the lower-right corner. If so, you should follow these prompts to install that extension.

### Running the Project

Unfortunately VSCode “Run” button doesn’t work to run the project in the simulator. We’re working on that. To run the project in the Codename One simulator, you’ll need to use the “Run in Simulator” command which is available in the **Maven favorites** menu.

In the left “Explorer” panel, you should see a “Maven” option at the bottom. Expand this to show all of the modules in the application. The first module listed is the “root” module, and is also the module on which all of the core actions are registered.

Right click on this module, then select “Favorites…​” in the context menu as shown below:

![](/blog/better-vscode-support-for-maven-projects/maven-favorites.png)

This will reveal a menu with all of the important actions you’ll need to perform on your project.

![](/blog/better-vscode-support-for-maven-projects/favorites-menu.png)

## IMPORTANT

> If this is your first time running the project, you should select "Tools > Update Codename One" to force the project to download all of the dependencies. If you don’t do this, it is possible you’ll get an error when you try to run the app, due to missing dependencies.

Select the “Run in Simulator” option to build and launch your app in the Codename One simulator.

![](/blog/better-vscode-support-for-maven-projects/hello-simulator.png)

### Still Work To Do

Currently, debugging is still a bit of a challenge in VSCode. We are working on improving that process, and should have that sorted out soon. If you use VSCode, please let us know what you think, on one of our channels: [Reddit](https://www.reddit.com/r/cn1/), [StackOverflow](https://stackoverflow.com/questions/tagged/codenameone), the mailing list, or in the comment section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **Bryan Buchanan** — June 7, 2022 at 11:22 am ([permalink](https://www.codenameone.com/blog/better-vscode-support-for-maven-projects.html#comment-24536))

> Bryan Buchanan says:
>
> Trying this out. Followed all the steps, and get to  
> If this is your first time running the project, you should select “Tools > Update Codename One” to force the project to download all of the dependencies
>
> Under Tools there is no Update option ?
>



### **Bryan Buchanan** — June 9, 2022 at 12:50 am ([permalink](https://www.codenameone.com/blog/better-vscode-support-for-maven-projects.html#comment-24537))

> Bryan Buchanan says:
>
> Oops – was looking at the wrong Tools location. All good now !
>



### **Bryan Buchanan** — June 10, 2022 at 7:53 am ([permalink](https://www.codenameone.com/blog/better-vscode-support-for-maven-projects.html#comment-24539))

> Bryan Buchanan says:
>
> Having played around with quite a few projects (after using the Maven conversion tool) I have to say the VSCode integration is excellent. Well done.
>



### **N P** — August 22, 2023 at 8:23 pm ([permalink](https://www.codenameone.com/blog/better-vscode-support-for-maven-projects.html#comment-24568))

> N P says:
>
> VScode is telling me “found no favorite commands” when I click on favorites
>



### **Shai Almog** — August 23, 2023 at 2:00 am ([permalink](https://www.codenameone.com/blog/better-vscode-support-for-maven-projects.html#comment-24569))

> Shai Almog says:
>
> Did you download the VSCode version of the project from start or did you get a project from a different source?
>



### **Raphael Lacuna** — September 23, 2023 at 12:02 am ([permalink](https://www.codenameone.com/blog/better-vscode-support-for-maven-projects.html#comment-24575))

> Raphael Lacuna says:
>
> I have the same issue, and I downloaded it from the VSCode version of the project
>



### **Steve Hannah** — September 23, 2023 at 12:50 pm ([permalink](https://www.codenameone.com/blog/better-vscode-support-for-maven-projects.html#comment-24576))

> Steve Hannah says:
>
> Just tested on a fresh project and seemed to work.  
> In the Explorer, expand “Maven”  
> You’ll see:  
> your-app  
> your-app-anroid  
> your-app-common  
> your-app-ios  
> your-app-javascript  
> …
>
> Expand the “your-app” one.  
> Then expand “Favourites”
>
> You should see all of the key build and run commands there.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
