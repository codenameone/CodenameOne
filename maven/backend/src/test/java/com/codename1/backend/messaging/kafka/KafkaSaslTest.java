/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.backend.messaging.kafka;

import com.codename1.backend.Crypto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaSaslTest {
    @Test
    @DisplayName("SCRAM-SHA-256 reproduces the RFC 7677 exchange and checks the server")
    void scramKnownAnswer() throws IOException {
        Crypto.installCryptoEngine();
        KafkaSasl.Scram s = new KafkaSasl.Scram(Crypto.SHA256, "user", "pencil",
                "rOprNGfwEbeRWgbNEkqO");
        assertEquals("n,,n=user,r=rOprNGfwEbeRWgbNEkqO", s.clientFirst());
        String serverFirst = "r=rOprNGfwEbeRWgbNEkqO%hvYDpWUa2RaTCAfuxFIlj)hNlF$k0,"
                + "s=W22ZaJ0SNY7soEsUEjb6gQ==,i=4096";
        assertEquals("c=biws,r=rOprNGfwEbeRWgbNEkqO%hvYDpWUa2RaTCAfuxFIlj)hNlF$k0,"
                + "p=dHzbZapWIk4jUhN+Ute9ytag9zjfMHgsqmmiz7AndVQ=", s.clientFinal(serverFirst));
        s.verify("v=6rriTRBi23WpRR/wtup+mMhUZUn/dB5nLTJRsjl95G4=");
        assertThrows(IOException.class,
                () -> s.verify("v=AAAATRBi23WpRR/wtup+mMhUZUn/dB5nLTJRsjl95G4="));
    }

    @Test
    @DisplayName("SCRAM refuses a replayed nonce, a weak iteration count and a server error")
    void scramRefusals() {
        KafkaSasl.Scram s = new KafkaSasl.Scram(Crypto.SHA512, "u", "p", "abc");
        assertThrows(IOException.class, () -> s.clientFinal("r=xyzdef,s=QUJD,i=4096"));
        assertThrows(IOException.class, () -> s.clientFinal("r=abcdef,s=QUJD,i=100"));
        assertThrows(IOException.class, () -> s.clientFinal("e=invalid-proof"));
    }

    @Test
    @DisplayName("SCRAM user names escape = and ,")
    void saslName() {
        assertEquals("a=3Db=2Cc", KafkaSasl.saslName("a=b,c"));
    }

    @Test
    @DisplayName("Mechanisms are matched without case, and an unknown one is refused")
    void mechanisms() throws IOException {
        assertEquals(KafkaSasl.SCRAM_SHA_512,
                new KafkaSasl("scram-sha-512", "u", "p", null).mechanism());
        assertThrows(IOException.class, () -> new KafkaSasl("GSSAPI", "u", "p", null));
        assertThrows(IOException.class, () -> new KafkaSasl("PLAIN", null, null, null));
    }

    @Test
    @DisplayName("A JAAS line gives its user name and password, quoted or not")
    void jaas() {
        String jaas = "org.apache.kafka.common.security.plain.PlainLoginModule required "
                + "username=\"$ConnectionString\" "
                + "password=\"Endpoint=sb://ns.servicebus.windows.net/;SharedAccessKeyName=k;"
                + "SharedAccessKey=a\\\"b=\";";
        assertEquals("$ConnectionString", KafkaTransport.jaasOption(jaas, "username"));
        assertEquals("Endpoint=sb://ns.servicebus.windows.net/;SharedAccessKeyName=k;"
                + "SharedAccessKey=a\"b=", KafkaTransport.jaasOption(jaas, "password"));
        assertEquals("bob", KafkaTransport.jaasOption("M required username=bob password=x;",
                "username"));
        assertNull(KafkaTransport.jaasOption("M required;", "username"));
    }

    @Test
    @DisplayName("URL credentials are percent-decoded as UTF-8")
    void percentDecode() {
        assertEquals("a@b/c=\u00e9", KafkaSasl.percentDecode("a%40b%2Fc%3D%C3%A9"));
        assertEquals("100%", KafkaSasl.percentDecode("100%"));
    }

    @Test
    @DisplayName("The MSK region comes from the broker's host name")
    void mskRegion() {
        assertEquals("us-east-1",
                KafkaSasl.regionOf("b-1.demo.abc123.c2.kafka.us-east-1.amazonaws.com"));
        assertEquals("eu-west-1",
                KafkaSasl.regionOf("boot-x.c1.kafka-serverless.eu-west-1.amazonaws.com"));
    }

    @Test
    @DisplayName("The AWS_MSK_IAM payload is a presigned kafka-cluster:Connect in lower-case JSON")
    void mskIamPayload() throws Exception {
        Crypto.installCryptoEngine();
        KafkaSasl sasl = new KafkaSasl("AWS_MSK_IAM", null, null, "us-east-1");
        java.lang.reflect.Field f = KafkaSasl.class.getDeclaredField("credentials");
        f.setAccessible(true);
        f.set(sasl, new com.codename1.backend.aws.Credentials("AKIDEXAMPLE", "secret", "tok"));
        String json = sasl.iamPayload("b-1.x.kafka.us-east-1.amazonaws.com", "20261125T100000Z");
        assertTrue(json.startsWith("{\"version\":\"2020_10_22\",\"host\":"
                + "\"b-1.x.kafka.us-east-1.amazonaws.com\",\"user-agent\":"), json);
        assertTrue(json.contains("\"action\":\"kafka-cluster:Connect\""), json);
        assertTrue(json.contains("\"x-amz-credential\":"
                + "\"AKIDEXAMPLE/20261125/us-east-1/kafka-cluster/aws4_request\""), json);
        assertTrue(json.contains("\"x-amz-date\":\"20261125T100000Z\""), json);
        assertTrue(json.contains("\"x-amz-security-token\":\"tok\""), json);
        assertTrue(json.contains("\"x-amz-signedheaders\":\"host\""), json);
        assertTrue(json.contains("\"x-amz-expires\":\"900\""), json);
        assertTrue(json.matches(".*\"x-amz-signature\":\"[0-9a-f]{64}\".*"), json);
    }

    @Test
    @DisplayName("SCRAM refuses a mandatory extension it does not know")
    void scramMandatoryExtension() {
        KafkaSasl.Scram s = new KafkaSasl.Scram(Crypto.SHA256, "u", "p", "abc");
        assertThrows(IOException.class, () -> s.clientFinal("m=ext,r=abcdef,s=QUJD,i=4096"));
    }

    @Test
    @DisplayName("A JAAS option is not found inside another option's quoted value")
    void jaasQuotedTrap() {
        String jaas = "M required password=\"a username=x\" username=\"real\";";
        assertEquals("real", KafkaTransport.jaasOption(jaas, "username"));
        assertEquals("a username=x", KafkaTransport.jaasOption(jaas, "password"));
    }

    @Test
    @DisplayName("The JAAS login module implies the mechanism; SCRAM without one is refused")
    void mechanismFromJaas() throws IOException {
        assertEquals(KafkaSasl.PLAIN, KafkaTransport.mechanismOf(
                "org.apache.kafka.common.security.plain.PlainLoginModule required;"));
        assertEquals(KafkaSasl.AWS_MSK_IAM, KafkaTransport.mechanismOf(
                "software.amazon.msk.auth.iam.IAMLoginModule required;"));
        assertThrows(IOException.class, () -> KafkaTransport.mechanismOf(
                "org.apache.kafka.common.security.scram.ScramLoginModule required;"));
        assertNull(KafkaTransport.mechanismOf("com.example.Custom required;"));
    }

    @Test
    @DisplayName("An @ in the URL path is not a user name, and redaction keeps the path")
    void urlAuthority() throws Exception {
        java.util.Properties p = new java.util.Properties();
        com.codename1.backend.Config c = com.codename1.backend.Config.of(p, "dev");
        KafkaTransport t = new KafkaTransport("kafka://broker:9092/x@y",
                com.codename1.backend.messaging.RetryPolicy.of(c), c);
        java.lang.reflect.Field f = KafkaTransport.class.getDeclaredField("sasl");
        f.setAccessible(true);
        assertNull(f.get(t));
        t.close();
        assertEquals("kafka://***@h:1/a@b", KafkaTransport.redact("kafka://u:secret@h:1/a@b"));
        assertEquals("kafka://h:1/a@b", KafkaTransport.redact("kafka://h:1/a@b"));
    }
}
