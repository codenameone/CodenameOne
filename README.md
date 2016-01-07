# Codename One
## Write Once Run Anywhere Native Mobile Apps Using Java

<img align="right" src="http://codenameone.com/img/phones.png" height="250">

Codename One allows Java developers to write their app once and have it work on all mobile devices (iOS, Android etc.). 
It features a simulator, designer (visual theme/builder) and ports to multiple OS's.

Codename One is a mature open source project and its roots go back to Sun Microsystems (2006) where one of its core underlying components was developed and open sourced. 

This open source project includes the libraries, native OS ports, resource editor & native VM code for Codename One. It also includes themes and there are separate related projects containing [demos](https://github.com/codenameone/codenameone-demos/), [skins](https://github.com/codenameone/codenameone-skins) etc.

Codename One can be extended easily using 3rd party libraries that can include native OS code there is an extensive list of these libraries (cn1libs) [here](https://www.codenameone.com/cn1libs.html).

You can learn more about Codename One and its capabilities at the [main site](http://www.codenameone.com) and you can read 
additional documentation [here](https://www.codenameone.com/getting-started.html).

You can also check out this introduction to Codename One video:

[![Introducing Codename One](http://img.youtube.com/vi/r6VO3zaBJGY/0.jpg)](http://www.youtube.com/watch?v=r6VO3zaBJGY "Introducing Codename One")


## Setup & Getting Started With The Code

Setup is covered in depth in [this article and video](https://www.codenameone.com/blog/how-to-use-the-codename-one-sources.html). Notice that this covers debugging the simulator and working with the code for that which requires the Codename One plugin for NetBeans. You can install that by installing NetBeans and typing "Codename One" in the plugin search section see [the getting started tutorial](https://www.codenameone.com/getting-started.html).

[![Using The Codename One Source Code](http://img.youtube.com/vi/2nD75pODPWk/0.jpg)](http://www.youtube.com/watch?v=2nD75pODPWk "Using The Codename One Source Code")

While Codename One itself works with all major IDE's the code in this repository was designed to work with NetBeans (ex-Sun guys). 

<img src="http://codenameone.com/img/NetBeans-logo.png" width="120">

<img src="http://codenameone.com/img/intellij_idea-logo.png" width="120">

<img src="http://codenameone.com/img/eclipse-logo.png" width="120">

## ParparVM
Codename One's iOS VM is quite unique and is open source as well. You can read more about it [in its dedicated folder in this repository](https://github.com/codenameone/CodenameOne/tree/master/vm).

ParparVM is a uniquely conservative VM that translates Java bytecode to C code. Thus providing native performance and access while still providing a safety net. This approach is unique to Codename One and is essential for future compatibility!

Apple has a tendency to change things abruptly e.g. 64bit support, bitcode etc. Since ParparVM just generates a standard Xcode project there were no code changes required for any of these tectonic shifts. It's as if you handcoded the project yourself!

More traditional compilers fall flat in these cases, furthermore they can't use Apple's native tools to their full extent e.g. profilers etc.

## Getting Help & FAQ

<img align="right" src="http://codenameone.com/img/blog/new_icon.png" height="250">
We provide support over at [StackOverflow when you tag using codenameone](http://stackoverflow.com/tags/codenameone), you can ask anything there and we try to be pretty responsive. [The StackOverflow link](http://stackoverflow.com/tags/codenameone) also serves as an excellent community driven FAQ since it literally maps user questions to answers.

Codename One has a [discussion group](https://www.codenameone.com/discussion-forum.html) where you can post questions. However, due to the nature of that group we try to limit discussions over the source. The discussion forum is intended for simpler usage and more complex source code hacks/native compilation might create noise there.
