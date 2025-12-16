package com.codename1.ui.util;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import com.codename1.ui.Display;
import static com.codename1.testing.TestUtils.*;

public class ResourcesCoverageTest extends UITestBase {

    @FormTest
    public void testMediaRule() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeShort(1);
        dos.writeByte(0xF2);
        dos.writeUTF("myTheme");

        dos.writeShort(2);

        dos.writeUTF("@dummyKey");
        dos.writeUTF("dummyValue");

        String platform = Display.getInstance().getPlatformName();
        // Suffix "Color" is important so Resources knows to read an int.
        dos.writeUTF("platform-" + platform + "-myKeyColor");
        dos.writeInt(0x00FF00);

        dos.close();

        Resources res = Resources.open(new ByteArrayInputStream(baos.toByteArray()), 160);
        Hashtable theme = res.getTheme("myTheme");

        assertTrue(theme.containsKey("myKeyColor"));
    }
}
