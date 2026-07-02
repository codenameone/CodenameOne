// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::testing-with-junit-java-001[]
package com.example.myapp;

import com.codename1.testing.junit.CodenameOneTest;
import com.codename1.testing.junit.RunOnEdt;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CodenameOneTest
class GreetingFormTest {

    @Test
    @RunOnEdt
    void formShowsExpectedTitle() {
        new Form("Hello").show();
        assertEquals("Hello", Display.getInstance().getCurrent().getTitle());
        assertTrue(CN.isEdt(), "@RunOnEdt method runs on the Codename One EDT");
    }
}
// end::testing-with-junit-java-001[]

// tag::testing-with-junit-java-002[]
@Test
@RunOnEdt                           // body runs on the EDT
void canMutateUiSafely() {
    Form f = new Form("X");
    f.add(new com.codename1.ui.Button("Tap"));
    f.show();                       // safe -- we are on the EDT
}

@Test
@RunOnEdt(timeoutMillis = 60000)    // default 30s; override for slow form builds
void slowFormBuild() { /* ... */ }
// end::testing-with-junit-java-002[]

// tag::testing-with-junit-java-003[]
@Test
@SimulatorProperty(name = "feature.flag", value = "on")
void appReadsFlagFromDisplayProperty() {
    assertEquals("on", Display.getInstance().getProperty("feature.flag", null));
}

@Test
@SimulatorProperties({
    @SimulatorProperty(name = "user.tier",   value = "pro"),
    @SimulatorProperty(name = "user.region", value = "eu")
})
void multipleAppProperties() { /* ... */ }
// end::testing-with-junit-java-003[]

// tag::testing-with-junit-java-004[]
@Test @Theme(nativeTheme = NativeTheme.IOS_MODERN)        void rendersUnderIosModern()    { /* ... */ }
@Test @Theme(nativeTheme = NativeTheme.IOS_FLAT)          void rendersUnderIosFlat()      { /* ... */ }
@Test @Theme(nativeTheme = NativeTheme.IPHONE_PRE_FLAT)   void rendersUnderClassicIos()   { /* ... */ }
@Test @Theme(nativeTheme = NativeTheme.ANDROID_MATERIAL)  void rendersUnderAndroid()      { /* ... */ }
@Test @Theme(nativeTheme = NativeTheme.ANDROID_HOLO_LIGHT) void rendersUnderHoloLight()   { /* ... */ }
@Test @Theme(nativeTheme = NativeTheme.ANDROID_LEGACY)    void rendersUnderAndroidLegacy(){ /* ... */ }
// end::testing-with-junit-java-004[]

// tag::testing-with-junit-java-005[]
@Test @Theme("/MyAppTheme.res")  void rendersAppTheme()  { /* ... */ }
// end::testing-with-junit-java-005[]

// tag::testing-with-junit-java-006[]
@Test @DarkMode                     void mainFormIsLegibleInDark()  { /* ... */ }
@Test @DarkMode(enabled = false)    void mainFormIsLegibleInLight() { /* ... */ }
// end::testing-with-junit-java-006[]

// tag::testing-with-junit-java-007[]
@CodenameOneTest
@LargerText                 // class-level: every test at 1.3x (AX2)
class AccessibilityTest {

    @Test void buttonsStillFit() { /* ... */ }

    @Test
    @LargerText(scale = 2.0f)    // method-level override: AX5
    void buttonsAtExtremeScale() { /* ... */ }
}
// end::testing-with-junit-java-007[]

// tag::testing-with-junit-java-008[]
@Test
@Orientation(Orientation.Value.LANDSCAPE)
void formStillFitsInLandscape() {
    assertFalse(Display.getInstance().isPortrait());
    // ... layout assertions ...
}
// end::testing-with-junit-java-008[]

// tag::testing-with-junit-java-009[]
@Test @RTL                          void mirrorsCorrectly()    { /* ... */ }
@Test @RTL(enabled = false)         void restoresLtrLayout()   { /* ... */ }
// end::testing-with-junit-java-009[]

// tag::testing-with-junit-java-010[]
@CodenameOneTest
@LargerText(scale = 1.3f)        // class default
@DarkMode                         // class default
class LayoutTest {

    @Test
    @LargerText(scale = 2.0f)     // overrides class -> 2.0x for this test
    void extremeScale() { /* runs at 2.0x scale + dark mode */ }

    @Test
    void defaultScale() { /* runs at 1.3x scale + dark mode */ }
}
// end::testing-with-junit-java-010[]

// tag::testing-with-junit-java-011[]
public class LoginNavTest extends com.codename1.testing.AbstractTest {
    @Override public boolean shouldExecuteOnEDT() { return true; }

    @Override public boolean runTest() throws Exception {
        new MyApp().runApp();
        TestUtils.waitForFormTitle("Login");
        TestUtils.setText("usernameField", "alice");
        TestUtils.clickButtonByLabel("Sign In");
        TestUtils.waitForFormTitle("Home");
        return TestUtils.screenshotTest("home-screen");
    }
}
// end::testing-with-junit-java-011[]

// tag::testing-with-junit-java-012[]
@CodenameOneTest
class LoginNavTest {

    @Test
    @RunOnEdt
    void signingInLandsOnHome() throws Exception {
        new MyApp().runApp();
        TestUtils.waitForFormTitle("Login");
        TestUtils.setText("usernameField", "alice");
        TestUtils.clickButtonByLabel("Sign In");
        TestUtils.waitForFormTitle("Home");
        assertTrue(TestUtils.screenshotTest("home-screen"), "home screenshot regressed");
    }
}
// end::testing-with-junit-java-012[]
