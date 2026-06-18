package com.codenameone.examples.hellocodenameone

import com.codename1.ui.CN

/**
 * Watch entry point ("watchMain") for the Apple Watch / Wear OS build slice,
 * declared via `codename1.watchMain` in codenameone_settings.properties next to
 * the phone `codename1.mainName` (HelloCodenameOne).
 *
 * It is a distinct lifecycle class - not the phone main - so the watch slice has
 * its own tree-shaking root even though it reuses the same screenshot suite. The
 * suite's individual tests adapt to the form factor at runtime via [CN.isWatch]
 * (e.g. AbstractGraphicsScreenshotTest emits four separate full-screen captures
 * on a watch instead of a cramped 2x2 grid), so a single application services
 * both form factors from two entry points.
 */
class HelloCodenameOneWatch : HelloCodenameOne() {
    override fun runApp() {
        System.out.println("CN1SS:INFO:watch entry HelloCodenameOneWatch isWatch=" + CN.isWatch())
        super.runApp()
    }
}
