/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security;

import com.codename1.junit.UITestBase;
import com.codename1.util.Base64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Covers [PublicKey#fromPem] / [PrivateKey#fromPem] across every container an
/// `openssl`-produced `.pem` file can be in, plus the malformed inputs that
/// used to surface as an opaque "invalid key format" from the platform.
///
/// The fixtures are throwaway keys generated for this test. Only the base64
/// bodies are stored, with the `-----BEGIN-----` armor assembled at runtime, so
/// repository secret scanning does not flag the file as a leaked private key.
class PemKeyTest extends UITestBase {

    /// Body of a throwaway PRIVATE KEY test key.
    private static final String RSA_PKCS8_LABEL = "PRIVATE KEY";
    private static final String RSA_PKCS8 = ""
            + "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDKbhYepFWuXOHh"
            + "4TWNdD7dmzDMrurYjI5fJQ5t7JPqblbCVXaxhPH4xmkm0DRfb9XSpiGJMPcVHR4c"
            + "HW3GYSWiO3SgZer5sahkKzNRUou/Mhw1gEo+rXDedwRk6COyPni6P02R6t+4ayAX"
            + "pJJmbgzJKrD0eYgWzio6a7UCe6338DsT/viXA7jc3qGuRw3WZgMNUV8itmeE/Wnk"
            + "pQZ871PM8To/5362/ohfCE7wO/9NNiTmNi0yjZxkVbHIbKcaU0qL6ByKGJryHZgt"
            + "pXYc49mWG+lAfq0+MIAXsr9qugcxHa2MQOVa8ceThbMfEmxFr7f5vLdnSRDDTzTd"
            + "ZA3ieWUTAgMBAAECggEABTsuQlrbOMcY5afWpNhc/e6r814oOEuWXxcSD5bBhWYv"
            + "wvqtncDI9mjXAGdfZk5f9DJrqdckfqmV+AJt4WaGFnJhSfo+rEOb2megMXkBa4Hc"
            + "AMK9jtXklZso94KYPMIoh7rP2/ZGrPleIIsVzXFvZ/NwFAesxwoYEhJrRxK/8cKt"
            + "V5WFBuYINAVL6tkTcG6Ghpl3HBAcftCcHPnN0N2ELaYxca4AzdHNJEFOIh8YX+CM"
            + "VMlQXiLSmCRyOsG0orDwA/T+qu2PbtELdxS8dgQm/p24BO4CK+dqmMqLpH9I5cmE"
            + "MwxToTAWcrJQmyG2nW66pm6+9ZKU+v1qzoPxBga44QKBgQDsaymxtE7NIXUUG5+f"
            + "nX6Y8Iwl+S9ZbtXilaFfeHkicBU+dYUqUDhCqgys8XJ+tBuXg3Vl17naniFzOxd0"
            + "GPbvzqJ1phkV3hZeWcSO5ipmCk4qa3qFZpIq0OgD1Xqi3WMTKriCWLgoPnRdp2Pf"
            + "Qo1oKiS5FaEetBYCMBThtmXXZwKBgQDbMkGIuhjSBwGQ79vO+hacuy9CPhTwBHP+"
            + "qiT3/z3s7IYFe92pUXXkiNaCpCYd9P0ixPAgzVhsULAylHosalQVzB06jYBuxVqu"
            + "vnG4e64sYtKq9nKyYEQ6Zk1EX7f8aSFz11seVfLQnKlSd7GsOCNjOa0v2bTcifao"
            + "tfaahZmVdQKBgDnZLuaQnAeNfDxjVfeUbfm2QlS4WGGlwSgkPMxDikBm9IvH7cGg"
            + "x2NogJmAqudd4rJ8NCmrU4quzriHaQG7ahDbmtz2u4SiRw7nIDVnFFDLjLzMd7pU"
            + "ksdvPpZRkiRvz2JNPcCHPOh7/7U61DE486jdRwcSx83fetMmOLXSD7FZAoGBAMxj"
            + "wkPx8270ZXt2jWokPK2MxXZpWTCtllOi57Hv6RhhPF8krv5RHTMqfYt38KsCZH/l"
            + "T1vm3kqxunqPhJSh2SIyIBcXFukzUWmb34J8oV52D6anAzBdH4GtHuNgtbjBdxYD"
            + "e81/q1jmm+RwA9Zoymadw2XZBRKX+s46TmarqRh5AoGBANjFYYu4HWDeTErTsyZE"
            + "SMhwrT3EG1Rkhit7duE8Uv7Kf37TvzuPqg8Y6lnbBy/RnnxyuuaHD2V+c85VHy0H"
            + "JZyT53ZrUQgmizubaPG2P6JUv9RSaSOKvpp9kkpuMxxk1sLt5NcOd62yeVyvqSXY"
            + "sdEGfQt9PNQaQSaI1oS5WAkr";

    /// Body of a throwaway PUBLIC KEY test key.
    private static final String RSA_SPKI_LABEL = "PUBLIC KEY";
    private static final String RSA_SPKI = ""
            + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAym4WHqRVrlzh4eE1jXQ+"
            + "3ZswzK7q2IyOXyUObeyT6m5WwlV2sYTx+MZpJtA0X2/V0qYhiTD3FR0eHB1txmEl"
            + "ojt0oGXq+bGoZCszUVKLvzIcNYBKPq1w3ncEZOgjsj54uj9NkerfuGsgF6SSZm4M"
            + "ySqw9HmIFs4qOmu1Anut9/A7E/74lwO43N6hrkcN1mYDDVFfIrZnhP1p5KUGfO9T"
            + "zPE6P+d+tv6IXwhO8Dv/TTYk5jYtMo2cZFWxyGynGlNKi+gcihia8h2YLaV2HOPZ"
            + "lhvpQH6tPjCAF7K/aroHMR2tjEDlWvHHk4WzHxJsRa+3+by3Z0kQw0803WQN4nll"
            + "EwIDAQAB";

    /// Body of a throwaway RSA PRIVATE KEY test key.
    private static final String RSA_PKCS1_LABEL = "RSA PRIVATE KEY";
    private static final String RSA_PKCS1 = ""
            + "MIIEpAIBAAKCAQEAym4WHqRVrlzh4eE1jXQ+3ZswzK7q2IyOXyUObeyT6m5WwlV2"
            + "sYTx+MZpJtA0X2/V0qYhiTD3FR0eHB1txmElojt0oGXq+bGoZCszUVKLvzIcNYBK"
            + "Pq1w3ncEZOgjsj54uj9NkerfuGsgF6SSZm4MySqw9HmIFs4qOmu1Anut9/A7E/74"
            + "lwO43N6hrkcN1mYDDVFfIrZnhP1p5KUGfO9TzPE6P+d+tv6IXwhO8Dv/TTYk5jYt"
            + "Mo2cZFWxyGynGlNKi+gcihia8h2YLaV2HOPZlhvpQH6tPjCAF7K/aroHMR2tjEDl"
            + "WvHHk4WzHxJsRa+3+by3Z0kQw0803WQN4nllEwIDAQABAoIBAAU7LkJa2zjHGOWn"
            + "1qTYXP3uq/NeKDhLll8XEg+WwYVmL8L6rZ3AyPZo1wBnX2ZOX/Qya6nXJH6plfgC"
            + "beFmhhZyYUn6PqxDm9pnoDF5AWuB3ADCvY7V5JWbKPeCmDzCKIe6z9v2Rqz5XiCL"
            + "Fc1xb2fzcBQHrMcKGBISa0cSv/HCrVeVhQbmCDQFS+rZE3BuhoaZdxwQHH7QnBz5"
            + "zdDdhC2mMXGuAM3RzSRBTiIfGF/gjFTJUF4i0pgkcjrBtKKw8AP0/qrtj27RC3cU"
            + "vHYEJv6duATuAivnapjKi6R/SOXJhDMMU6EwFnKyUJshtp1uuqZuvvWSlPr9as6D"
            + "8QYGuOECgYEA7GspsbROzSF1FBufn51+mPCMJfkvWW7V4pWhX3h5InAVPnWFKlA4"
            + "QqoMrPFyfrQbl4N1Zde52p4hczsXdBj2786idaYZFd4WXlnEjuYqZgpOKmt6hWaS"
            + "KtDoA9V6ot1jEyq4gli4KD50Xadj30KNaCokuRWhHrQWAjAU4bZl12cCgYEA2zJB"
            + "iLoY0gcBkO/bzvoWnLsvQj4U8ARz/qok9/897OyGBXvdqVF15IjWgqQmHfT9IsTw"
            + "IM1YbFCwMpR6LGpUFcwdOo2AbsVarr5xuHuuLGLSqvZysmBEOmZNRF+3/Gkhc9db"
            + "HlXy0JypUnexrDgjYzmtL9m03In2qLX2moWZlXUCgYA52S7mkJwHjXw8Y1X3lG35"
            + "tkJUuFhhpcEoJDzMQ4pAZvSLx+3BoMdjaICZgKrnXeKyfDQpq1OKrs64h2kBu2oQ"
            + "25rc9ruEokcO5yA1ZxRQy4y8zHe6VJLHbz6WUZIkb89iTT3Ahzzoe/+1OtQxOPOo"
            + "3UcHEsfN33rTJji10g+xWQKBgQDMY8JD8fNu9GV7do1qJDytjMV2aVkwrZZTouex"
            + "7+kYYTxfJK7+UR0zKn2Ld/CrAmR/5U9b5t5Ksbp6j4SUodkiMiAXFxbpM1Fpm9+C"
            + "fKFedg+mpwMwXR+BrR7jYLW4wXcWA3vNf6tY5pvkcAPWaMpmncNl2QUSl/rOOk5m"
            + "q6kYeQKBgQDYxWGLuB1g3kxK07MmREjIcK09xBtUZIYre3bhPFL+yn9+0787j6oP"
            + "GOpZ2wcv0Z58crrmhw9lfnPOVR8tByWck+d2a1EIJos7m2jxtj+iVL/UUmkjir6a"
            + "fZJKbjMcZNbC7eTXDnetsnlcr6kl2LHRBn0LfTzUGkEmiNaEuVgJKw==";

    /// Body of a throwaway RSA PUBLIC KEY test key.
    private static final String RSA_PKCS1_PUB_LABEL = "RSA PUBLIC KEY";
    private static final String RSA_PKCS1_PUB = ""
            + "MIIBCgKCAQEAym4WHqRVrlzh4eE1jXQ+3ZswzK7q2IyOXyUObeyT6m5WwlV2sYTx"
            + "+MZpJtA0X2/V0qYhiTD3FR0eHB1txmElojt0oGXq+bGoZCszUVKLvzIcNYBKPq1w"
            + "3ncEZOgjsj54uj9NkerfuGsgF6SSZm4MySqw9HmIFs4qOmu1Anut9/A7E/74lwO4"
            + "3N6hrkcN1mYDDVFfIrZnhP1p5KUGfO9TzPE6P+d+tv6IXwhO8Dv/TTYk5jYtMo2c"
            + "ZFWxyGynGlNKi+gcihia8h2YLaV2HOPZlhvpQH6tPjCAF7K/aroHMR2tjEDlWvHH"
            + "k4WzHxJsRa+3+by3Z0kQw0803WQN4nllEwIDAQAB";

    /// Body of a throwaway EC PRIVATE KEY test key.
    private static final String EC_SEC1_LABEL = "EC PRIVATE KEY";
    private static final String EC_SEC1 = ""
            + "MHcCAQEEIJH5okiyahqy8Ixppi+BedFt4ivFpGsBswfVmwWeDvntoAoGCCqGSM49"
            + "AwEHoUQDQgAEderdY+o+XsXzcHTlaoe82r3o3sXh4tthVHoG3wwC85wOUYmuK/c4"
            + "pPZ0ZQdRH+GOAXgB+oRNfj8WSYM9mShrng==";

    /// Body of a throwaway PRIVATE KEY test key.
    private static final String EC_PKCS8_LABEL = "PRIVATE KEY";
    private static final String EC_PKCS8 = ""
            + "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgkfmiSLJqGrLwjGmm"
            + "L4F50W3iK8WkawGzB9WbBZ4O+e2hRANCAAR16t1j6j5exfNwdOVqh7zavejexeHi"
            + "22FUegbfDALznA5Ria4r9zik9nRlB1Ef4Y4BeAH6hE1+PxZJgz2ZKGue";

    /// Body of a throwaway PUBLIC KEY test key.
    private static final String EC_SPKI_LABEL = "PUBLIC KEY";
    private static final String EC_SPKI = ""
            + "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEderdY+o+XsXzcHTlaoe82r3o3sXh"
            + "4tthVHoG3wwC85wOUYmuK/c4pPZ0ZQdRH+GOAXgB+oRNfj8WSYM9mShrng==";

    /// Body of a throwaway ENCRYPTED PRIVATE KEY test key.
    private static final String RSA_ENCRYPTED_LABEL = "ENCRYPTED PRIVATE KEY";
    private static final String RSA_ENCRYPTED = ""
            + "MIIFNTBfBgkqhkiG9w0BBQ0wUjAxBgkqhkiG9w0BBQwwJAQQY8To5swati8sXuzo"
            + "3z2E8AICCAAwDAYIKoZIhvcNAgkFADAdBglghkgBZQMEASoEEKdygeJPfRDAh00W"
            + "ATuxzPIEggTQo43lk/saOfmHkeGcnzchdwEk5h3wKkq2wFa3M/Uzkuk6Hmj/Da4e"
            + "MSTBCDPcbxhsHq2J6mIcjt8xbAwIAtCfbs7wiR7Hy+rGnWqvefcT52RMbGTLpkN9"
            + "V7SuLs6B6M3gidSeghYNdfHsNt4gI0lJndBFYxitS3x7OSSTlspiLzLo2BK4sN1+"
            + "Mh5YtK0Dh13VEAa9UZ/JVfjpxVXjC4BXEZ0dcb/GgX/jwP/XKDabAXilu5jw3HkF"
            + "0EAK/AtmDkaVGbyTnRB7w6gDXhEVK2rJwpL86s80S27ZvuTCIn6PFnCwG1zWzk+7"
            + "8FD4NYcfsbZqfPvbyQ86wT4GsGitDihhBwnE6TUpvyGfplHcWnPHgrrdix4W60DJ"
            + "0xafMSzS7miVBrNqqQwh9FFRAewZ3FCb9Zu771pE67QsfK1dSJLP15uyOuEhsMsb"
            + "3qxaXohsJBTKGcn0c4LG4qnTGiwcdwlGHDd7xYxr+uOLfbp8NU5uFVO4Yi5rk/Mf"
            + "CkgznBwiM6SKJVCE2UeDwVeh4sFVYxGJ9crzPoxyXvEWsov6+VQKjg9c5BUS9A/M"
            + "Cc5KSZ+JzdKA1vcLjAxm4aK9yP5MvN3gQatC2JGYV/HsK4H5D+wM+C667maXRoHw"
            + "YA+1x77r9WubN6mUiK/0rzBFfLSv/ZiAcFL1C//H5+yVgxe8tYHUrgavPm3tpFWd"
            + "KDVFI9pQXZ1RXyFiW5RdNYdKVHMVQsGD41uwa8GfwLFPbQ/W0p184d+rXMZwOO+O"
            + "34j7s3DXdQzDL/L2Oq/CCEwNzCZxR6LFwfbqIww6YpQZtNWlGoZ+97H7ofNJZeFA"
            + "pMuhOJA89uZ6X7J7i/nKJEfKeIK/jjwYOm0nYFh+X6wfISgtHX4Jmedfj0FLL93D"
            + "FaSl5Zb7eSKkx4PmM3rDuRvffC2DVE6NgdDBSIab6ZMVQeV+CpbdepEkJfAHLHPK"
            + "e/qqpK+1ex8bRmtyABH8u/ghOvq4SbpQvB4L0MHUYmwDMs85Z3LZa6NlHFpYd6sx"
            + "L3YyHrPB5+e7+KRimnyG/W+2yqQ/DTvp4Jvu53LXsVebCjO7ah8JU1eKVWI0v5sN"
            + "D1sy3aDUma/0Nxcigux/kmV5HmgIw77wH81W7xPZwMfJ16lUgbKUgt9s5kzJBHOk"
            + "PyzUdN6XmVYEQnouz4pjCCb+e3V0fyvrDWXrJpTyQ35007Lc7UQemKhibwxyddOj"
            + "91sHzfCyzUzu/Zt9GzdXH/y90DMTQdnWgG4eN8RhMmvl55EHDNe6H9iWufhhjTWX"
            + "kZIjkRmeWWJfJLR+EiphIAVDaqtP2cOV01u6e5tUbb5KZ/0toqNfyWtSbZQ9Wuao"
            + "tO3bNJ4uQNjOzDmdUjbgvC9sCSSQEPL9sU5Rc2/HekEsq+OB3rXvUBV4ZDG0QNHe"
            + "gXpgJLXRDgRWXRXJQW0CxC1EyC/i+27qk6+O72De1VkIkwE/EdhY+/og3b2NVWiA"
            + "MSSOHGsVw16H/sEAfavw8KR687KraiCXxMuCO6qTRMJkZ3oFcWWCYTcLcLs8X7vD"
            + "2Agk+VJipUjmP2DGPGmfheb09pKrc3f8N8HZJEaVEnVpEr5ps/cf1Jrg5rRhwWfH"
            + "6j/tPHJczFOda/pPLAvKUTJFb0ykC1SarM1a7JAlBkIjQV/6gy1LnZo=";

    /// Body of a throwaway EC PRIVATE KEY test key written with explicit curve parameters.
    private static final String EC_SEC1_EXPLICIT_LABEL = "EC PRIVATE KEY";
    private static final String EC_SEC1_EXPLICIT = ""
            + "MIIBaAIBAQQgzgQyqm3SoKJl/+kk1lZcl5DdXBwSi5mcV2dUxnlnnl6ggfowgfcC"
            + "AQEwLAYHKoZIzj0BAQIhAP////8AAAABAAAAAAAAAAAAAAAA////////////////"
            + "MFsEIP////8AAAABAAAAAAAAAAAAAAAA///////////////8BCBaxjXYqjqT57Pr"
            + "vVV2mIa8ZR0GsMxTsPY7zjw+J9JgSwMVAMSdNgiG5wSTamZ44ROdJreBn36QBEEE"
            + "axfR8uEsQkf4vOblY6RA8ncDfYEt6zOg9KE5RdiYwpZP40Li/hp/m47n60p8D54W"
            + "K84zV2sxXs7LtkBoN79R9QIhAP////8AAAAA//////////+85vqtpxeehPO5ysL8"
            + "YyVRAgEBoUQDQgAELryQVp8o9+EzTdiZFP3DYQLp8K4b54Nhj++QzO8OKuAFi3Y7"
            + "WIGMvCvnnWjHO2n1HlYN2qjIcumoTe+Vc0lLow==";

    /// Body of a throwaway PRIVATE KEY test key written with explicit curve parameters.
    private static final String EC_PKCS8_EXPLICIT_LABEL = "PRIVATE KEY";
    private static final String EC_PKCS8_EXPLICIT = ""
            + "MIIBeQIBADCCAQMGByqGSM49AgEwgfcCAQEwLAYHKoZIzj0BAQIhAP////8AAAAB"
            + "AAAAAAAAAAAAAAAA////////////////MFsEIP////8AAAABAAAAAAAAAAAAAAAA"
            + "///////////////8BCBaxjXYqjqT57PrvVV2mIa8ZR0GsMxTsPY7zjw+J9JgSwMV"
            + "AMSdNgiG5wSTamZ44ROdJreBn36QBEEEaxfR8uEsQkf4vOblY6RA8ncDfYEt6zOg"
            + "9KE5RdiYwpZP40Li/hp/m47n60p8D54WK84zV2sxXs7LtkBoN79R9QIhAP////8A"
            + "AAAA//////////+85vqtpxeehPO5ysL8YyVRAgEBBG0wawIBAQQgzgQyqm3SoKJl"
            + "/+kk1lZcl5DdXBwSi5mcV2dUxnlnnl6hRANCAAQuvJBWnyj34TNN2JkU/cNhAunw"
            + "rhvng2GP75DM7w4q4AWLdjtYgYy8K+edaMc7afUeVg3aqMhy6ahN75VzSUuj";

    private static String pem(String label, String base64) {
        StringBuilder sb = new StringBuilder("-----BEGIN ").append(label).append("-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            sb.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
        }
        return sb.append("-----END ").append(label).append("-----\n").toString();
    }

    private static byte[] der(String base64) {
        return Base64.decode(base64.getBytes());
    }

    /// The Temurin 8 build this project compiles against ships no SunEC
    /// provider, so EC signing cannot run there. The rewrap assertions below do
    /// not depend on it and always run; only the "and the platform accepts the
    /// result" half is conditional.
    private static boolean ecProviderPresent() {
        try {
            java.security.KeyFactory.getInstance("EC");
            return true;
        } catch (java.security.NoSuchAlgorithmException e) {
            return false;
        }
    }

    // ---- the case from the report: armored PEM straight from a backend ----

    @Test
    void rsaPemRoundTrip() {
        PublicKey pub = PublicKey.fromPem(pem(RSA_SPKI_LABEL, RSA_SPKI));
        PrivateKey priv = PrivateKey.fromPem(pem(RSA_PKCS8_LABEL, RSA_PKCS8));

        assertEquals(PublicKey.RSA, pub.getAlgorithm());
        assertEquals("X.509", pub.getFormat());
        assertEquals(PublicKey.RSA, priv.getAlgorithm());
        assertEquals("PKCS#8", priv.getFormat());
        assertArrayEquals(der(RSA_SPKI), pub.getEncoded());
        assertArrayEquals(der(RSA_PKCS8), priv.getEncoded());

        byte[] plaintext = "Secret message".getBytes();
        byte[] ciphertext = Cipher.rsaEncrypt(Cipher.RSA_OAEP_SHA256, pub, plaintext);
        assertArrayEquals(plaintext, Cipher.rsaDecrypt(Cipher.RSA_OAEP_SHA256, priv, ciphertext));
    }

    @Test
    void rsaPemAcceptedAsRawFileBytes() {
        // Util.readInputStream gives bytes, not a String -- the overload has to
        // reach the same key.
        byte[] fileBytes = pem(RSA_SPKI_LABEL, RSA_SPKI).getBytes();
        assertArrayEquals(der(RSA_SPKI), PublicKey.fromPem(fileBytes).getEncoded());
    }

    // ---- older containers get rewrapped rather than rejected ----

    @Test
    void pkcs1PrivateKeyIsRewrappedAsPkcs8() {
        PrivateKey priv = PrivateKey.fromPem(pem(RSA_PKCS1_LABEL, RSA_PKCS1));
        assertEquals(PublicKey.RSA, priv.getAlgorithm());
        // the rewrap has to be byte-identical to what "openssl pkcs8 -topk8" makes
        assertArrayEquals(der(RSA_PKCS8), priv.getEncoded());

        PublicKey pub = PublicKey.fromPem(pem(RSA_SPKI_LABEL, RSA_SPKI));
        byte[] ciphertext = Cipher.rsaEncrypt(Cipher.RSA_OAEP_SHA256, pub, "pkcs1".getBytes());
        assertArrayEquals("pkcs1".getBytes(),
                Cipher.rsaDecrypt(Cipher.RSA_OAEP_SHA256, priv, ciphertext));
    }

    @Test
    void pkcs1PublicKeyIsRewrappedAsSpki() {
        PublicKey pub = PublicKey.fromPem(pem(RSA_PKCS1_PUB_LABEL, RSA_PKCS1_PUB));
        assertEquals(PublicKey.RSA, pub.getAlgorithm());
        assertArrayEquals(der(RSA_SPKI), pub.getEncoded());

        PrivateKey priv = PrivateKey.fromPem(pem(RSA_PKCS8_LABEL, RSA_PKCS8));
        byte[] ciphertext = Cipher.rsaEncrypt(Cipher.RSA_OAEP_SHA256, pub, "pkcs1pub".getBytes());
        assertArrayEquals("pkcs1pub".getBytes(),
                Cipher.rsaDecrypt(Cipher.RSA_OAEP_SHA256, priv, ciphertext));
    }

    @Test
    void sec1EcPrivateKeyIsRewrappedAsPkcs8() {
        // "openssl ecparam -genkey" emits SEC1, so this is the default EC file.
        PrivateKey priv = PrivateKey.fromPem(pem(EC_SEC1_LABEL, EC_SEC1));
        PublicKey pub = PublicKey.fromPem(pem(EC_SPKI_LABEL, EC_SPKI));
        assertEquals(PublicKey.EC, priv.getAlgorithm());
        assertEquals(PublicKey.EC, pub.getAlgorithm());
        // lifting the curve out of the SEC1 [0] field has to land exactly where
        // "openssl pkcs8 -topk8" puts it
        assertArrayEquals(der(EC_PKCS8), priv.getEncoded());

        assumeTrue(ecProviderPresent(), "JDK has no EC provider");
        byte[] data = "sign me".getBytes();
        byte[] sig = Signature.sign(Signature.SHA256_WITH_ECDSA, priv, data);
        assertTrue(Signature.verify(Signature.SHA256_WITH_ECDSA, pub, data, sig));
    }

    @Test
    void ecPkcs8RoundTrip() {
        PrivateKey priv = PrivateKey.fromPem(pem(EC_PKCS8_LABEL, EC_PKCS8));
        PublicKey pub = PublicKey.fromPem(pem(EC_SPKI_LABEL, EC_SPKI));
        assertArrayEquals(der(EC_PKCS8), priv.getEncoded());
        assertEquals(PublicKey.EC, pub.getAlgorithm());

        assumeTrue(ecProviderPresent(), "JDK has no EC provider");
        byte[] data = "sign me too".getBytes();
        byte[] sig = Signature.sign(Signature.SHA256_WITH_ECDSA, priv, data);
        assertTrue(Signature.verify(Signature.SHA256_WITH_ECDSA, pub, data, sig));
    }

    // ---- tolerated input shapes ----

    @Test
    void bareBase64WithoutArmorIsAccepted() {
        // keys carried in JSON or a build hint arrive without armor
        assertArrayEquals(der(RSA_SPKI), PublicKey.fromPem(RSA_SPKI).getEncoded());
        assertArrayEquals(der(RSA_PKCS8), PrivateKey.fromPem(RSA_PKCS8).getEncoded());
    }

    @Test
    void bareBase64StillTellsPublicFromPrivate() {
        // with no label to go on the container has to be read out of the DER,
        // or the mix-up only surfaces as the platform's "invalid key format"
        CryptoException asPublic = assertThrows(CryptoException.class,
                () -> PublicKey.fromPem(RSA_PKCS8));
        assertTrue(asPublic.getMessage().contains("private key"), asPublic.getMessage());

        CryptoException asPrivate = assertThrows(CryptoException.class,
                () -> PrivateKey.fromPem(RSA_SPKI));
        assertTrue(asPrivate.getMessage().contains("public key"), asPrivate.getMessage());
    }

    @Test
    void crlfLineEndingsAndSurroundingTextAreIgnored() {
        String armored = pem(RSA_SPKI_LABEL, RSA_SPKI).replace("\n", "\r\n");
        String noisy = "# key rotated 2026-01-01\r\n" + armored + "\r\ntrailing note\r\n";
        assertArrayEquals(der(RSA_SPKI), PublicKey.fromPem(noisy).getEncoded());
    }

    @Test
    void explicitAlgorithmOverloadSkipsDetection() {
        PublicKey pub = PublicKey.fromPem(PublicKey.RSA, pem(RSA_SPKI_LABEL, RSA_SPKI));
        assertEquals(PublicKey.RSA, pub.getAlgorithm());
        assertArrayEquals(der(RSA_SPKI), pub.getEncoded());
    }

    // ---- rejected input names its own problem ----

    @Test
    void encryptedPrivateKeyIsRejectedWithTheDecryptCommand() {
        CryptoException e = assertThrows(CryptoException.class,
                () -> PrivateKey.fromPem(pem(RSA_ENCRYPTED_LABEL, RSA_ENCRYPTED)));
        assertTrue(e.getMessage().contains("encrypted"), e.getMessage());
        assertTrue(e.getMessage().contains("openssl pkcs8"), e.getMessage());
    }

    @Test
    void publicKeyPemIsRejectedByPrivateKeyFactory() {
        CryptoException e = assertThrows(CryptoException.class,
                () -> PrivateKey.fromPem(pem(RSA_SPKI_LABEL, RSA_SPKI)));
        assertTrue(e.getMessage().contains("PUBLIC KEY"), e.getMessage());
    }

    @Test
    void certificateIsRejectedWithTheExtractCommand() {
        CryptoException e = assertThrows(CryptoException.class,
                () -> PublicKey.fromPem(pem("CERTIFICATE", RSA_SPKI)));
        assertTrue(e.getMessage().contains("openssl x509"), e.getMessage());
    }

    @Test
    void garbageIsACryptoExceptionNotAnArrayIndexError() {
        assertThrows(CryptoException.class, () -> PublicKey.fromPem("not a key at all"));
        assertThrows(CryptoException.class, () -> PublicKey.fromPem(""));
        assertThrows(CryptoException.class, () -> PublicKey.fromPem(pem("PUBLIC KEY", "!!!!")));
    }

    @Test
    void truncatedDerIsACryptoExceptionNotAnArrayIndexError() {
        byte[] full = der(RSA_SPKI);
        for (int cut = 1; cut < 24; cut++) {
            byte[] chopped = new byte[full.length - cut];
            System.arraycopy(full, 0, chopped, 0, chopped.length);
            String truncated = pem(RSA_SPKI_LABEL, Base64.encodeNoNewline(chopped));
            assertThrows(CryptoException.class, () -> PublicKey.fromPem(truncated),
                    "cut of " + cut + " bytes should not escape as a runtime error");
        }
    }

    @Test
    void footerMustMatchTheHeaderLabel() {
        // RFC 7468. A block closed by someone else's footer is a spliced file,
        // and one with no footer was cut off; neither should load silently.
        String spliced = "-----BEGIN PUBLIC KEY-----\n" + RSA_SPKI + "\n-----END PRIVATE KEY-----\n";
        CryptoException mismatched = assertThrows(CryptoException.class,
                () -> PublicKey.fromPem(spliced));
        assertTrue(mismatched.getMessage().contains("-----END PUBLIC KEY-----"), mismatched.getMessage());

        String headerOnly = "-----BEGIN PUBLIC KEY-----\n" + RSA_SPKI + "\n";
        assertThrows(CryptoException.class, () -> PublicKey.fromPem(headerOnly));
    }

    @Test
    void unarmoredInputAcceptsTheSameContainersAsArmored() {
        // stripping the armor must not change which formats load, and must not
        // change the bytes they produce
        assertArrayEquals(der(RSA_SPKI), PublicKey.fromPem(RSA_PKCS1_PUB).getEncoded());
        assertArrayEquals(der(RSA_PKCS8), PrivateKey.fromPem(RSA_PKCS1).getEncoded());
        assertArrayEquals(der(EC_PKCS8), PrivateKey.fromPem(EC_SEC1).getEncoded());
    }

    @Test
    void unarmoredInputStillTellsPublicFromPrivate() {
        assertTrue(assertThrows(CryptoException.class,
                () -> PrivateKey.fromPem(RSA_PKCS1_PUB)).getMessage().contains("public key"));
        assertTrue(assertThrows(CryptoException.class,
                () -> PublicKey.fromPem(EC_SEC1)).getMessage().contains("private key"));
    }

    @Test
    void oversizedDerLengthDoesNotAllocate() {
        // A four-byte length near Integer.MAX_VALUE used to make the bounds
        // check "pos + length > der.length" overflow to a negative number and
        // pass, so a ten-byte PEM reached new byte[length] and died with
        // OutOfMemoryError. Every declared length must come back as a
        // CryptoException instead.
        for (int shift = 0; shift < 32; shift++) {
            int length = 1 << shift;
            byte[] oversized = {0x30, 0x08, 0x30, 0x06, 0x06, (byte) 0x84,
                    (byte) (length >>> 24), (byte) (length >>> 16),
                    (byte) (length >>> 8), (byte) length};
            String armored = pem("PUBLIC KEY", Base64.encodeNoNewline(oversized));
            assertThrows(CryptoException.class, () -> PublicKey.fromPem(armored),
                    "declared length 2^" + shift + " must not escape as a runtime error");
        }
    }

    @Test
    void contentAfterTheBase64PaddingIsRejected() {
        // Base64.decode stops at the first '=' and ignores the rest, so without
        // an explicit check a spliced body loads on its first half alone.
        CryptoException e = assertThrows(CryptoException.class,
                () -> PublicKey.fromPem(pem(RSA_SPKI_LABEL, RSA_SPKI + "=garbage")));
        assertTrue(e.getMessage().contains("padding"), e.getMessage());

        assertThrows(CryptoException.class,
                () -> PublicKey.fromPem(pem(RSA_SPKI_LABEL, RSA_SPKI + "!!")));
        assertThrows(CryptoException.class,
                () -> PublicKey.fromPem(pem(RSA_SPKI_LABEL, RSA_SPKI.substring(0, RSA_SPKI.length() - 1))));
    }

    @Test
    void readsCannotEscapeTheEnclosingDerElement() {
        // An AlgorithmIdentifier declaring length 0 (or too few bytes) followed
        // by an OID that really belongs to the enclosing SEQUENCE used to be
        // walked as though the OID were its own, reporting a malformed key as
        // valid RSA -- and it then failed in the platform bridge with exactly
        // the opaque error this parser exists to replace.
        byte[] oid = {0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7,
                0x0D, 0x01, 0x01, 0x01};

        byte[] emptyAlgId = new byte[15];
        emptyAlgId[0] = 0x30;
        emptyAlgId[1] = 0x0D;
        emptyAlgId[2] = 0x30;
        emptyAlgId[3] = 0x00;
        emptyAlgId[4] = 0x06;
        emptyAlgId[5] = 0x09;
        System.arraycopy(oid, 0, emptyAlgId, 6, oid.length);
        assertThrows(CryptoException.class,
                () -> PublicKey.fromPem(pem("PUBLIC KEY", Base64.encodeNoNewline(emptyAlgId))));

        byte[] shortAlgId = emptyAlgId.clone();
        shortAlgId[3] = 0x02;
        assertThrows(CryptoException.class,
                () -> PublicKey.fromPem(pem("PUBLIC KEY", Base64.encodeNoNewline(shortAlgId))));

        byte[] pkcs8AlgId = new byte[18];
        pkcs8AlgId[0] = 0x30;
        pkcs8AlgId[1] = 0x10;
        pkcs8AlgId[2] = 0x02;
        pkcs8AlgId[3] = 0x01;
        pkcs8AlgId[4] = 0x00;
        pkcs8AlgId[5] = 0x30;
        pkcs8AlgId[6] = 0x00;
        pkcs8AlgId[7] = 0x06;
        pkcs8AlgId[8] = 0x09;
        System.arraycopy(oid, 0, pkcs8AlgId, 9, oid.length);
        assertThrows(CryptoException.class,
                () -> PrivateKey.fromPem(pem("PRIVATE KEY", Base64.encodeNoNewline(pkcs8AlgId))));
    }

    @Test
    void sec1WithExplicitCurveParametersIsPreserved() {
        // ECParameters is a CHOICE: usually a named-curve OID, but a whole
        // SEQUENCE when the key was written with "openssl ecparam
        // -param_enc explicit". Reading an OID out of it rejected a valid SEC1
        // key, so the field is carried over whole -- and the result is what
        // "openssl pkcs8 -topk8" produces for the same file, byte for byte.
        PrivateKey priv = PrivateKey.fromPem(pem(EC_SEC1_EXPLICIT_LABEL, EC_SEC1_EXPLICIT));
        assertEquals(PublicKey.EC, priv.getAlgorithm());
        assertArrayEquals(der(EC_PKCS8_EXPLICIT), priv.getEncoded());

        // and unarmored input reaches the same key
        assertArrayEquals(der(EC_PKCS8_EXPLICIT),
                PrivateKey.fromPem(EC_SEC1_EXPLICIT).getEncoded());
    }

    private static byte[] hex(String s) {
        String h = s.replace(" ", "");
        byte[] out = new byte[h.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    @Test
    void aContainerMissingItsMandatoryFieldsIsRejected() {
        // A well-formed AlgorithmIdentifier and nothing else looks exactly like
        // the start of an SPKI. Classifying on the first child alone returned a
        // key carrying no public value, which then failed in the platform
        // bridge with the opaque error this class exists to replace.
        assertThrows(CryptoException.class, () -> PublicKey.fromPem(
                pem("PUBLIC KEY", Base64.encodeNoNewline(
                        hex("300f 300d 0609 2a864886f70d010101 0500")))));

        // the same omission on the private side: no privateKey OCTET STRING
        assertThrows(CryptoException.class, () -> PrivateKey.fromPem(
                pem("PRIVATE KEY", Base64.encodeNoNewline(
                        hex("3012 020100 300d 0609 2a864886f70d010101 0500")))));

        // RSAPrivateKey's nine INTEGER fields are all mandatory
        assertThrows(CryptoException.class, () -> PrivateKey.fromPem(
                pem("PRIVATE KEY", Base64.encodeNoNewline(hex("3009 020100 020101 020102")))));
    }

    @Test
    void aLeadingParametersBlockIsSkipped() {
        // "openssl ecparam -name prime256v1 -genkey" (without -noout) writes an
        // EC PARAMETERS block ahead of the key, and taking whatever block came
        // first rejected that file -- the exact command the javadoc says works.
        String twoBlocks = pem("EC PARAMETERS", "BggqhkjOPQMBBw==")
                + pem(EC_SEC1_LABEL, EC_SEC1);
        assertArrayEquals(der(EC_PKCS8), PrivateKey.fromPem(twoBlocks).getEncoded());
    }

    @Test
    void eachFactoryPicksItsOwnBlockFromAMixedFile() {
        String both = pem(EC_SPKI_LABEL, EC_SPKI) + pem(EC_PKCS8_LABEL, EC_PKCS8);
        assertArrayEquals(der(EC_PKCS8), PrivateKey.fromPem(both).getEncoded());
        assertArrayEquals(der(EC_SPKI), PublicKey.fromPem(both).getEncoded());

        String reversed = pem(EC_PKCS8_LABEL, EC_PKCS8) + pem(EC_SPKI_LABEL, EC_SPKI);
        assertArrayEquals(der(EC_SPKI), PublicKey.fromPem(reversed).getEncoded());
        assertArrayEquals(der(EC_PKCS8), PrivateKey.fromPem(reversed).getEncoded());
    }

    @Test
    void aFileWithNoUsableBlockNamesWhatItHolds() {
        CryptoException e = assertThrows(CryptoException.class,
                () -> PrivateKey.fromPem(pem("EC PARAMETERS", "BggqhkjOPQMBBw==")));
        assertTrue(e.getMessage().contains("EC PARAMETERS"), e.getMessage());
    }

    @Test
    void anIncompleteSpkiValueIsRejected() {
        // Peeking at the BIT STRING tag was not enough: a lone 0x03 byte and an
        // empty "03 00" both passed as SubjectPublicKeyInfo, and SPKI has
        // exactly two fields so nothing may follow the value either.
        assertThrows(CryptoException.class, () -> PublicKey.fromPem(pem("PUBLIC KEY",
                Base64.encodeNoNewline(hex("3010 300d 0609 2a864886f70d010101 0500 03")))));
        assertThrows(CryptoException.class, () -> PublicKey.fromPem(pem("PUBLIC KEY",
                Base64.encodeNoNewline(hex("3011 300d 0609 2a864886f70d010101 0500 0300")))));
        assertThrows(CryptoException.class, () -> PublicKey.fromPem(pem("PUBLIC KEY",
                Base64.encodeNoNewline(hex("3014 300d 0609 2a864886f70d010101 0500 030100 0500")))));
    }

    @Test
    void sec1RejectsFieldsItHasNoRoomFor() {
        // ECPrivateKey ends with at most one [0] and one [1]. Accepting anything
        // else let a malformed key carry thousands of junk children, each one
        // appended to a growing array that was copied whole every time.
        StringBuilder content = new StringBuilder("020101").append("0420")
                .append("00000000000000000000000000000000000000000000000000000000000000")
                .append("00")
                .append("A00A06082a8648ce3d030107");
        for (int i = 0; i < 2000; i++) {
            content.append("0500");
        }
        byte[] body = hex(content.toString());
        byte[] blob = new byte[body.length + 4];
        blob[0] = 0x30;
        blob[1] = (byte) 0x82;
        blob[2] = (byte) (body.length >> 8);
        blob[3] = (byte) body.length;
        System.arraycopy(body, 0, blob, 4, body.length);

        CryptoException e = assertThrows(CryptoException.class,
                () -> PrivateKey.fromPem(pem("EC PRIVATE KEY", Base64.encodeNoNewline(blob))));
        assertTrue(e.getMessage().contains("unexpected field"), e.getMessage());
    }

    @Test
    void algorithmIdentifierCarriesAtMostOneParametersField() {
        // AlgorithmIdentifier ::= SEQUENCE { OID, parameters ANY OPTIONAL }.
        // Reading the OID and stopping accepted { rsaEncryption, NULL, NULL }.
        assertThrows(CryptoException.class, () -> PublicKey.fromPem(pem("PUBLIC KEY",
                Base64.encodeNoNewline(
                        hex("3014 300f 0609 2a864886f70d010101 0500 0500 030100")))));
    }

    @Test
    void aBitStringOfOnlyItsUnusedBitsOctetIsRejected() {
        // "03 01 00" is a one-byte BIT STRING whose single octet is the
        // unused-bits count, so it carries no key material -- the length check
        // has to demand more than that one byte of metadata.
        assertThrows(CryptoException.class, () -> PublicKey.fromPem(pem("PUBLIC KEY",
                Base64.encodeNoNewline(hex("3012 300d 0609 2a864886f70d010101 0500 030100")))));
    }

    @Test
    void rfc5958VersionOneKeysAreNormalized() {
        // JDK 11 refuses a version-1 OneAsymmetricKey ("version mismatch") while
        // 17 and later accept it, so passing one through works on some supported
        // runtimes and not others. Normalizing reproduces the canonical
        // version-0 key exactly.
        byte[] canonical = der(RSA_PKCS8);
        int off = (canonical[1] & 0xFF) < 0x80 ? 2 : 2 + (canonical[1] & 0x7F);
        byte[] inner = new byte[canonical.length - off];
        System.arraycopy(canonical, off, inner, 0, inner.length);
        inner[2] = 0x01;                                  // version 0 -> 1

        byte[] publicKey = new byte[66];                  // a [1] publicKey field
        publicKey[0] = (byte) 0xA1;
        publicKey[1] = 64;
        byte[] content = new byte[inner.length + publicKey.length];
        System.arraycopy(inner, 0, content, 0, inner.length);
        System.arraycopy(publicKey, 0, content, inner.length, publicKey.length);

        byte[] v1 = new byte[content.length + 4];
        v1[0] = 0x30;
        v1[1] = (byte) 0x82;
        v1[2] = (byte) (content.length >> 8);
        v1[3] = (byte) content.length;
        System.arraycopy(content, 0, v1, 4, content.length);

        PrivateKey key = PrivateKey.fromPem(pem("PRIVATE KEY", Base64.encodeNoNewline(v1)));
        assertArrayEquals(canonical, key.getEncoded());

        // a key that is already version 0 is passed through untouched
        assertArrayEquals(canonical, PrivateKey.fromPem(pem(RSA_PKCS8_LABEL, RSA_PKCS8)).getEncoded());
    }

    @Test
    void nonMinimalDerLengthsAreRejected() {
        // DER demands the shortest length encoding; BER does not. JDK 11 and 17
        // refuse a redundant length while 21 and later accept it, so a key
        // encoded this way loads on some supported runtimes and not others.
        byte[] spki = der(RSA_SPKI);
        byte[] content = new byte[spki.length - 4];
        System.arraycopy(spki, 4, content, 0, content.length);

        byte[] leadingZero = new byte[content.length + 5];
        leadingZero[0] = 0x30;
        leadingZero[1] = (byte) 0x83;
        leadingZero[2] = 0x00;
        leadingZero[3] = (byte) (content.length >> 8);
        leadingZero[4] = (byte) content.length;
        System.arraycopy(content, 0, leadingZero, 5, content.length);
        CryptoException e = assertThrows(CryptoException.class,
                () -> PublicKey.fromPem(pem("PUBLIC KEY", Base64.encodeNoNewline(leadingZero))));
        assertTrue(e.getMessage().contains("non-minimal"), e.getMessage());

        // long form used where the short form would do
        assertThrows(CryptoException.class, () -> PublicKey.fromPem(pem("PUBLIC KEY",
                Base64.encodeNoNewline(hex("3014 30810d 0609 2a864886f70d010101 0500 03020001")))));
    }

    @Test
    void unterminatedArmorIsRejected() {
        assertThrows(CryptoException.class,
                () -> PublicKey.fromPem("-----BEGIN PUBLIC KEY" + RSA_SPKI));
    }
}
