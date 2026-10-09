/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.security.crypto;

/// Keys and signatures made once with the openssl command line, so what the
/// runtime reads and verifies in these tests was written by something else.
///
/// The keys exist for the tests alone and guard nothing.
public final class KeyFixtures {
    private KeyFixtures() {
    }

    /// What every signature below signs, as ASCII.
    public static final String MESSAGE = "The quick brown fox jumps over the lazy dog";

    /// openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048.
    public static final String RSA_PKCS8_PEM =
            "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCx1KwoWUSGSYGU\n" +
            "0D7Lzk2dL/X6uYOU9p+FXxRUV3AcaJg+kpUL9+tOAeJMUs1szIuEA9XYzLBVNscn\n" +
            "Ff2OILFi8GW4P2nIYmSSvkoSwKWam5XYQ5JgFpUbwnchsnBGgtRnmypI3JIKqWqZ\n" +
            "UnctzlMzkVj8664qNwKGDv7jehiot7cFZVau5cglWo2LTlcgXKZBRf9jmSkmOaL5\n" +
            "tyVu+SbFksdA7sp3uE/Bh6VkEAKRanqCsBw4SCQICHV+FGsPIhzahDJyCf0pxNcN\n" +
            "2t8sYz/kI1ZalBdZaF+hu2PkvNnRJqbARH6kmNYwEBgjqSifP1kDo/CBWBINwo+Y\n" +
            "CqbMjyLjAgMBAAECggEABwCFQNd2+8SkLgxfFJ5Mbw6G5Hbuh+yIDnPWdJW2y9+Q\n" +
            "cxJJfR2nbbxkQTYXcZvCOJIAFxfEL67G+7KBd9mwsmEd2Dni++ln5WFJwGBGhQZw\n" +
            "SYIrns301BF9qF2CzzvyihvRd+n7dCEEmgHlwG951jN4agkLpAzjdAzeG23gz6/P\n" +
            "KAopQuo8vjiKE6rh7gFDubu8ugc2OpAdAum0ckJ06KC8jJKphNq/LVTO6EpyzgY1\n" +
            "NgFSTqYPLJaFrw6p7BKZ+iVD2O6es6FaWPu0Jwv4Mo8H7XnGsnUZvnfUxgYoqBvv\n" +
            "FRiZ8ewkN4FKVZDKM5IfqZCFb/LwUYM40VtREzUvcQKBgQD6VpUGuChWqP3xvN7u\n" +
            "t3MMDj1txCXUGeJ6WGDaxThDEHaBHEIoXpauGKBnxkuIoR7bLWAnufVffxRX/5Ay\n" +
            "8tZeP5wpe6LMGpl0HjzOcWn87QHJbytghpVc3bifQwO/neaoTER5gEonT14L0ln4\n" +
            "CN1fnu1MB4CkDoyTpgTtASKMkwKBgQC12kirIE6qIuZgKc68GdOOS33IwTxyL/iP\n" +
            "DrCVK4YkoQ53hFZQssZCkECNOAvlFVjxNNG8DGYhYHVNs0Jl+Dt4Sx7+36N+z8WN\n" +
            "29Iig06nQmfKFELzBKVBzdMgHReHM1cXAOdDo5RceRMWTVQsVd3ojzRvWrQJTt5z\n" +
            "unQyrJhScQKBgQCE6uXTnIImiTHUYZEItLTMKN9q4aOoO1op1bUPU3ns+dfB86wY\n" +
            "3SgqJf89Omcuk0Xb3/rW/QCQhNvbYWFB+/fgMOwMho3IyzLBGbD1d/hrh7fUKUeh\n" +
            "x7OUjFETlrRt0DwBDgWpcXlt59Eqe7SzYpmPxMWAAdfGw8bWOmcRI/IhKwKBgHyb\n" +
            "vU2dTqngTjG4lBNqMv9/FQq59lxcKJqGO1OLxlhVD9+vi6GyTo4P4Fuj+uqXbSGi\n" +
            "ytBrQpQ+T0LVwXqz1LRB7VRCE/ryDfF9ngjOJtgPdaUPqyxwk3h6u992b8fR0yxN\n" +
            "DyrW7PNMd1rB1BqpH+yaLBjdcx4pr95m9fY/NATRAoGBAPK9LKIoqOQF1s29RHVu\n" +
            "Z921meAke6sx0x/IZ8ZUuIX8ddLwj8lHDbIYmNZ0OUGRewKEl2SHBd/zkLPsML9t\n" +
            "WtoHYrcbIqiC4vQY/QE7zWnYaTffm+rqbYoivDU/LsbFJdWM0C9vVw004HSKCzLH\n" +
            "E1mCd1xkW4ryPsv009Gtp6/m\n" +
            "-----END PRIVATE KEY-----\n";

    /// The same key: openssl rsa -traditional.
    public static final String RSA_PKCS1_PEM =
            "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIEpAIBAAKCAQEAsdSsKFlEhkmBlNA+y85NnS/1+rmDlPafhV8UVFdwHGiYPpKV\n" +
            "C/frTgHiTFLNbMyLhAPV2MywVTbHJxX9jiCxYvBluD9pyGJkkr5KEsClmpuV2EOS\n" +
            "YBaVG8J3IbJwRoLUZ5sqSNySCqlqmVJ3Lc5TM5FY/OuuKjcChg7+43oYqLe3BWVW\n" +
            "ruXIJVqNi05XIFymQUX/Y5kpJjmi+bclbvkmxZLHQO7Kd7hPwYelZBACkWp6grAc\n" +
            "OEgkCAh1fhRrDyIc2oQycgn9KcTXDdrfLGM/5CNWWpQXWWhfobtj5LzZ0SamwER+\n" +
            "pJjWMBAYI6konz9ZA6PwgVgSDcKPmAqmzI8i4wIDAQABAoIBAAcAhUDXdvvEpC4M\n" +
            "XxSeTG8OhuR27ofsiA5z1nSVtsvfkHMSSX0dp228ZEE2F3GbwjiSABcXxC+uxvuy\n" +
            "gXfZsLJhHdg54vvpZ+VhScBgRoUGcEmCK57N9NQRfahdgs878oob0Xfp+3QhBJoB\n" +
            "5cBvedYzeGoJC6QM43QM3htt4M+vzygKKULqPL44ihOq4e4BQ7m7vLoHNjqQHQLp\n" +
            "tHJCdOigvIySqYTavy1UzuhKcs4GNTYBUk6mDyyWha8OqewSmfolQ9junrOhWlj7\n" +
            "tCcL+DKPB+15xrJ1Gb531MYGKKgb7xUYmfHsJDeBSlWQyjOSH6mQhW/y8FGDONFb\n" +
            "URM1L3ECgYEA+laVBrgoVqj98bze7rdzDA49bcQl1Bnielhg2sU4QxB2gRxCKF6W\n" +
            "rhigZ8ZLiKEe2y1gJ7n1X38UV/+QMvLWXj+cKXuizBqZdB48znFp/O0ByW8rYIaV\n" +
            "XN24n0MDv53mqExEeYBKJ09eC9JZ+AjdX57tTAeApA6Mk6YE7QEijJMCgYEAtdpI\n" +
            "qyBOqiLmYCnOvBnTjkt9yME8ci/4jw6wlSuGJKEOd4RWULLGQpBAjTgL5RVY8TTR\n" +
            "vAxmIWB1TbNCZfg7eEse/t+jfs/FjdvSIoNOp0JnyhRC8wSlQc3TIB0XhzNXFwDn\n" +
            "Q6OUXHkTFk1ULFXd6I80b1q0CU7ec7p0MqyYUnECgYEAhOrl05yCJokx1GGRCLS0\n" +
            "zCjfauGjqDtaKdW1D1N57PnXwfOsGN0oKiX/PTpnLpNF29/61v0AkITb22FhQfv3\n" +
            "4DDsDIaNyMsywRmw9Xf4a4e31ClHocezlIxRE5a0bdA8AQ4FqXF5befRKnu0s2KZ\n" +
            "j8TFgAHXxsPG1jpnESPyISsCgYB8m71NnU6p4E4xuJQTajL/fxUKufZcXCiahjtT\n" +
            "i8ZYVQ/fr4uhsk6OD+Bbo/rql20hosrQa0KUPk9C1cF6s9S0Qe1UQhP68g3xfZ4I\n" +
            "zibYD3WlD6sscJN4ervfdm/H0dMsTQ8q1uzzTHdawdQaqR/smiwY3XMeKa/eZvX2\n" +
            "PzQE0QKBgQDyvSyiKKjkBdbNvUR1bmfdtZngJHurMdMfyGfGVLiF/HXS8I/JRw2y\n" +
            "GJjWdDlBkXsChJdkhwXf85Cz7DC/bVraB2K3GyKoguL0GP0BO81p2Gk335vq6m2K\n" +
            "Irw1Py7GxSXVjNAvb1cNNOB0igsyxxNZgndcZFuK8j7L9NPRraev5g==\n" +
            "-----END RSA PRIVATE KEY-----\n";

    /// Its public half: openssl pkey -pubout.
    public static final String RSA_PUBLIC_PEM =
            "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsdSsKFlEhkmBlNA+y85N\n" +
            "nS/1+rmDlPafhV8UVFdwHGiYPpKVC/frTgHiTFLNbMyLhAPV2MywVTbHJxX9jiCx\n" +
            "YvBluD9pyGJkkr5KEsClmpuV2EOSYBaVG8J3IbJwRoLUZ5sqSNySCqlqmVJ3Lc5T\n" +
            "M5FY/OuuKjcChg7+43oYqLe3BWVWruXIJVqNi05XIFymQUX/Y5kpJjmi+bclbvkm\n" +
            "xZLHQO7Kd7hPwYelZBACkWp6grAcOEgkCAh1fhRrDyIc2oQycgn9KcTXDdrfLGM/\n" +
            "5CNWWpQXWWhfobtj5LzZ0SamwER+pJjWMBAYI6konz9ZA6PwgVgSDcKPmAqmzI8i\n" +
            "4wIDAQAB\n" +
            "-----END PUBLIC KEY-----\n";

    /// openssl pkcs8 -topk8 -v2 aes-256-cbc, which this runtime refuses.
    public static final String RSA_ENCRYPTED_PKCS8_PEM =
            "-----BEGIN ENCRYPTED PRIVATE KEY-----\n" +
            "MIIFNTBfBgkqhkiG9w0BBQ0wUjAxBgkqhkiG9w0BBQwwJAQQk2ALDP9A8gEdJm3l\n" +
            "uUAViAICCAAwDAYIKoZIhvcNAgkFADAdBglghkgBZQMEASoEEMvtj1l2W4/pJDw1\n" +
            "gYN0+qUEggTQjSc3f4rJ9fdx2JTXdsqDWSHMudLM0RgnkcpOQ9Ecv4LBjlIAcBrY\n" +
            "ow8e/r1hG1dlZCGxgzh45QCbcSkiYVkom8SvkMc055oPgESFoq/sk5BqCSFkoWA1\n" +
            "19pJ2Zg8SrPEn1na0iIWKN0D5g6Bm/8zxRaRD66+1tW2pfiEjok50yDSL/K40jcm\n" +
            "s4g6WdwWR11R5txRgxwmtN3HTRlePfOkpenTv4IzYqx74XJl1msEEVwaUasLR6sU\n" +
            "aLAnnxHhwPJGK9ueEh44jPeAuqLPesSJ8xpsywkhUu24INTdzJRwLId387iFDeb+\n" +
            "K7xDDfNySve5TPgf5mmzKPj6yKAI6qc9/Bx8ZXvlhPL3TswY8EYlexEPDPIJdeP2\n" +
            "h1Jtkgq5sSQgpbO0TofbJ9F6WB9lPHOzK0Kd+16ckxtYgGVS6f/DjuPQ3XZYe5HE\n" +
            "Vwu7lLDac9Ln5sSDwEiXH3AI/vqvH0rmlyhpktKpAfiqQCsowfPGdmZAycmKoEpf\n" +
            "qAmnnKiEh9dw8QHzr8tt7Emmb28G3AIHesQCextzXD51XBfe21ysz2+kFY1RNx3+\n" +
            "rJ/nmGA/gMjCmp8/kaJ0BoJNZtnjX2fJSJSKbZdccF9wBWEEI9xXh0hWfsLqbinK\n" +
            "XeHn0ENZ7qcITfGwcQgkH8wXXVGbgj2GNwW2U58jXt1h+n3a5N6oabju7epYZo8y\n" +
            "R0eMVJ95yxkhLWzPI/+pRSjrqWySZtKdH5Onopvk3YDV+GJVV9EwxFmEq60mq8lT\n" +
            "+mR3ybVnpjp5511lFiX/29Yq/4vufI4tW5bp13PY8Rh4bWefjpbu6ti0dP/UuL0u\n" +
            "7QZ1XECt3+r7E9Gf653B8rTP6lLsMoJsJLI5iNcj6o1DM9GIf7iust6W1JNdtjNi\n" +
            "AeENL9yroBEZKiCtTUUgu/nno5ShOXCKxtLWs9vdCMzwrVBtWgPNapWQkX5uaR56\n" +
            "KDHGfW9qO5Z01VhNoPhWa8M75nlz/TTU6POaWLPsr5Vms5Z+PLZPWBOcvZmbqPaZ\n" +
            "6fAdDMJn+WNEu9Y4Qfsp8xNtK6mQ77AEX7Zp+Kuism+bnI+tcrklsLo0I/GjSR5F\n" +
            "eeNWeM57n2kD7ZFfvn5dyOHDR6ebqNGPoDFwaW/UaRsACDqumoMq1Rat7EvhFHy0\n" +
            "5czKbMQZdXc15KyGPW9d+EP/PFzwLT6BA2zGTdBLAZ2Zqip/vkWGpovfDKD6odqM\n" +
            "0nxGaQWupfGpbgtnOCFn3I/HP84/ZuaSyPCz6dLRfa8NJiUKiOAqc+/cgmEdqyhW\n" +
            "sDykDPWilHav1aJrCdlc1yYZaib9zAQg4t136iOGlCLYgAmPJIshyKAqPnhtnna7\n" +
            "fbSoDn4dvgUPZKkHT5WHTdFXA70mo0kEbbGhSM2LpvVgjN2xBAGNmrOYre/otHk0\n" +
            "stK6M6LdEA+UnA7n6ChnR3l/8G9dFqrRPclvR2ZHrTgV6Ylm7naONTA9cgbbhsId\n" +
            "safGBkfW0RY9vr5fv1MLlKDNuZKM8e/oO8JwNpoYr3l3YWvy11aZcI9gzMRLRr6k\n" +
            "RZwlYUljW6R3uy/m7Sf5lsE1uDEKXMl/aG31EyEtf2c4HiPmde5QCQIaImxHuwtM\n" +
            "0NheE/aZaFqauH5/7jN3E4szKaiLQIDxTEZOO+P4PLF1mHxd4fC4eLo=\n" +
            "-----END ENCRYPTED PRIVATE KEY-----\n";

    /// openssl rsa -traditional -aes128, which this runtime refuses.
    public static final String RSA_ENCRYPTED_PKCS1_PEM =
            "-----BEGIN RSA PRIVATE KEY-----\n" +
            "Proc-Type: 4,ENCRYPTED\n" +
            "DEK-Info: AES-128-CBC,B72FCB14DD4C46659AD6B5DD51D21977\n" +
            "\n" +
            "KtHVJOQiLRxyccGlgLq61iygNaIkMcCfc3qwhQUDccVzMaLutpOXxuhowZTByU/D\n" +
            "8QTPHsFMbtZO+sxEJDN04wY/i2UZhp0UzpgpGRejHouJDFqKxiFs3iN7+ko9r6Dq\n" +
            "SAy4PfLyVV2ugCkD8vpoT47kyhzFGqByjQQS0Qldg/l2QlRmqqWHbCZxwtuLhGY9\n" +
            "tBBH8mVayNuMeOpk5fROy7HmWJI4K7saNi7PGzNJm3wPtLWwnnLhd6c/e6MVP0Rc\n" +
            "iMQda5Fdtj3x14jyXfaqL6z7n+AezVl+NRVKF5RPikGfOIIRdqHNYxglWVmzsEdR\n" +
            "WTpJ8Cg9I+29R/9S5S8c8+aFfj6bSO6H030H3TkOaAtRnZLyLMv4Zf2iUD1k/L7U\n" +
            "kxzUnY42mZ9ksYQiZAzo8euNROqXANwtBuvvFT6MHO2PD6CqtNd//YMbCc+ogoq3\n" +
            "jcF3IXrFDZHrjOUikHcSBD9eSA3tb/Z7aJj2yQHuyTTMmPSyxM7G6D2wx9eO7qBQ\n" +
            "k3NIGj7RzUb7zjXl5qISD0WtmyAKGZxsgBc4DxKXCPx6HRiHKJ5U4+nLPy+wc52A\n" +
            "/y4hnhzmx/8zoIUfDH7Y/79dKiqT3ftdj0z6LQLveypvasNt2uhOOwZ2O2b3byv3\n" +
            "FfjLybEptPvcu2aAL7IPHiFsusALYmvBXCT0h6nhaT1D6uOsRFzjyV8Wbku1JQyf\n" +
            "UIx0hrA1WRoACZQvOLx6UNuAiWeb3WmjdeSaAbmk90K9ym8dtTpQceq/RrF/CU7j\n" +
            "+Za4WPH7CYKkWA21oNDsRmnNh79+99amvW06iXkLhUMZqUpMrmVJS9mzNW7VeMFX\n" +
            "DQLfAVhQgovHS7lcaCqvSuLtGmH1cz30OQF8AjIR5kqRDJ15QffwqljsEpZWnEcT\n" +
            "NclOS1SMQ37vIDUKleY777dJvX4Pl4aMV3qZ6nSakYroavGuh/qZeJOBqnp+qlg0\n" +
            "t2akurGCmyuIyPk+k5w31HShQIfxk//L03h/VMdVPuqD/T8p18gomx00IAB77v1P\n" +
            "XgOqYUpTkGX4dIQnyOqRSdrjmMcjeEjmDM+Sx2UZpKYPfCXMGKzV+83TTU/nR1XG\n" +
            "6lgwZoxTaeTJSmRBAeYHe1uPjihWybMR9VcbDzNV1peI8AHgqbh8mDf4Xn2FQALA\n" +
            "LG4ls1phFOUY3IxxjXnf1XdZla1r4wu9LEJewRbu5fK89+w/U9Vh1myPuTPFpSNj\n" +
            "FZpMOcmWakdE0hXOQM69ve+YrDrvX1fqUAJ/Wcn6/ohNNdbYSpRTHplcF18XX9Kx\n" +
            "BbSGY8JojZ/5HPazWWkV/gYf6o2XEgeuBwsK+4h1fAowHa2ECcK0uciw8tjhq8eB\n" +
            "8yfD42RdJMEnvVvKdqlmg+b3RwN/cJP0xD3UBPyz22ziS60QE68+Hz691q9Uky6x\n" +
            "ib29AL27mqvoqCQSjyDyQ4O5pBZUaa+55217vLLNE0VI/TmxWBCC78Zf7fSY4Gtf\n" +
            "p17XWylpZPhiakBS2HIT0YE5xHGGXo+8hHQxdpdz0GMykQ/GHZlhu4QR1eCsBCSV\n" +
            "7Z63E79hbv+/IquL/Psu1Rdv0pwB9TpGZu7FjEFG7U3S4FsdRRe3jvPcQ+/DhxKv\n" +
            "-----END RSA PRIVATE KEY-----\n";

    /// openssl ecparam -name prime256v1 -genkey -noout.
    public static final String EC256_SEC1_PEM =
            "-----BEGIN EC PRIVATE KEY-----\n" +
            "MHcCAQEEIIgmxCWZRrdWhDOrnu51+kCbkgZSM3VP3GNCvbVsiyeWoAoGCCqGSM49\n" +
            "AwEHoUQDQgAE9teOALB/mkUnrMHBMAXpTYWQk0KLoYJIcZHMCSPhQ78Nht5XPJML\n" +
            "PGNYq+X6TDK8hDiGCR247x2klHqnh6fzdA==\n" +
            "-----END EC PRIVATE KEY-----\n";

    /// The same key: openssl pkcs8 -topk8 -nocrypt.
    public static final String EC256_PKCS8_PEM =
            "-----BEGIN PRIVATE KEY-----\n" +
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgiCbEJZlGt1aEM6ue\n" +
            "7nX6QJuSBlIzdU/cY0K9tWyLJ5ahRANCAAT2144AsH+aRSeswcEwBelNhZCTQouh\n" +
            "gkhxkcwJI+FDvw2G3lc8kws8Y1ir5fpMMryEOIYJHbjvHaSUeqeHp/N0\n" +
            "-----END PRIVATE KEY-----\n";

    /// Its public half.
    public static final String EC256_PUBLIC_PEM =
            "-----BEGIN PUBLIC KEY-----\n" +
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE9teOALB/mkUnrMHBMAXpTYWQk0KL\n" +
            "oYJIcZHMCSPhQ78Nht5XPJMLPGNYq+X6TDK8hDiGCR247x2klHqnh6fzdA==\n" +
            "-----END PUBLIC KEY-----\n";

    /// openssl ecparam -name secp384r1 -genkey -noout.
    public static final String EC384_SEC1_PEM =
            "-----BEGIN EC PRIVATE KEY-----\n" +
            "MIGkAgEBBDCE6iyC0VTYwavC2mbFTGa531Bo3OExsEpFeuM7ncV7cCPlKjBhOZuo\n" +
            "wBFebFViTTKgBwYFK4EEACKhZANiAASFg2xJ6X100TrwsZkykrpPrV+L3xsCTvDq\n" +
            "6GCqDPv8fTYrhJC7YaDwsBbeUPvF2b5dqCFPjwvaFeRwwxBA3lvKGUVDOFcmpTa0\n" +
            "4skWzsrLWbxaHDBWMLjPXv9f9+NZsh0=\n" +
            "-----END EC PRIVATE KEY-----\n";

    /// The same key as PKCS#8.
    public static final String EC384_PKCS8_PEM =
            "-----BEGIN PRIVATE KEY-----\n" +
            "MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCE6iyC0VTYwavC2mbF\n" +
            "TGa531Bo3OExsEpFeuM7ncV7cCPlKjBhOZuowBFebFViTTKhZANiAASFg2xJ6X10\n" +
            "0TrwsZkykrpPrV+L3xsCTvDq6GCqDPv8fTYrhJC7YaDwsBbeUPvF2b5dqCFPjwva\n" +
            "FeRwwxBA3lvKGUVDOFcmpTa04skWzsrLWbxaHDBWMLjPXv9f9+NZsh0=\n" +
            "-----END PRIVATE KEY-----\n";

    /// Its public half.
    public static final String EC384_PUBLIC_PEM =
            "-----BEGIN PUBLIC KEY-----\n" +
            "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEhYNsSel9dNE68LGZMpK6T61fi98bAk7w\n" +
            "6uhgqgz7/H02K4SQu2Gg8LAW3lD7xdm+XaghT48L2hXkcMMQQN5byhlFQzhXJqU2\n" +
            "tOLJFs7Ky1m8WhwwVjC4z17/X/fjWbId\n" +
            "-----END PUBLIC KEY-----\n";

    /// The RSA private key as PKCS#8 DER, base64.
    public static final String RSA_PKCS8_DER =
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCx1KwoWUSGSYGU0D7Lzk2dL/X6" +
            "uYOU9p+FXxRUV3AcaJg+kpUL9+tOAeJMUs1szIuEA9XYzLBVNscnFf2OILFi8GW4P2nIYmSSvkoS" +
            "wKWam5XYQ5JgFpUbwnchsnBGgtRnmypI3JIKqWqZUnctzlMzkVj8664qNwKGDv7jehiot7cFZVau" +
            "5cglWo2LTlcgXKZBRf9jmSkmOaL5tyVu+SbFksdA7sp3uE/Bh6VkEAKRanqCsBw4SCQICHV+FGsP" +
            "IhzahDJyCf0pxNcN2t8sYz/kI1ZalBdZaF+hu2PkvNnRJqbARH6kmNYwEBgjqSifP1kDo/CBWBIN" +
            "wo+YCqbMjyLjAgMBAAECggEABwCFQNd2+8SkLgxfFJ5Mbw6G5Hbuh+yIDnPWdJW2y9+QcxJJfR2n" +
            "bbxkQTYXcZvCOJIAFxfEL67G+7KBd9mwsmEd2Dni++ln5WFJwGBGhQZwSYIrns301BF9qF2Czzvy" +
            "ihvRd+n7dCEEmgHlwG951jN4agkLpAzjdAzeG23gz6/PKAopQuo8vjiKE6rh7gFDubu8ugc2OpAd" +
            "Aum0ckJ06KC8jJKphNq/LVTO6EpyzgY1NgFSTqYPLJaFrw6p7BKZ+iVD2O6es6FaWPu0Jwv4Mo8H" +
            "7XnGsnUZvnfUxgYoqBvvFRiZ8ewkN4FKVZDKM5IfqZCFb/LwUYM40VtREzUvcQKBgQD6VpUGuChW" +
            "qP3xvN7ut3MMDj1txCXUGeJ6WGDaxThDEHaBHEIoXpauGKBnxkuIoR7bLWAnufVffxRX/5Ay8tZe" +
            "P5wpe6LMGpl0HjzOcWn87QHJbytghpVc3bifQwO/neaoTER5gEonT14L0ln4CN1fnu1MB4CkDoyT" +
            "pgTtASKMkwKBgQC12kirIE6qIuZgKc68GdOOS33IwTxyL/iPDrCVK4YkoQ53hFZQssZCkECNOAvl" +
            "FVjxNNG8DGYhYHVNs0Jl+Dt4Sx7+36N+z8WN29Iig06nQmfKFELzBKVBzdMgHReHM1cXAOdDo5Rc" +
            "eRMWTVQsVd3ojzRvWrQJTt5zunQyrJhScQKBgQCE6uXTnIImiTHUYZEItLTMKN9q4aOoO1op1bUP" +
            "U3ns+dfB86wY3SgqJf89Omcuk0Xb3/rW/QCQhNvbYWFB+/fgMOwMho3IyzLBGbD1d/hrh7fUKUeh" +
            "x7OUjFETlrRt0DwBDgWpcXlt59Eqe7SzYpmPxMWAAdfGw8bWOmcRI/IhKwKBgHybvU2dTqngTjG4" +
            "lBNqMv9/FQq59lxcKJqGO1OLxlhVD9+vi6GyTo4P4Fuj+uqXbSGiytBrQpQ+T0LVwXqz1LRB7VRC" +
            "E/ryDfF9ngjOJtgPdaUPqyxwk3h6u992b8fR0yxNDyrW7PNMd1rB1BqpH+yaLBjdcx4pr95m9fY/" +
            "NATRAoGBAPK9LKIoqOQF1s29RHVuZ921meAke6sx0x/IZ8ZUuIX8ddLwj8lHDbIYmNZ0OUGRewKE" +
            "l2SHBd/zkLPsML9tWtoHYrcbIqiC4vQY/QE7zWnYaTffm+rqbYoivDU/LsbFJdWM0C9vVw004HSK" +
            "CzLHE1mCd1xkW4ryPsv009Gtp6/m";

    /// The RSA public key as SubjectPublicKeyInfo DER, base64.
    public static final String RSA_PUBLIC_DER =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsdSsKFlEhkmBlNA+y85NnS/1+rmDlPaf" +
            "hV8UVFdwHGiYPpKVC/frTgHiTFLNbMyLhAPV2MywVTbHJxX9jiCxYvBluD9pyGJkkr5KEsClmpuV" +
            "2EOSYBaVG8J3IbJwRoLUZ5sqSNySCqlqmVJ3Lc5TM5FY/OuuKjcChg7+43oYqLe3BWVWruXIJVqN" +
            "i05XIFymQUX/Y5kpJjmi+bclbvkmxZLHQO7Kd7hPwYelZBACkWp6grAcOEgkCAh1fhRrDyIc2oQy" +
            "cgn9KcTXDdrfLGM/5CNWWpQXWWhfobtj5LzZ0SamwER+pJjWMBAYI6konz9ZA6PwgVgSDcKPmAqm" +
            "zI8i4wIDAQAB";

    /// The P-256 private key as PKCS#8 DER, base64.
    public static final String EC256_PKCS8_DER =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgiCbEJZlGt1aEM6ue7nX6QJuSBlIz" +
            "dU/cY0K9tWyLJ5ahRANCAAT2144AsH+aRSeswcEwBelNhZCTQouhgkhxkcwJI+FDvw2G3lc8kws8" +
            "Y1ir5fpMMryEOIYJHbjvHaSUeqeHp/N0";

    /// The P-256 public key as SubjectPublicKeyInfo DER, base64.
    public static final String EC256_PUBLIC_DER =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE9teOALB/mkUnrMHBMAXpTYWQk0KLoYJIcZHMCSPh" +
            "Q78Nht5XPJMLPGNYq+X6TDK8hDiGCR247x2klHqnh6fzdA==";

    /// The P-384 private key as PKCS#8 DER, base64.
    public static final String EC384_PKCS8_DER =
            "MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDCE6iyC0VTYwavC2mbFTGa531Bo3OEx" +
            "sEpFeuM7ncV7cCPlKjBhOZuowBFebFViTTKhZANiAASFg2xJ6X100TrwsZkykrpPrV+L3xsCTvDq" +
            "6GCqDPv8fTYrhJC7YaDwsBbeUPvF2b5dqCFPjwvaFeRwwxBA3lvKGUVDOFcmpTa04skWzsrLWbxa" +
            "HDBWMLjPXv9f9+NZsh0=";

    /// The P-384 public key as SubjectPublicKeyInfo DER, base64.
    public static final String EC384_PUBLIC_DER =
            "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEhYNsSel9dNE68LGZMpK6T61fi98bAk7w6uhgqgz7/H02" +
            "K4SQu2Gg8LAW3lD7xdm+XaghT48L2hXkcMMQQN5byhlFQzhXJqU2tOLJFs7Ky1m8WhwwVjC4z17/" +
            "X/fjWbId";

    /// openssl dgst -sha256 -sign, base64.
    public static final String RS256_SIGNATURE =
            "PGfzHoC0p2A3fhbldgq5NKXCJTtZ+2e70RNQ1H9ocxiHTvl2SLJoWjusbGglKB7RDllBFW8dPz3w" +
            "5r9mTs/mZb5tgjSQW5HPiKeaZLbzU64m/8pvXmAYh2RYmBtSjTns6obmErLpXGmCTkW7tMNicR1L" +
            "uCRD92BjUmtG1wTiwHErKL1QscL60HSff4j4ZEaxMSdOD2E4wzUSMJXVn580cVITZebVAlSD2LpS" +
            "0A1JZpTCPQCQz35ac+gHmjMwHxmOh5fQ0mGKmMaFDPsSECj2a/dv9d+lNae4C3rOEoH3tqXQcipL" +
            "nKLgOZxBVLnKEh5BSYdBGzQVl7NHJStnJsCUVA==";

    /// openssl dgst -sha384 -sign, base64.
    public static final String RS384_SIGNATURE =
            "j7FAwtPWJGmUeI0adWMMdGD5FiItegDeN76j/nAyXVM+u6RIpURy8k9lsyL/3ORnhCVp5lCw74RD" +
            "DcWX37nfG+jMGarajkeoltq8OxK7qJndsaSzFqS1hiD23zL8Epv7RN/PKpslrNLlImH5BOuCd8A8" +
            "QJuOHkx+CDkwntXr6qAMP9T5huacOnbAD1al48CfodQyUhT6mBt6iXwSrlluljAw0dxMf0Za8uHW" +
            "dzy+C3UfUVs26AfIrMTAKICyj3fUGz7PEBmXTix29+G/dwzhScp8R76o/Vi3IxDvWkD8t3Fow33f" +
            "WGJ9zjZ3t6vXIPefK4hfGK89xwKMqRm4aetjbg==";

    /// openssl dgst -sha512 -sign, base64.
    public static final String RS512_SIGNATURE =
            "LqbyEzVyWTdho1gmfDjHZ2o+uDLbyfLd9BvLJjEiWKJZTjNa0+D586lgP+iTWxBUwL0g8jjJr3l2" +
            "FF/jLBRQ4wHZOXIpOA4sl3he4cD2jO2Sacm5BSjEUnPXGDkZDkGHfr4RTAHsYMitftDvNFtePS+h" +
            "JSSNXord/JVlTLFVy+4d8RWwKmtiwWuQE7JvKzz+wEKryX+LCwSOFJUpvTdxPOu2DpL9P/TgFFGE" +
            "uDzO8yxhti2uw4zpgUpBkiNyoyrGJ/eUhr2+7mNxZwPJ7E23YU7xTK1rs1WIO4bp/9aVKFKEcXLR" +
            "drr7HZWEBabW529XDn5MNnKG1fIJc90v8rG45g==";

    /// openssl dgst -sha256 -sigopt rsa_padding_mode:pss -sigopt rsa_pss_saltlen:32 -sigopt rsa_mgf1_md:sha256, base64.
    public static final String PS256_SIGNATURE =
            "q3gJ+04n3fCy7T65Wt7OvJuEgyyrmtdIToEnL1J9gHyBsNz/GOyN6sQy0j48KMp0spWyx2L+ae++" +
            "d9uOMfh2lIjZ3s9PVRC/ZJSXjlUvdlV6k96OTXpYCuu1WRt6nPM3KMyxW1VGMy/TRcF+m0cpq0V8" +
            "InuPGLkFA11TNDVSJMxlTjW+CYQZ0kjNLe/itzvaFBtPt6GUbXVBQ541WjJCbn2I7LwcRp+xFhrh" +
            "QT+zCDtkFSqZrflbr3prUpmk89oUxPfGFxBpkk+V4L3cziIeAwXW73v7MnzZIVzk3sYqQb58tvtO" +
            "TDfI+fRRTFEr+VwbrvmnVd37BVBA0frx+oZ3Iw==";

    /// openssl dgst -sha256 -sign with the P-256 key: ASN.1 DER, base64.
    public static final String ES256_SIGNATURE =
            "MEUCIQDEdXYq3I0XC10NVjpa9UesQWfqtlv3gWon3s0Eh6M7pAIgJfSQufjCEeNsxe/7YPnPVmeV" +
            "F6sWI5vNR1LRJLWWQeU=";

    /// openssl dgst -sha384 -sign with the P-384 key: ASN.1 DER, base64.
    public static final String ES384_SIGNATURE =
            "MGUCMAiCwWhMQ0uNc0zxu42btr30j4TZEjZTLFQ3sgg91+ITBdpZrt8zk1EETDD8b1ElDAIxAOvK" +
            "UryV2Llf+R8jdMmG2jcr3pJSofQ4T5Rpbn2L+3YO3IhD0aWzw+sZnU6dImSVXA==";
}
