# Codename One - 使用 Java 或 Kotlin 构建跨平台原生应用

Codename One 是一个面向 Java 和 Kotlin 开发者的移动优先跨平台开发环境。它可以将 Java 字节码编译为原生操作系统可执行文件（iOS、Android、UWP 等）。Codename One 是一个完整的移动开发平台，提供虚拟机、模拟器、设计工具（可视化主题/构建器/CSS）、IDE 集成、多操作系统端口等。它通过可移植的抽象层提供对底层原生操作系统代码（如 Objective-C、C#、Dalvik/ART）的完整访问，实现 100% 代码复用。

## 🌟 Codename One 的独特优势

- **真正的 "一次编写，随处运行"**，无需特殊硬件，支持 100% 代码复用。
- **将 Java 或 Kotlin 编译为 iOS、UWP（通用 Windows 平台）、Android 甚至 JavaScript（支持无缝 PWA 和线程）**。
- **开源且免费，同时提供企业级商业服务**。
- **易于使用，提供 100% 可移植的拖放式 GUI 构建器**。
- **完整访问底层原生操作系统功能**，可使用本机编程语言（如 Objective-C）进行扩展，而不会影响可移植性。
- **完全掌控屏幕上的每个像素**，可以重写绘制逻辑或使用玻璃面板进行绘制。
- **支持原生控件（视图）**，可与 Codename One 组件混合使用（重量级/轻量级组件混合）。
- **开箱即用的无缝持续集成（CI）支持**。

## ✨ Codename One 的具体优势

### **Codename One 的模拟器即时运行**

不同于 Android 等平台的模拟器，Codename One 采用模拟器，启动速度快，即使在调试时也能迅速运行。同时支持 IDE 功能，如实时代码热重载，可在运行时修改代码。

这意味着更快的调试周期和更快的开发进程！

### **丰富的设备皮肤选择**

选择各种设备 "皮肤"，查看应用在不同设备上的外观。皮肤会考虑分辨率和设备密度，提供像素级精确的呈现效果。切换设备皮肤几乎是即时的。

您可以在[这里](https://github.com/codenameone/)编辑和贡献设备皮肤。

### **交互式控制台**

使用交互式 Groovy 控制台在运行时与应用 API 进行交互，检查应用状态或实验性修改代码。

这样，您无需重新编译就能进行问题排查和实验。

### **实时代码重载**

模拟器支持 IntelliJ 的 "Reload Changed Classes" 功能（在 NetBeans 中称为 "Apply Code Changes"）。这样，您对 Java 源代码的修改会立即生效，无需重启应用。

### **CSS 实时更新**

修改 CSS 样式表后，模拟器会立即反映更改，包括主题、图片、字体等，所有修改在保存时即时更新，无需刷新或重新加载。

这让应用的样式调整变得极为高效！

### **组件检查器**

使用强大的组件检查器浏览 UI 组件层级结构，帮助找出多余的间距或未对齐的元素。您可以在运行时更改 UIID（选择器），查看其对 UI 的影响。

### **网络监视器**

查看应用的所有网络请求，检查 HTTP 请求的头部和主体，还可以模拟慢速网络环境。

### **UI 单元测试录制**

使用测试录制工具录制应用的 UI 单元测试，并集成到 CI 过程中，实现自动化设备测试。

## 🛠 Codename One 的工作原理

Codename One 是一个成熟的开源项目，其核心组件可追溯至 Sun Microsystems（2006 年）。

Codename One 应用的表现与原生应用无异，因为它们是真正的原生应用。

Codename One 使用目标平台的官方构建工具将代码静态编译为原生二进制文件。

- 在不原生支持 Java 的平台（如 iOS）上，Codename One 先将 JVM 字节码转换为本机代码，如 iOS 平台转换为 C 代码，并生成标准 Xcode 项目。
- 在 Android 上，Java 本身受到支持，因此应用 jar 文件直接打包进 Android Studio Gradle 项目，可直接使用 Android SDK 构建。

## 🚀 快速入门

目前，我们正在迁移到 Maven，推荐使用新的[项目创建工具](https://start.codenameone.com)。

### **Maven 快速入门**

```sh
git clone https://github.com/codenameone/CodenameOne
cd CodenameOne/maven
mvn install
```

此过程可能较长，因为会自动下载约 1GB 的依赖项。

如果要构建 archetype 项目，还需执行：

```sh
git clone https://github.com/shannah/cn1-maven-archetypes
cd cn1-maven-archetypes
mvn install
```

下载并解压项目后，在 `pom.xml` 文件中找到 `<cn1.version>` 和 `<cn1.plugin.version>` 属性，并将其修改为本地安装的 SNAPSHOT 版本（如 `7.0.21-SNAPSHOT`）。

### **Ant 快速入门**

```sh
git clone https://github.com/codenameone/CodenameOne
cd CodenameOne
ant
```

#### **运行单元测试**

```sh
ant test-javase
```

#### **运行示例**

```sh
ant samples
```

## 🔍 ParparVM

Codename One 的 iOS 虚拟机 ParparVM 采用保守的转换策略，将 Java 字节码转换为 C 代码，提供原生性能和安全性。

由于 Codename One 生成标准 Xcode 项目，因此 Apple 任何架构更改（如 64 位支持、bitcode）都无需代码修改。

## 🛠 iKVM 修改版

Codename One 维护了 iKVM 的一个分支，允许我们在 Windows 10 设备上原生运行 Codename One 的 UWP 实现。

## 📚 重要资源

- [JavaDoc](https://www.codenameone.com/javadoc.html)
- [开发者指南（HTML & PDF）](https://www.codenameone.com/documentation.html)
- [Codename One 工作原理（StackOverflow）](https://stackoverflow.com/)
- [Codename One 学院](https://www.codenameone.com/academy.html)
- [官方博客](https://www.codenameone.com/blog.html)
- [社区论坛](https://groups.google.com/forum/#!forum/codenameone-discussions)

## ❓ 获取帮助

我们在 StackOverflow 提供支持，使用 `codenameone` 标签提问即可。您还可以在[讨论组](https://groups.google.com/forum/#!forum/codenameone-discussions)进行交流。

---

Codename One 让移动应用开发变得更快、更简单，立即开始您的跨平台开发之旅吧！
