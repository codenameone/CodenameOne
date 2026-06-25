package com.codenameone.examples.hellocodenameone.tests

import com.codename1.car.Car
import com.codename1.car.CarApplication
import com.codename1.car.CarContext
import com.codename1.car.CarGridItem
import com.codename1.car.CarGridTemplate
import com.codename1.car.CarListTemplate
import com.codename1.car.CarRow
import com.codename1.car.CarScreen
import com.codename1.car.CarTemplate
import com.codename1.ui.CN
import com.codename1.ui.Label
import com.codename1.ui.layouts.BoxLayout

/**
 * On-device (emulator / simulator) integration coverage for the portable in-car API
 * (`com.codename1.car`). CI has no connected head unit, so this exercises the parts that DO run on a
 * bare device: the native `Display.isCarConnected()` / `getCarBridge()` chain (iOS
 * `IOSNative.isCarPlayConnected`, Android `AndroidCarSupport`), graceful no-op behaviour, and the
 * cross-platform template model. It proves the car natives/glue are linked and the API resolves on
 * the real platform, not just in the JVM unit test. A real head-unit run (CarPlay simulator / Android
 * Auto DHU) remains a manual step.
 */
class CarApiTest : BaseTest() {
    override fun runTest(): Boolean {
        // 1. With no head unit connected, the API must be an inert no-op.
        if (CN.isCarConnected()) {
            fail("CN.isCarConnected() must be false with no head unit connected")
            return false
        }
        if (Car.getCurrentContext() != null) {
            fail("Car.getCurrentContext() must be null with no active session")
            return false
        }

        // 2. Registration round-trips and does not touch the head unit.
        val app: CarApplication = object : CarApplication() {
            override fun onCreateRootScreen(context: CarContext): CarScreen = LibraryScreen()
        }
        Car.setApplication(app)
        if (Car.getApplication() !== app) {
            fail("Car.setApplication/getApplication did not round-trip")
            return false
        }

        // 3. The template model builds the expected tree (no CarContext needed).
        val listTemplate: CarTemplate = LibraryScreen().dispatchCreateTemplate()
        if (listTemplate !is CarListTemplate) {
            fail("Root screen did not produce a CarListTemplate")
            return false
        }
        if (listTemplate.title != "Library") {
            fail("List title mismatch: ${listTemplate.title}")
            return false
        }
        val rows = listTemplate.sections.flatMap { it.rows }
        if (rows.size != 2) {
            fail("Expected 2 rows, got ${rows.size}")
            return false
        }
        if (rows[0].title != "Albums" || !rows[0].isBrowsable) {
            fail("First row mismatch: ${rows[0].title} browsable=${rows[0].isBrowsable}")
            return false
        }

        val gridTemplate = GridScreen().dispatchCreateTemplate()
        if (gridTemplate !is CarGridTemplate || gridTemplate.items.size != 3) {
            fail("Grid template did not build 3 items")
            return false
        }

        // 4. Show a summary form so the screenshot suite captures a frame on the device.
        val form = createForm("Car API", BoxLayout.y(), "car-api")
        form.addAll(
                Label("In-car API on-device check"),
                Label("connected: ${CN.isCarConnected()}"),
                Label("list rows: ${rows.size}"),
                Label("grid items: ${gridTemplate.items.size}"),
                Label("PASS")
        )
        form.show()
        return true
    }

    private class LibraryScreen : CarScreen() {
        override fun onCreateTemplate(): CarTemplate =
                CarListTemplate().setTitle("Library")
                        .addRow(CarRow("Albums").setBrowsable(true))
                        .addRow(CarRow("Now Playing").setText("idle"))
    }

    private class GridScreen : CarScreen() {
        override fun onCreateTemplate(): CarTemplate =
                CarGridTemplate().setTitle("Browse")
                        .addItem(CarGridItem("Charts", null))
                        .addItem(CarGridItem("Genres", null))
                        .addItem(CarGridItem("New", null))
    }
}
