---
title: 'TIP: Edit the Docs for Fun and Profit'
slug: tip-edit-docs-fun-profit
url: /blog/tip-edit-docs-fun-profit/
original_url: https://www.codenameone.com/blog/tip-edit-docs-fun-profit.html
aliases:
- /blog/tip-edit-docs-fun-profit.html
date: '2017-07-09'
author: Shai Almog
---

![Header Image](/blog/tip-edit-docs-fun-profit/tip.jpg)

On occasion I answer a question on stackoverflow, the discussion forum or elsewhere and I get a response of the form of: “this wasn’t clear from the docs”. We improved the docs but befitting a huge project run by engineers without a technical writer in sight this is a bit of a problem…​ The thing is that this is a problem you can fix regardless of your level in engineering or in English.

The benefits are pretty cool:

  * You have a pull request in your name

  * Documenting stuff helps understand the nuance and makes you a better programmer

  * We would really appreciate it

That’s a lot for something that’s so trivial to accomplish!

### Fixing the JavaDocs

Fixing the JavaDoc for a class couldn’t be easier. You need a github account (which is free) you can then go to <https://github.com/codenameone/CodenameOne> and type in the search bar the name of the file you want to fix in the javadoc e.g. if you want to improve the JavaDoc for component just type: `filename:Component.java`

Include the `filename:` prefix for a quicker find…​

You should see something like this:

![List of component classes the first one is what we are looking for](/blog/tip-edit-docs-fun-profit/edit-docs-search-component.png)

Figure 1. List of component classes the first one is what we are looking for

When we enter we first must select the master branch in the combo box then we can press the edit button on the top right:

![The combo box to select the master branch and edit button for the source file](/blog/tip-edit-docs-fun-profit/edit-docs-edit-button.png)

Figure 2. The combo box to select the master branch and edit button for the source file

You’ll get a couple of standard notices:

![Standard notices](/blog/tip-edit-docs-fun-profit/edit-docs-notices.png)

Figure 3. Standard notices

Find the method whose JavaDoc you want to edit and just edit it. Once you do that scroll all the way down and enter a description in the propose file change box and submit it:

![The propose change box](/blog/tip-edit-docs-fun-profit/edit-docs-propose-change.png)

Figure 4. The propose change box, make sure to describe your change briefly

Once you click propose change you can make it into a pull request and thus submit it to us. This takes two clicks on green buttons and literally nothing more.

![First stage of pull request you are prompted with the diff of the change you made](/blog/tip-edit-docs-fun-profit/edit-docs-pull-request-1.png)

Figure 5. First stage of pull request you are prompted with the diff of the change you made

![Second stage of pull request you can edit the submit comment](/blog/tip-edit-docs-fun-profit/edit-docs-pull-request-2.png)

Figure 6. Second stage of pull request you can edit the submit comment

We usually accept simple pull requests pretty quickly. Please feel free to submit. Once you go thru that process it becomes trivial and helps you dive deeper into the code.

You can see the pull request I did for the screenshots above here: <https://github.com/codenameone/CodenameOne/pull/2153>

### Edit the Developer Guide

This is actually easier. You can just open the developer guide and edit it without a pull request as it’s hosted in our project wiki!

You can open it [here](https://github.com/codenameone/CodenameOne/wiki) or by clicking the wiki tab in the Codename One github project. The edit button on the top right allows you to edit the document instantly and save it directly to the project.

The one hurdle is that the guide is written using asciidoc which is a simple language but if you aren’t familiar with it then it might take a couple of minutes to understand. You can just follow the conventions in other parts of the guide to get started.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — August 19, 2017 at 7:37 pm ([permalink](https://www.codenameone.com/blog/tip-edit-docs-fun-profit.html#comment-23761))

> Thank you Shai.  
> Are the wiki modifications by users automatically converted to the manual pages in the Codename One site without any check by the Codename One team?
>
> I’ve seen few errors and several incompleteness in the manual and in the API about the code examples, I’ll try to contribute the next time that I’ll find something strange.  
> Of course, I’m only an user of Codename One and I’m studying it, so at the moment I feel better to tell a problem in the “Issues page” of GitHub than to try to do changes by myself.
>



### **Shai Almog** — August 20, 2017 at 4:58 am ([permalink](https://www.codenameone.com/blog/tip-edit-docs-fun-profit.html#comment-23434))

> No. We monitor the edits and review every change. Then when we choose we sync it to the site.
>
> Feel free to edit or clarify, worst case scenario we’ll fix mistakes or omissions.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
