---
title: Github Migration Completed
slug: github-migration-completed
url: /blog/github-migration-completed/
original_url: https://www.codenameone.com/blog/github-migration-completed.html
aliases:
- /blog/github-migration-completed.html
date: '2015-03-28'
author: Shai Almog
---

![Header Image](/blog/github-migration-completed/github-logo.jpg)

Its been a long weekend, but now that its finally over the long and tedious migration to github is almost completely  
behind us.  
Make sure to update your URLs to the new repository at  
<https://github.com/codenameone/CodenameOne/>.  
There are still some references to the old google code repository in the plugins etc. so you should update  
them when we release the next update. Furthermore, if you used our custom update centers make sure your  
update center doesn’t reference a googlecode URL! 

### New Structure

Since github encourages smaller repositories we decided it might be a good idea to split up some non-core  
pieces of the project. So we moved the demos to the  
[codenameone-demos](https://github.com/codenameone/codenameone-demos)  
project and the device skin UI resources were moved to  
[codenameone-skins](https://github.com/codenameone/codenameone-skins).  
We also removed some big directories containing the javadocs, repositories etc. Those are all mapped to  
server locations on codenameone.com now. 

Another big change is the removal of all the jar, zip and other dependencies that might be required for the  
project build. These were major size contributors and allowed us to bring the project size down to a very manageable  
size. Right now we are still mapping the files that are “really needed” vs. the ones that just should never  
have been committed.  
We created a new project called  
[cn1-binaries](https://github.com/codenameone/cn1-binaries) that contains  
all those binaries. You will need to have it on the same level as the main cn1 project for everything to compile  
properly. I’m not sure if this is OK with the github guidelines if not we will find another solution. 

### The Migration

The migration was remarkably painful, the manual issue export code from Google didn’t work on Mac OS and there  
were no public workaround. Its a Python script that assumes the users are familiar with python environments  
in a typical Google hackish sort of way. Eventually I just purchased a droplet instance from Digital Ocean and  
performed the migration in the cloud, this has the benefit of using a very fast networking pipeline which made a  
lot of the work way faster even though I was working on a remote shared instance. It also made the python code  
work since for things such as this Linux instructions are easier than the Mac OS instructions. 

Some issue meta-data didn’t transfer well, they are all marked as if I added them and then added all the comments  
which is pretty lame for a migration script. So you should make sure to signup to the issues you submitted and  
make sure everything you provided is still there (e.g. test cases). 

Source migration was even more painful, it took roughly 15 attempts to get it right. The git-svn scripts all failed badly on  
fetching the repository during the GC phase and the only way we were eventually able to do this was thru  
the direct svn command like this: 
    
    
    git svn clone https://codenameone.googlecode.com/svn/trunk -A .authors.txt . --no-metadata

Notice that we used the `--no-metadata` tag which is discouraged, it just didn’t work otherwise. We also  
pointed to the trunk thus discarding branch and tag data since we didn’t see any other way of getting this to work… 

Unfortunately, this was only the beginning. We ended up with a 7.4gb workspace which is obviously way too much…  
Following the instructions to remove directories and all their history we ended up with a 10gb repository  
(seriously…). Pruning data from a version control system is always difficult (this isn’t a GIT specific issue).  
Despite running the git gc with the aggressive and prune flag the size was still pretty huge. 

Eventually the only thing that worked in shrinking the repository was to clone it to a new location using  
the file: URL path e.g.: 
    
    
    git clone file:///path/project.git .

Notice the . in the end indicating the empty current directory as the destination.  
Once this was done we were able to split the repository to smaller repos and now we have the new  
structure above while maintaining as much history as possible.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
