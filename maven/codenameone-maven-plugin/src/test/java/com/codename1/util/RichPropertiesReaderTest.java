package com.codename1.util;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class RichPropertiesReaderTest {

    private InputStream toStream(String str) throws IOException {
        return new ByteArrayInputStream(str.getBytes("UTF-8"));
    }

    @Test
    public void testReadSimpleProperties() throws Exception {
        RichPropertiesReader reader = new RichPropertiesReader();
        {
            String string = "foo=bar\nfizz=bazz\nback=ward";
            Properties props = new Properties();
            reader.load(toStream(string), props);
            assertEquals(3, props.size());
            assertEquals("bar", props.getProperty("foo"));
            assertEquals("bazz", props.getProperty("fizz"));
            assertEquals("ward", props.getProperty("back"));

        }


    }

    @Test
    public void testReadRichProperties() throws Exception {
        RichPropertiesReader reader = new RichPropertiesReader();
        {
            String dependencyString =  "<dependency>\n" +
                    "            <groupId>com.codenameone</groupId>\n" +
                    "            <artifactId>cn1-builder-resources-android</artifactId>\n" +
                    "            <scope>runtime</scope>\n" +
                    "        </dependency>\n" +
                    "        <dependency>\n" +
                    "            <groupId>com.codenameone</groupId>\n" +
                    "            <artifactId>codenameone-android</artifactId>\n" +
                    "            <scope>runtime</scope>\n" +
                    "        </dependency>";
            String string = "foo=bar\nfizz=bazz\nback=ward\n"
                    + "[dependencies]\n====\n"
                    + dependencyString + "\n"
                    + "====\npass=word\nkey=val\n\n\nfool=barl";
            Properties props = new Properties();
            reader.load(toStream(string), props);
            assertEquals(7, props.size());
            assertEquals("bar", props.getProperty("foo"));
            assertEquals("bazz", props.getProperty("fizz"));
            assertEquals("ward", props.getProperty("back"));
            assertEquals("word", props.getProperty("pass"));
            assertEquals("val", props.getProperty("key"));
            assertEquals("barl", props.getProperty("fool"));
            assertEquals(dependencyString, props.getProperty("dependencies"));


        }


    }
}
