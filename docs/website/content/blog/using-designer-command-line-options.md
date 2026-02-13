---
title: Using the Designers Command Line Options
slug: using-designer-command-line-options
url: /blog/using-designer-command-line-options/
original_url: https://www.codenameone.com/blog/using-designer-command-line-options.html
aliases:
- /blog/using-designer-command-line-options.html
date: '2016-10-26'
author: Shai Almog
---

![Header Image](/blog/using-designer-command-line-options/designer-tool-command-line.jpg)

Codename One’s resource files contain a lot of details but it’s sometimes hard to add elements into the resource  
file automatically. Recently I wanted to add a better way to add images directly in the new GUI builder. I considered  
several options and eventually decided to operate the designer tool automatically to do the “heavy lifting”.

I’m not the first person to try this though, some Codename One users automate tasks by generating the theme  
and UI XML’s to automatically scaffold an app.

Sidebar: Resource File XML

The resource file is a binary format which is very efficient for device consumption but not very convenient for  
code generators or source control. To solve that we created the XML Team Mode (you can toggle it under  
the File menu in the designer tool), when it is active and you open a resource file the tool starts  
by looking under the res directory for an XML file matching the resource file name and loads that instead of  
the binary resource file.

Saving is done both into the XML and the binary res file thus preserving device compatibility. This allows you to  
commit the XML and free form files into version control and handle conflicts more elegantly.

### The Command Line

The Codename One plugin automatically installs the designer in the users home directory. You can invoke  
the designer from the command line on Linux/Unix using:
    
    
    java -jar ~/.codenameone/designer_1.jar

For Windows you would do something similar as:
    
    
    java -jar c:Usersmyuser.codenameonedesigner_1.jar

From this point on I’ll use the unix/linux notation which is slightly shorter and should be easily understandable.

#### Generating a Resource file From XML

You can scaffold a project resource file relatively easily by building all of the element XML’s within the res  
directory of the project and then force the creation of an up to date resource file by invoking the designers  
`-regen` command line.

This is useful for wizards or tools that customize a theme.xml file and then want to save the changes directly  
so it is visible in the UI without the user having to re-open the res file and save it.
    
    
    java -jar ~/.codenameone/designer_1.jar -regen path-to-my-file.res

This will take the XML files and regenerate the res file with their modifications.

#### Version

The `-buildVersion` returns the designer tool version, this might be important if you depend on features/behaviors  
of a specific version. Notice that this is an integer sequential version that doesn’t match the general version  
number!

The version is based on the date as YYYYMMDD which makes it completely sequential e.g.:
    
    
    java -jar ~/.codenameone/designer_1.jar -buildVersion

Produces:
    
    
    20161021

#### Image Import

You can just import an image directly into a resource, notice that this is done one by one so if you want to add  
multiple images you need to do so one at a time. Also notice that this command doesn’t check if the designer  
is already running or if the file is already open which means an open copy of the designer might erase the changes  
made by the command…​

You can add a regular image thru a command such as:
    
    
    java -jar ~/.codenameone/designer_1.jar -img path-to/theme.res path-to/image.png image.png

The last argument is the name of the image in the resource file, it’s optional and if you leave it out the name will  
use the file name.

This will add a regular image, you can add a multi-image in a similar way to the “quick add” method in the designer  
by using the following command:
    
    
    java -jar ~/.codenameone/designer_1.jar -mimg path-to/theme.res hd path-to/image.png

The `hd` argument represents the source size of the image we are adding which allows the system to decide on  
the way to adapt it. The following DPI arguments are supported: `high`, `veryhigh`, `hd`, `560`, `2hd` & `4k`.

### Future Progress

We will add more features to the designer command line as we move forward to address more capabilities and  
extract automation to simpler cleaner tools.

Right now the designer includes in it a great deal of legacy within one monolithic tool so the right thing to do  
is to break it down piece by piece. The first piece is naturally the GUI builder.

Localization, theming, image management etc. should slowly separate into smaller tools. This is easier to do  
if we can just modularize the pieces thru a simple command line interface & as a side advantage it also makes  
it easier for you guys to automate some of your recurring tasks.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
