<h1 align=center>
 <img align=center width="100%" src="https://www.codenameone.com/github/CN1-Banner-Dark-Blue.jpg" />
</h1>

![GitHub repo size](https://img.shields.io/github/repo-size/codenameone/CodenameOne?style=flat-square)
![GitHub top language](https://img.shields.io/github/languages/top/codenameone/CodenameOne?color=orange&style=flat-square)
![GitHub last commit](https://img.shields.io/github/last-commit/codenameone/CodenameOne?color=success&style=flat-square)
![GitHub license](https://img.shields.io/badge/license-GPL%20%2B%20CE-FFFF00?style=flat-square)
[![GitHub Stars](https://img.shields.io/github/stars/codenameone/CodenameOne?label=GitHub%20stars&style=social)](https://github.com/codenameone/CodenameOne/stargazers/)

## 100% native. Fast. Full-stack Java.

[Codename One](https://www.codenameone.com/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=overview) takes Java from your UI to your server, compiled to native code. Share models and typed APIs across the full stack, control the UI you ship, and deploy your server without a JVM. Target iOS, Android, desktop, watch and TV, with a JavaScript port for the web. Published benchmarks below compare runtime performance with HotSpot, Spring and Go.

The UI ships with your app, so you control its components and theme. Native interfaces and peer components give you access to platform SDKs and views. The framework is GPLv2 with the Classpath Exception: free for commercial applications, with no royalties.

| What do you want to build? | Start here |
| --- | --- |
| A new Java app | [Generate a project](https://www.codenameone.com/initializr/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=app-start) or [try the browser playground](https://www.codenameone.com/playground/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=playground) |
| A native Java service | [Build a backend](https://www.codenameone.com/developer-guide/backend/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=backend) with Spring-style APIs and no JVM in the deployed executable |
| An app and its backend | [Share models and generated REST clients](https://www.codenameone.com/blog/java-backend-shared-models/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=shared-models) |
| An existing Android app on more platforms | [Import classic Android application code](https://www.codenameone.com/developer-guide/android-interop/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=android-import), including activities, XML layouts and resources |
| Evidence before adopting | [Compare performance](#measured-performance) and [inspect platform support](https://www.codenameone.com/port-status/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=platforms) |

### A backend that compiles the wiring into your application

Write controllers, dependency injection, transactions and scheduled jobs with Spring-style annotations. The build generates routing and wiring as ordinary code, then ParparVM translates Java bytecode to C and the native toolchain compiles the executable. Develop and debug on the JVM; deploy a native service without it.

The backend includes PostgreSQL, MySQL/MariaDB and SQLite access, managed ORM, WebSockets, sessions, an OAuth2/OIDC security stack, database migrations, OpenTelemetry and MCP tools. Initializr offers **App with backend** and **Backend only** project types.

This is an evolving backend with its own supported API surface. Its annotations live in `com.codename1.backend.annotations`; it is not a drop-in Spring Boot replacement and does not run arbitrary JVM libraries. Start with the [backend guide](https://www.codenameone.com/developer-guide/backend/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=backend-guide) and its compatibility limits.

### Bring classic Android apps to iOS, desktop and the web

Import an Android Studio module with `cn1:import-android-project`, or place its sources in `common/src/main/android`. The compatibility layer implements supported `android.*` and AndroidX APIs over Codename One, including activities, fragments, XML layouts, RecyclerView, ConstraintLayout and Material Components. Java and Kotlin sources are supported.

The application runs through Codename One's ports without an Android runtime on the other platforms. This supports the documented classic Android API surface, not every Android library or framework behavior. Check the [supported APIs, side-by-side captures and limitations](https://www.codenameone.com/developer-guide/android-interop/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=android-coverage), particularly reflection, Parcel and SQLite concurrency, against your application's screens and data flows.

### Measured performance

These are published reference measurements, not a promise about every application. Each comparison uses a different workload; follow the links for hardware, commands and raw results.

| Comparison | Recorded result | Scope |
| --- | --- | --- |
| HotSpot / JDK 25 | **37.5% less elapsed time and 45.8% less peak memory** for ParparVM self-translation | Linux ARM64 Neoverse N2 baseline, seven calibration runs, identical generated files. [All machines and workloads](https://www.codenameone.com/blog/parparvm-four-byte-header/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=performance-hotspot). Allocation-heavy code can favor HotSpot. |
| Spring Boot 4.1.1 / JDK 25 | **3.9× plaintext and 3.4× JSON throughput** | Two pinned server CPUs in a Linux ARM64 VM on an M4 Max, 32 connections, matched 60-second warmup. Lower-level CN1 HTTP handlers versus Spring MVC/Tomcat; no database, TLS or authentication. [Methodology and reproduction bundle](https://www.codenameone.com/blog/java-server-work-before-startup/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=performance-spring). |
| Go / fasthttp | **595,610 vs 496,293 requests/s** | Separate plaintext test: CN1 native musl, two pinned cores, 64 connections, medians of three interleaved runs. Go used less memory and a smaller binary. [Full comparison](https://www.codenameone.com/developer-guide/backend-operations/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=performance-go). |

The Spring comparison measures selected HTTP stacks, not equivalent framework features or the cost of CN1's annotation-generated controllers. The HotSpot comparison measures our compiler workload; the linked results also show workloads where HotSpot is faster. Measure your own service before sizing a deployment.

### Build and test the whole application

- **UI you control:** CSS themes, Liquid Glass and Material 3, layouts, animation, vector graphics and custom drawing. Embed platform views where needed.
- **Shared app and server code:** typed REST contracts, generated clients, entities and persistence APIs, with platform-specific code kept behind explicit boundaries.
- **Local development:** simulator, device skins, component and network inspectors, live CSS, JUnit and screenshot tests. Maven and Gradle tooling are available.
- **Coding-agent tools:** generated project instructions and semantic MCP tools for the simulator and JavaSE-hosted tooling.
- **Device integration:** camera, notifications, maps, Bluetooth LE, biometrics, secure storage and native SDK access.
- **Native build options:** use local toolchains or the optional cloud service, including iOS builds from Windows or Linux. The free cloud tier includes 100 build credits per month.

### Supported targets

| Form factor | Targets |
| --- | --- |
| Mobile | Android and iOS |
| Desktop | Native Windows, Linux and macOS; JVM desktop applications |
| Web | JavaScript applications and installable PWAs |
| TV | Apple TV (tvOS) and Android TV / Google TV |
| Watch | Apple Watch (watchOS) and Wear OS |
| Vehicle | Apple CarPlay and Android Auto integrations |
| Server | Native Java backend; JVM development mode |

Check [Port Status](https://www.codenameone.com/port-status/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=port-status) for each target's architecture, OS requirements and test coverage.

## How does it work?

ParparVM translates reachable JVM bytecode into C for the native Apple, Windows, Linux and backend targets. The platform compiler builds the generated code together with the runtime. Android applications use the Android toolchain; the JavaScript port produces browser applications. JavaSE powers the JVM desktop target and simulator.

Codename One draws its portable UI rather than wrapping every platform widget. Native interfaces and `PeerComponent` connect it to platform SDKs and views. The Android compatibility layer compiles supported Android resources and API calls into this same application model.

## Quick start and documentation

[Generate a project](https://www.codenameone.com/initializr/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=quick-start), choose an app, app with backend, or backend-only project, and follow the [getting-started guide](https://www.codenameone.com/getting-started/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=getting-started).

- [Developer guide](https://www.codenameone.com/developer-guide/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=developer-guide)
- [Client Javadoc](https://www.codenameone.com/javadoc/) and [backend Javadoc](https://www.codenameone.com/backend/javadoc/)
- [Libraries and native integrations](https://www.codenameone.com/cn1libs/)
- [Engineering blog](https://www.codenameone.com/blog/?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=engineering)
- [GitHub Discussions](https://github.com/codenameone/CodenameOne/discussions)

## Setup & Getting Started With The Code

The setup is covered in depth in [this article and video](https://www.codenameone.com/blog/building-codename-one-from-source-maven-edition.html). 

<div>
  <a href="https://www.youtube.com/watch?v=H8-QMIsTHNc " target="_blank"><img src="https://i.imgur.com/X0xzM6H.jpg" alt="Building Codename One from Source - Maven Edition
" img width="80%"> </a>
</div>

**IMPORTANT:** *Building* the Codename One framework from source requires **JDK 8** -- some sub-modules must use `-source 1.5` and `-target 1.5` to maintain backward compatibility with parts of the toolchain, and newer JDKs cannot emit those targets.

*Running* a Codename One application (the simulator or the "Run as desktop app" target) supports **JDK 11 through 25** (Eclipse Temurin: <https://adoptium.net>).


### Quick Start with Maven

~~~~
git clone https://github.com/codenameone/CodenameOne
cd CodenameOne/maven
mvn install -Plocal-dev-javase
~~~~

NOTE: The `-Plocal-dev-javase` profile is necessary for building the javase port.  Without it, you'll get build errors.

This will build and install Codename One in your local Maven repository, including the `cn1app-archetype` and `cn1lib-archetype` Maven archetypes. This process can take a while since it automatically downloads dependencies with a size of ~1GB.

Now that Codename One is installed in your local Maven repository, you can use that version in a project instead of the release version.
A new testing project can be quickly generated with the [Codename One initializr](https://start.codenameone.com).

After downloading and extracting the project, open its pom.xml file and and look for the `<cn1.version>` and `<cn1.plugin.version>` properties.
Then change these to point to the version that got installed into your *local* maven repository by `mvn install -Plocal-dev-javase`. The locally built version will usually be a SNAPSHOT version (e.g. 7.0.21-SNAPSHOT).


### Quick Start with Ant

**Getting and Building Sources**

~~~~
$ git clone https://github.com/codenameone/CodenameOne
$ cd CodenameOne
$ ant
~~~~

**Running Unit Tests**

~~~~
$ ant test-javase
~~~~

**Running Samples**

The Samples directory contains a growing set of sample applications.  These samples aren't meant to be demos, but rather samples of how to use APIs.

You can launch the sample runner app from the command-line using:

~~~
$ ant samples
~~~


## ParparVM
Codename One's native compiler is open source. You can read more about it [in its dedicated folder in this repository](https://github.com/codenameone/CodenameOne/tree/master/vm).

ParparVM translates Java bytecode to portable C, performs reachability analysis to remove unused code, and then hands the generated project to the target's native compiler. It powers the native Apple, Windows, Linux and backend targets.

You can open the generated native project and use the platform's debugger and profiler directly. On Apple platforms, for example, the output is a standard Xcode project with readable call stacks and native performance tooling.


## Help Improve Codename One

<img align="right" src="http://codenameone.com/github/new_icon.png" height="150">

Outside pull requests are disabled because even a small framework change can interact with the repository's cross-platform build and screenshot pipelines. The maintainers integrate code changes after running that matrix.

You can still materially improve the project:

- Ask usage and API-design questions in [GitHub Discussions](https://github.com/codenameone/CodenameOne/discussions).
- File [GitHub Issues](https://github.com/codenameone/CodenameOne/issues/new/choose) for reproducible bugs, performance counterexamples, toolchain compatibility problems, and documentation gaps.
- Include a minimal project, the affected target, Codename One and JDK versions, complete logs, and screenshots where they help.
- Challenge the published benchmarks and architecture. A counterexample we can reproduce is more useful than a general feature request.

Read [How to Help Improve Codename One](CONTRIBUTING.md) before opening a report. The [`codenameone` tag on Stack Overflow](https://stackoverflow.com/tags/codenameone) also contains years of community questions and answers.

<br>
  
## Project Contributors

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/beazl-peter"><img src="https://avatars.githubusercontent.com/u/68695557?v=4?s=100" width="100px;" alt="beazl-peter"/><br /><sub><b>beazl-peter</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=beazl-peter" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/liannacasper"><img src="https://avatars.githubusercontent.com/u/67953602?v=4?s=100" width="100px;" alt="liannacasper"/><br /><sub><b>liannacasper</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=liannacasper" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/sergeyCodenameOne"><img src="https://avatars.githubusercontent.com/u/69102702?v=4?s=100" width="100px;" alt="sergeyCodenameOne"/><br /><sub><b>sergeyCodenameOne</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=sergeyCodenameOne" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ThomasH99"><img src="https://avatars.githubusercontent.com/u/16265939?v=4?s=100" width="100px;" alt="ThomasH99"/><br /><sub><b>ThomasH99</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=ThomasH99" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://www.groupsapp.online"><img src="https://avatars.githubusercontent.com/u/11293898?v=4?s=100" width="100px;" alt="Javier Anton"/><br /><sub><b>Javier Anton</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=javieranton-zz" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://diamonddevgroup.com"><img src="https://avatars.githubusercontent.com/u/7268931?v=4?s=100" width="100px;" alt="Diamond"/><br /><sub><b>Diamond</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=diamondobama" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://www.informatica-libera.net/"><img src="https://avatars.githubusercontent.com/u/1997316?v=4?s=100" width="100px;" alt="Francesco Galgani"/><br /><sub><b>Francesco Galgani</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=jsfan3" title="Code">💻</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/kutoman"><img src="https://avatars.githubusercontent.com/u/5825645?v=4?s=100" width="100px;" alt="kutoman"/><br /><sub><b>kutoman</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=kutoman" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ramsestom"><img src="https://avatars.githubusercontent.com/u/636758?v=4?s=100" width="100px;" alt="ramsestom"/><br /><sub><b>ramsestom</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=ramsestom" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Maaartinus"><img src="https://avatars.githubusercontent.com/u/2324516?v=4?s=100" width="100px;" alt="Maaartinus"/><br /><sub><b>Maaartinus</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=Maaartinus" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/DurankGts"><img src="https://avatars.githubusercontent.com/u/16245755?v=4?s=100" width="100px;" alt="Durank"/><br /><sub><b>Durank</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=DurankGts" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://boardspace.net/"><img src="https://avatars.githubusercontent.com/u/5963076?v=4?s=100" width="100px;" alt="ddyer0"/><br /><sub><b>ddyer0</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=ddyer0" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/carlosverdier"><img src="https://avatars.githubusercontent.com/u/14301433?v=4?s=100" width="100px;" alt="carlosverdier"/><br /><sub><b>carlosverdier</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=carlosverdier" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Firethunder"><img src="https://avatars.githubusercontent.com/u/1608647?v=4?s=100" width="100px;" alt="Robert Edelmann"/><br /><sub><b>Robert Edelmann</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=Firethunder" title="Code">💻</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Adalbert393"><img src="https://avatars.githubusercontent.com/u/18614910?v=4?s=100" width="100px;" alt="Adalbert393"/><br /><sub><b>Adalbert393</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=Adalbert393" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://sjhannah.com"><img src="https://avatars.githubusercontent.com/u/2677562?v=4?s=100" width="100px;" alt="Steve Hannah"/><br /><sub><b>Steve Hannah</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=shannah" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/digappsepp"><img src="https://avatars.githubusercontent.com/u/32707062?v=4?s=100" width="100px;" alt="digappsepp"/><br /><sub><b>digappsepp</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=digappsepp" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Pavneet-Sing"><img src="https://avatars.githubusercontent.com/u/11755381?v=4?s=100" width="100px;" alt="Pavneet Singh"/><br /><sub><b>Pavneet Singh</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=Pavneet-Sing" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/vprise"><img src="https://avatars.githubusercontent.com/u/16166226?v=4?s=100" width="100px;" alt="vprise"/><br /><sub><b>vprise</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=vprise" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://jrmydev.000webhostapp.com/"><img src="https://avatars.githubusercontent.com/u/10810617?v=4?s=100" width="100px;" alt="JrmyDev"/><br /><sub><b>JrmyDev</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=JrmyDev" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://csdesigninc.ca"><img src="https://avatars.githubusercontent.com/u/1958073?v=4?s=100" width="100px;" alt="Terry Wilkinson"/><br /><sub><b>Terry Wilkinson</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=twilkinson" title="Code">💻</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/jaanushansen"><img src="https://avatars.githubusercontent.com/u/11716510?v=4?s=100" width="100px;" alt="Jaanus Hansen"/><br /><sub><b>Jaanus Hansen</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=jaanushansen" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/jegesh"><img src="https://avatars.githubusercontent.com/u/6535446?v=4?s=100" width="100px;" alt="Yaakov Gesher"/><br /><sub><b>Yaakov Gesher</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=jegesh" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Munken"><img src="https://avatars.githubusercontent.com/u/773660?v=4?s=100" width="100px;" alt="Michael Munch"/><br /><sub><b>Michael Munch</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=Munken" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/saeder"><img src="https://avatars.githubusercontent.com/u/9945131?v=4?s=100" width="100px;" alt="saeder"/><br /><sub><b>saeder</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=saeder" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://neptunedreams.com"><img src="https://avatars.githubusercontent.com/u/19262903?v=4?s=100" width="100px;" alt="Miguel Muñoz"/><br /><sub><b>Miguel Muñoz</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=SwingGuy1024" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://ahmedengu.com"><img src="https://avatars.githubusercontent.com/u/2976004?v=4?s=100" width="100px;" alt="Ahmed Aboumalwa"/><br /><sub><b>Ahmed Aboumalwa</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=ahmedengu" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/FabioConceicao"><img src="https://avatars.githubusercontent.com/u/13354592?v=4?s=100" width="100px;" alt="Fabio"/><br /><sub><b>Fabio</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=FabioConceicao" title="Code">💻</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="http://forann.eu"><img src="https://avatars.githubusercontent.com/u/12081628?v=4?s=100" width="100px;" alt="Piotr"/><br /><sub><b>Piotr</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=PiotrZub" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://mat2095.de"><img src="https://avatars.githubusercontent.com/u/11258252?v=4?s=100" width="100px;" alt="Matthias Bay"/><br /><sub><b>Matthias Bay</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=Mat2095" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/sannysanoff"><img src="https://avatars.githubusercontent.com/u/952071?v=4?s=100" width="100px;" alt="Sanny Sanoff"/><br /><sub><b>Sanny Sanoff</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=sannysanoff" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/McSym28"><img src="https://avatars.githubusercontent.com/u/8185872?v=4?s=100" width="100px;" alt="McSym28"/><br /><sub><b>McSym28</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=McSym28" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://ericleong.me"><img src="https://avatars.githubusercontent.com/u/1572011?v=4?s=100" width="100px;" alt="Eric Leong"/><br /><sub><b>Eric Leong</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=ericleong" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://davidday.tw/"><img src="https://avatars.githubusercontent.com/u/47077427?v=4?s=100" width="100px;" alt="David Day"/><br /><sub><b>David Day</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=dj6082013" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Rocketeer007"><img src="https://avatars.githubusercontent.com/u/11492464?v=4?s=100" width="100px;" alt="Nick Price"/><br /><sub><b>Nick Price</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=Rocketeer007" title="Code">💻</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ahnafbinazad"><img src="https://avatars.githubusercontent.com/u/66205903?v=4?s=100" width="100px;" alt="Ahnaf Bin Azad"/><br /><sub><b>Ahnaf Bin Azad</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=ahnafbinazad" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/OctavioAnino"><img src="https://avatars.githubusercontent.com/u/114261436?v=4?s=100" width="100px;" alt="Octavio E Anino"/><br /><sub><b>Octavio E Anino</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=OctavioAnino" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://linktr.ee/yashpimple"><img src="https://avatars.githubusercontent.com/u/97302447?v=4?s=100" width="100px;" alt="Yash Pimple"/><br /><sub><b>Yash Pimple</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=YashPimple" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Wninayyds"><img src="https://avatars.githubusercontent.com/u/90488923?v=4?s=100" width="100px;" alt="Nina"/><br /><sub><b>Nina</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=Wninayyds" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/FercueNat"><img src="https://avatars.githubusercontent.com/u/113535859?v=4?s=100" width="100px;" alt="FercueNat"/><br /><sub><b>FercueNat</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=FercueNat" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ImmediandoSrl"><img src="https://avatars.githubusercontent.com/u/172423330?v=4?s=100" width="100px;" alt="ImmediandoSrl"/><br /><sub><b>ImmediandoSrl</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=ImmediandoSrl" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/davideprimasc"><img src="https://avatars.githubusercontent.com/u/159039808?v=4?s=100" width="100px;" alt="davideprimasc"/><br /><sub><b>davideprimasc</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=davideprimasc" title="Code">💻</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/DB107"><img src="https://avatars.githubusercontent.com/u/154587979?v=4?s=100" width="100px;" alt="DB107"/><br /><sub><b>DB107</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=DB107" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://speakerdeck.com/eltociear"><img src="https://avatars.githubusercontent.com/u/22633385?v=4?s=100" width="100px;" alt="Ikko Eltociear Ashimine"/><br /><sub><b>Ikko Eltociear Ashimine</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=eltociear" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/SamC1832js"><img src="https://avatars.githubusercontent.com/u/79888848?v=4?s=100" width="100px;" alt="Sam C"/><br /><sub><b>Sam C</b></sub></a><br /><a href="https://github.com/codenameone/CodenameOne/commits?author=SamC1832js" title="Code">💻</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This historical list recognizes people who contributed code and documentation before outside pull requests were disabled. Current participation happens through [issues and discussions](CONTRIBUTING.md).
