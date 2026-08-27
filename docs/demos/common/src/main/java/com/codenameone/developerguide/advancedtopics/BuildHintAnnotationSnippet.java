package com.codenameone.developerguide.advancedtopics;

import com.codename1.annotations.buildhints.Android;
import com.codename1.annotations.buildhints.AndroidMinSdk;
import com.codename1.annotations.buildhints.Build;
import com.codename1.annotations.buildhints.DesktopBuild;
import com.codename1.annotations.buildhints.DesktopTitleBar;
import com.codename1.annotations.buildhints.Ios;
import com.codename1.annotations.buildhints.ThemeMode;
import com.codename1.annotations.buildhints.Toggle;

// tag::buildHintAnnotations[]
@Android(themeMode = ThemeMode.MODERN, minSdkVersion = AndroidMinSdk.API_24)
@Build(nativeTheme = ThemeMode.MODERN)
@DesktopBuild(titleBar = DesktopTitleBar.NATIVE, width = 1280, height = 800)
@Ios(themeMode = ThemeMode.MODERN,
     newStorageLocation = Toggle.ON,
     pods = {"Intercom", "AFNetworking"})
public class BuildHintAnnotationSnippet {
}
// end::buildHintAnnotations[]
