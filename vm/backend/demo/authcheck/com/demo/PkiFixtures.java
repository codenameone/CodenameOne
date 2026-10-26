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
package com.demo;

/// The certificates, keys and key stores of `maven/backend/src/test/resources/pki`,
/// written by its `generate.sh` with the openssl command line, copied here so the
/// translated binary has them without reading files. Regenerate both together.
///
/// The keys exist for the tests alone and guard nothing.
final class PkiFixtures {
    private PkiFixtures() {
    }

    static final String ROOT_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIBrjCCAVSgAwIBAgIUW8wNuefCp7QUk4khOEG9qf23OccwCgYIKoZIzj0EAwIw\n" +
            "NDELMAkGA1UEBhMCVVMxETAPBgNVBAoMCFRlc3QgUEtJMRIwEAYDVQQDDAlUZXN0\n" +
            "IFJvb3QwIBcNMjUwMTAxMDAwMDAwWhgPMjEyNTAxMDEwMDAwMDBaMDQxCzAJBgNV\n" +
            "BAYTAlVTMREwDwYDVQQKDAhUZXN0IFBLSTESMBAGA1UEAwwJVGVzdCBSb290MFkw\n" +
            "EwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEGKZxeWvYNGe0deyUaWVvJ/Z1rfenjl+Q\n" +
            "paTHQhPTxG0w8Xaok6I32OrD+zF6kIENIZdaGACWyf+e9YNVXQlH6qNCMEAwDwYD\n" +
            "VR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFPt32dsoOolH\n" +
            "URGvbR8Iagl94mG5MAoGCCqGSM49BAMCA0gAMEUCIQCB0Sl2iB8o26LfcVcSuuYP\n" +
            "aruAPo1YN80Uof3190qkrAIgaFLj4QaQKFN51cB2xlHa+hOFjoF75dWNbLuKWfKd\n" +
            "Ah0=\n" +
            "-----END CERTIFICATE-----\n";

    static final String INTER_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIICkTCCAjigAwIBAgIBAjAKBggqhkjOPQQDAjA0MQswCQYDVQQGEwJVUzERMA8G\n" +
            "A1UECgwIVGVzdCBQS0kxEjAQBgNVBAMMCVRlc3QgUm9vdDAgFw0yNTAxMDEwMDAw\n" +
            "MDBaGA8yMTI1MDEwMTAwMDAwMFowPDELMAkGA1UEBhMCVVMxETAPBgNVBAoMCFRl\n" +
            "c3QgUEtJMRowGAYDVQQDDBFUZXN0IEludGVybWVkaWF0ZTCCASIwDQYJKoZIhvcN\n" +
            "AQEBBQADggEPADCCAQoCggEBAMfzuGX5KAkqd/nvFgjt5HxI57gTuTN1stlbHntu\n" +
            "E614t3h9/it/72gBrC9MWdVEwzusEs6YRthJwNisLO/KBmdTu31CKi8dsi4WKbJj\n" +
            "zhkiFIlDpfnSHLWfEoZ2p96FXdJ2YnvjcsXMkAyHb+RaHgy9n4eBw3LgyWFOS/Eg\n" +
            "z+6nDCDyKJxR80hCdOOE3l0c6Ky9uvgAxUrPwLWmIl4TmL6qLUD8V3Mx6oT/dap0\n" +
            "wu/QjnuTpi4l1tpy/mfpLpmNBaVkgMQZIh2Si3QHKTx8l954PsgxfDT7eousGfGe\n" +
            "gE8tKIOc6THCquTMccXfAOKFTKHkApEnAqIxvpYBcEb3FSECAwEAAaNmMGQwEgYD\n" +
            "VR0TAQH/BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFLJK6Myk\n" +
            "tu+12iEwUsA53C9SDOalMB8GA1UdIwQYMBaAFPt32dsoOolHURGvbR8Iagl94mG5\n" +
            "MAoGCCqGSM49BAMCA0cAMEQCIB/fVKkUas3qDTuTUSrmr/+Kgb8S5/k2/Bvad32u\n" +
            "AX5uAiAUcRUMBe/PTspLkKDE+IdCCyA54YyPJBIitGSKMtmZpw==\n" +
            "-----END CERTIFICATE-----\n";

    static final String LEAF_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIC1DCCAbygAwIBAgIBAzANBgkqhkiG9w0BAQsFADA8MQswCQYDVQQGEwJVUzER\n" +
            "MA8GA1UECgwIVGVzdCBQS0kxGjAYBgNVBAMMEVRlc3QgSW50ZXJtZWRpYXRlMCAX\n" +
            "DTI1MDEwMTAwMDAwMFoYDzIxMjUwMTAxMDAwMDAwWjAsMRAwDgYDVQQKDAdFeGFt\n" +
            "cGxlMRgwFgYDVQQDDA9hcGkuZXhhbXBsZS5jb20wWTATBgcqhkjOPQIBBggqhkjO\n" +
            "PQMBBwNCAARppzDc+mmcp+kDUWh0p0h3AoLybuwlMGW1wTW1tFVXtpWAU1qGe+w8\n" +
            "6C4ImkEtcMDchQksvwm752Dv6AEf+tzlo4G5MIG2MAwGA1UdEwEB/wQCMAAwDgYD\n" +
            "VR0PAQH/BAQDAgeAMBMGA1UdJQQMMAoGCCsGAQUFBwMBMEEGA1UdEQQ6MDiCD2Fw\n" +
            "aS5leGFtcGxlLmNvbYINKi5leGFtcGxlLmNvbYcECgAAAYcQIAENuAAAAAAAAAAA\n" +
            "AAAAATAfBgNVHSMEGDAWgBSySujMpLbvtdohMFLAOdwvUgzmpTAdBgNVHQ4EFgQU\n" +
            "tMtA63i61HXJpsmTRRB1XYKwOIIwDQYJKoZIhvcNAQELBQADggEBAFaPX90HbErp\n" +
            "jyROdNaeiOROY8LIAwTMGmpBAG61IH03xkSV/9b++/yIRL1cXpD/hY8O1tbCnLgE\n" +
            "hqaScfQ3X4CcvdkKoNF4kL5mX9BONm0gs2d9PFtdETTKI5NomTWTC5ANMLYfqdSY\n" +
            "f9HhPyvJq2M6rub9ILvAhRIit7mDx6IxCpr1kSQPU2y9PDSM7qEgzj7y3/ndTK8i\n" +
            "/ANp8HobveucQHvJLwWpq4tEymYZEopeVKsmIXh43fghYxnTD57Fw9+Hwore8gfP\n" +
            "j24ALpQrsSwALYSQ+LG7hLEOewZ7ykaRX0nfuKyBZVY+Lu3jTa5ajh+Qhn+vRDFb\n" +
            "kkHAW2KxAks=\n" +
            "-----END CERTIFICATE-----\n";

    static final String CHAIN_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIC1DCCAbygAwIBAgIBAzANBgkqhkiG9w0BAQsFADA8MQswCQYDVQQGEwJVUzER\n" +
            "MA8GA1UECgwIVGVzdCBQS0kxGjAYBgNVBAMMEVRlc3QgSW50ZXJtZWRpYXRlMCAX\n" +
            "DTI1MDEwMTAwMDAwMFoYDzIxMjUwMTAxMDAwMDAwWjAsMRAwDgYDVQQKDAdFeGFt\n" +
            "cGxlMRgwFgYDVQQDDA9hcGkuZXhhbXBsZS5jb20wWTATBgcqhkjOPQIBBggqhkjO\n" +
            "PQMBBwNCAARppzDc+mmcp+kDUWh0p0h3AoLybuwlMGW1wTW1tFVXtpWAU1qGe+w8\n" +
            "6C4ImkEtcMDchQksvwm752Dv6AEf+tzlo4G5MIG2MAwGA1UdEwEB/wQCMAAwDgYD\n" +
            "VR0PAQH/BAQDAgeAMBMGA1UdJQQMMAoGCCsGAQUFBwMBMEEGA1UdEQQ6MDiCD2Fw\n" +
            "aS5leGFtcGxlLmNvbYINKi5leGFtcGxlLmNvbYcECgAAAYcQIAENuAAAAAAAAAAA\n" +
            "AAAAATAfBgNVHSMEGDAWgBSySujMpLbvtdohMFLAOdwvUgzmpTAdBgNVHQ4EFgQU\n" +
            "tMtA63i61HXJpsmTRRB1XYKwOIIwDQYJKoZIhvcNAQELBQADggEBAFaPX90HbErp\n" +
            "jyROdNaeiOROY8LIAwTMGmpBAG61IH03xkSV/9b++/yIRL1cXpD/hY8O1tbCnLgE\n" +
            "hqaScfQ3X4CcvdkKoNF4kL5mX9BONm0gs2d9PFtdETTKI5NomTWTC5ANMLYfqdSY\n" +
            "f9HhPyvJq2M6rub9ILvAhRIit7mDx6IxCpr1kSQPU2y9PDSM7qEgzj7y3/ndTK8i\n" +
            "/ANp8HobveucQHvJLwWpq4tEymYZEopeVKsmIXh43fghYxnTD57Fw9+Hwore8gfP\n" +
            "j24ALpQrsSwALYSQ+LG7hLEOewZ7ykaRX0nfuKyBZVY+Lu3jTa5ajh+Qhn+vRDFb\n" +
            "kkHAW2KxAks=\n" +
            "-----END CERTIFICATE-----\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIICkTCCAjigAwIBAgIBAjAKBggqhkjOPQQDAjA0MQswCQYDVQQGEwJVUzERMA8G\n" +
            "A1UECgwIVGVzdCBQS0kxEjAQBgNVBAMMCVRlc3QgUm9vdDAgFw0yNTAxMDEwMDAw\n" +
            "MDBaGA8yMTI1MDEwMTAwMDAwMFowPDELMAkGA1UEBhMCVVMxETAPBgNVBAoMCFRl\n" +
            "c3QgUEtJMRowGAYDVQQDDBFUZXN0IEludGVybWVkaWF0ZTCCASIwDQYJKoZIhvcN\n" +
            "AQEBBQADggEPADCCAQoCggEBAMfzuGX5KAkqd/nvFgjt5HxI57gTuTN1stlbHntu\n" +
            "E614t3h9/it/72gBrC9MWdVEwzusEs6YRthJwNisLO/KBmdTu31CKi8dsi4WKbJj\n" +
            "zhkiFIlDpfnSHLWfEoZ2p96FXdJ2YnvjcsXMkAyHb+RaHgy9n4eBw3LgyWFOS/Eg\n" +
            "z+6nDCDyKJxR80hCdOOE3l0c6Ky9uvgAxUrPwLWmIl4TmL6qLUD8V3Mx6oT/dap0\n" +
            "wu/QjnuTpi4l1tpy/mfpLpmNBaVkgMQZIh2Si3QHKTx8l954PsgxfDT7eousGfGe\n" +
            "gE8tKIOc6THCquTMccXfAOKFTKHkApEnAqIxvpYBcEb3FSECAwEAAaNmMGQwEgYD\n" +
            "VR0TAQH/BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFLJK6Myk\n" +
            "tu+12iEwUsA53C9SDOalMB8GA1UdIwQYMBaAFPt32dsoOolHURGvbR8Iagl94mG5\n" +
            "MAoGCCqGSM49BAMCA0cAMEQCIB/fVKkUas3qDTuTUSrmr/+Kgb8S5/k2/Bvad32u\n" +
            "AX5uAiAUcRUMBe/PTspLkKDE+IdCCyA54YyPJBIitGSKMtmZpw==\n" +
            "-----END CERTIFICATE-----\n";

    static final String EXPIRED_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIC0jCCAbqgAwIBAgIBBDANBgkqhkiG9w0BAQsFADA8MQswCQYDVQQGEwJVUzER\n" +
            "MA8GA1UECgwIVGVzdCBQS0kxGjAYBgNVBAMMEVRlc3QgSW50ZXJtZWRpYXRlMB4X\n" +
            "DTIwMDEwMTAwMDAwMFoXDTIxMDEwMTAwMDAwMFowLDEQMA4GA1UECgwHRXhhbXBs\n" +
            "ZTEYMBYGA1UEAwwPYXBpLmV4YW1wbGUuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0D\n" +
            "AQcDQgAEaacw3PppnKfpA1FodKdIdwKC8m7sJTBltcE1tbRVV7aVgFNahnvsPOgu\n" +
            "CJpBLXDA3IUJLL8Ju+dg7+gBH/rc5aOBuTCBtjAMBgNVHRMBAf8EAjAAMA4GA1Ud\n" +
            "DwEB/wQEAwIHgDATBgNVHSUEDDAKBggrBgEFBQcDATBBBgNVHREEOjA4gg9hcGku\n" +
            "ZXhhbXBsZS5jb22CDSouZXhhbXBsZS5jb22HBAoAAAGHECABDbgAAAAAAAAAAAAA\n" +
            "AAEwHwYDVR0jBBgwFoAUskrozKS277XaITBSwDncL1IM5qUwHQYDVR0OBBYEFLTL\n" +
            "QOt4utR1yabJk0UQdV2CsDiCMA0GCSqGSIb3DQEBCwUAA4IBAQCPURGdCkNIA8nT\n" +
            "HdKPL3pX4NChGvWX7bnMExKlpT2Gyb6++TK5Prs6ReJkMQVLn7qMfNKaPTyu5QEq\n" +
            "6mOaV0y15ubI0evCplJRWnz/HwjQRKNPJ9s2TLr2rebaCaRpNQr1Cp6cW42HJ9Cy\n" +
            "dWw3f5iTaEC1pcoaDA3rp2n5ciIsxQ93z+LQtaKb25IsBJ3TiwVVzswWBJUP57yA\n" +
            "pDyHucKeVDrngkXkHaCqsNcaa8LQuRzGE3O7//jScWlVctU6ncuyWfwGDCRGGgLG\n" +
            "SSW1aWg9Tv3Ygp8KCo5YeUfoQ3C9Yjr9PAuYnDVYR8sqZg737gbSU8W4EsQzijmQ\n" +
            "EVMj/caX\n" +
            "-----END CERTIFICATE-----\n";

    static final String CLIENT_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIICoDCCAYigAwIBAgIBBTANBgkqhkiG9w0BAQsFADA8MQswCQYDVQQGEwJVUzER\n" +
            "MA8GA1UECgwIVGVzdCBQS0kxGjAYBgNVBAMMEVRlc3QgSW50ZXJtZWRpYXRlMCAX\n" +
            "DTI1MDEwMTAwMDAwMFoYDzIxMjUwMTAxMDAwMDAwWjAsMRAwDgYDVQQKDAdFeGFt\n" +
            "cGxlMRgwFgYDVQQDDA9hcGkuZXhhbXBsZS5jb20wWTATBgcqhkjOPQIBBggqhkjO\n" +
            "PQMBBwNCAARppzDc+mmcp+kDUWh0p0h3AoLybuwlMGW1wTW1tFVXtpWAU1qGe+w8\n" +
            "6C4ImkEtcMDchQksvwm752Dv6AEf+tzlo4GFMIGCMAwGA1UdEwEB/wQCMAAwEwYD\n" +
            "VR0lBAwwCgYIKwYBBQUHAwIwHQYDVR0RBBYwFIISY2xpZW50LmV4YW1wbGUuY29t\n" +
            "MB0GA1UdDgQWBBS0y0DreLrUdcmmyZNFEHVdgrA4gjAfBgNVHSMEGDAWgBSySujM\n" +
            "pLbvtdohMFLAOdwvUgzmpTANBgkqhkiG9w0BAQsFAAOCAQEAUavwXNFhjjWxRNNP\n" +
            "ZLqWzrbvxCxw4Ygh9K1H0AR54iIetNJkLkHs/V7IO5CaolR4/vD3qqmixjRBGOCm\n" +
            "IU50yqgVNHo6aN1YqZeGKoHwAFj6P+cnRqscSxY2D9zDvEICHcCCqzvtioqoFqRY\n" +
            "qRMBr0KzZvccIlzBQgTRnRcZBKEmhmvOVLbD03k2PbW5F3r4f7wAgAbDezUb1kMU\n" +
            "rKeeTOLXU1o6JN6Z1iJP0crWjinTm0OQNBaap36Hl8velm7tkNKvLS6oBhzoDQ52\n" +
            "TTmu7sZE81RTn3HMLR5x/yf/Qyf1mxdMCckuQ5PNi9UMu8hWq3TznT2naBN2V08t\n" +
            "Eeal/g==\n" +
            "-----END CERTIFICATE-----\n";

    static final String CRITICAL_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIICoDCCAYigAwIBAgIBBjANBgkqhkiG9w0BAQsFADA8MQswCQYDVQQGEwJVUzER\n" +
            "MA8GA1UECgwIVGVzdCBQS0kxGjAYBgNVBAMMEVRlc3QgSW50ZXJtZWRpYXRlMCAX\n" +
            "DTI1MDEwMTAwMDAwMFoYDzIxMjUwMTAxMDAwMDAwWjAsMRAwDgYDVQQKDAdFeGFt\n" +
            "cGxlMRgwFgYDVQQDDA9hcGkuZXhhbXBsZS5jb20wWTATBgcqhkjOPQIBBggqhkjO\n" +
            "PQMBBwNCAARppzDc+mmcp+kDUWh0p0h3AoLybuwlMGW1wTW1tFVXtpWAU1qGe+w8\n" +
            "6C4ImkEtcMDchQksvwm752Dv6AEf+tzlo4GFMIGCMAwGA1UdEwEB/wQCMAAwGgYD\n" +
            "VR0RBBMwEYIPYXBpLmV4YW1wbGUuY29tMBYGA1UdIAEB/wQMMAowCAYGZ4EMAQIB\n" +
            "MB0GA1UdDgQWBBS0y0DreLrUdcmmyZNFEHVdgrA4gjAfBgNVHSMEGDAWgBSySujM\n" +
            "pLbvtdohMFLAOdwvUgzmpTANBgkqhkiG9w0BAQsFAAOCAQEAK4sMQ+VNuVHu4K9B\n" +
            "yWPrj5UjT+4OBSye7RSY34CMpyqCubvRUSWRj1UERZp99VokRz4oKKuclpPppGDM\n" +
            "rCKZrTd5Nkjru8HeLrhyR0b3f3Zw8yGGTGUGzc4E3OvYCQSQvM+pHJAuSzY9vV6h\n" +
            "aPWmhGqZT1Vc2SSKLHJ76w4wfzeIcGUYZnuYpsvdTn6FBEPZbqd5SQ1qUPj9acaH\n" +
            "Xc6wM709N/H0qD6lOq3K85uy2UmXNK0DLHafMSF4PwDGkt69RQMWMVNKGrFhPRXj\n" +
            "dHUG0t2u44+DtS2Y5zhLK+pFUVeML0vJ90NlJsV2ZgqRSHlRiUm1NRMg5CqCvAtY\n" +
            "aUNOeg==\n" +
            "-----END CERTIFICATE-----\n";

    static final String BYLEAF_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIChzCCAi2gAwIBAgIBBzAKBggqhkjOPQQDAjAsMRAwDgYDVQQKDAdFeGFtcGxl\n" +
            "MRgwFgYDVQQDDA9hcGkuZXhhbXBsZS5jb20wIBcNMjUwMTAxMDAwMDAwWhgPMjEy\n" +
            "NTAxMDEwMDAwMDBaMDwxCzAJBgNVBAYTAlVTMREwDwYDVQQKDAhUZXN0IFBLSTEa\n" +
            "MBgGA1UEAwwRVGVzdCBJbnRlcm1lZGlhdGUwggEiMA0GCSqGSIb3DQEBAQUAA4IB\n" +
            "DwAwggEKAoIBAQDH87hl+SgJKnf57xYI7eR8SOe4E7kzdbLZWx57bhOteLd4ff4r\n" +
            "f+9oAawvTFnVRMM7rBLOmEbYScDYrCzvygZnU7t9QiovHbIuFimyY84ZIhSJQ6X5\n" +
            "0hy1nxKGdqfehV3SdmJ743LFzJAMh2/kWh4MvZ+HgcNy4MlhTkvxIM/upwwg8iic\n" +
            "UfNIQnTjhN5dHOisvbr4AMVKz8C1piJeE5i+qi1A/FdzMeqE/3WqdMLv0I57k6Yu\n" +
            "Jdbacv5n6S6ZjQWlZIDEGSIdkot0Byk8fJfeeD7IMXw0+3qLrBnxnoBPLSiDnOkx\n" +
            "wqrkzHHF3wDihUyh5AKRJwKiMb6WAXBG9xUhAgMBAAGjYzBhMA8GA1UdEwEB/wQF\n" +
            "MAMBAf8wDgYDVR0PAQH/BAQDAgIEMB0GA1UdDgQWBBSySujMpLbvtdohMFLAOdwv\n" +
            "UgzmpTAfBgNVHSMEGDAWgBS0y0DreLrUdcmmyZNFEHVdgrA4gjAKBggqhkjOPQQD\n" +
            "AgNIADBFAiAxukgXOvHjUTAX2rYgVtSEwrlDe1J+At82cVMmdV2PewIhAJLibqRB\n" +
            "yruRFPild+xP2kdc8PnQYsHNRuF0NTw3S2OR\n" +
            "-----END CERTIFICATE-----\n";

    static final String SUB_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIICZzCCAU+gAwIBAgIBCDANBgkqhkiG9w0BAQsFADA8MQswCQYDVQQGEwJVUzER\n" +
            "MA8GA1UECgwIVGVzdCBQS0kxGjAYBgNVBAMMEVRlc3QgSW50ZXJtZWRpYXRlMCAX\n" +
            "DTI1MDEwMTAwMDAwMFoYDzIxMjUwMTAxMDAwMDAwWjAWMRQwEgYDVQQDDAtUZXN0\n" +
            "IFN1YiBDQTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABJrsHwU9O7+CoZBmTEFg\n" +
            "NLqQ2rVgr8d86hdveo6G56phET3b4Vqrz5S1W2VL8GtFiF9hDlDDlJUQj22N3O1T\n" +
            "QxOjYzBhMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgIEMB0GA1UdDgQW\n" +
            "BBTeVHDN98FTdPi74koYmWbnm/tpMzAfBgNVHSMEGDAWgBSySujMpLbvtdohMFLA\n" +
            "OdwvUgzmpTANBgkqhkiG9w0BAQsFAAOCAQEAUSyOzYijZLj4H9lR9QNRDrG3cdDS\n" +
            "K9iQUPgA7mM67lzZ6rSyxF4Oejj9znN9qocYR8LP1nPRfrFvmoE6RjoU8IxvdY8r\n" +
            "+QmRDHXhd2zgtx67TO18jDt53e1Xhc7ne+llEUK8YflrbZW0oOZqJLDilTfAD7QK\n" +
            "xzCEw9hJ8bdJ84gnCMv7GdFzMqUcVrJ/nnqRgvO9W09erlmknUGBdaFfN/qtz2s1\n" +
            "+nYyemkctGPoLa3WLYlE7d6QDkz4+bt9/r8cbYyRAeKyxGqCgE3VdBSC1FCfIsOO\n" +
            "tYdJLI0JCKuEoNfL2H4KVv7yhCL176WHDNLEkwxrH0ZYnwXbegafC3Y2Rw==\n" +
            "-----END CERTIFICATE-----\n";

    static final String UNDERSUB_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIB7jCCAZOgAwIBAgIBCTAKBggqhkjOPQQDAjAWMRQwEgYDVQQDDAtUZXN0IFN1\n" +
            "YiBDQTAgFw0yNTAxMDEwMDAwMDBaGA8yMTI1MDEwMTAwMDAwMFowLDEQMA4GA1UE\n" +
            "CgwHRXhhbXBsZTEYMBYGA1UEAwwPYXBpLmV4YW1wbGUuY29tMFkwEwYHKoZIzj0C\n" +
            "AQYIKoZIzj0DAQcDQgAEaacw3PppnKfpA1FodKdIdwKC8m7sJTBltcE1tbRVV7aV\n" +
            "gFNahnvsPOguCJpBLXDA3IUJLL8Ju+dg7+gBH/rc5aOBuTCBtjAMBgNVHRMBAf8E\n" +
            "AjAAMA4GA1UdDwEB/wQEAwIHgDATBgNVHSUEDDAKBggrBgEFBQcDATBBBgNVHREE\n" +
            "OjA4gg9hcGkuZXhhbXBsZS5jb22CDSouZXhhbXBsZS5jb22HBAoAAAGHECABDbgA\n" +
            "AAAAAAAAAAAAAAEwHwYDVR0jBBgwFoAU3lRwzffBU3T4u+JKGJlm55v7aTMwHQYD\n" +
            "VR0OBBYEFLTLQOt4utR1yabJk0UQdV2CsDiCMAoGCCqGSM49BAMCA0kAMEYCIQCq\n" +
            "koMtIBodF3BGi9hJVqFpA5Q9VzhEpojvwLC4lswxCgIhAJgd6SWZ7iX20dJTMJB8\n" +
            "2etbUv9JlSTAUrYtod1HVGH8\n" +
            "-----END CERTIFICATE-----\n";

    static final String PSS_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIDYDCCAhSgAwIBAgIUelaKLuawvO1B+Dqh0c3e+E63Yl8wQQYJKoZIhvcNAQEK\n" +
            "MDSgDzANBglghkgBZQMEAgEFAKEcMBoGCSqGSIb3DQEBCDANBglghkgBZQMEAgEF\n" +
            "AKIDAgEgMBMxETAPBgNVBAMMCFBTUyBSb290MCAXDTI1MDEwMTAwMDAwMFoYDzIx\n" +
            "MjUwMTAxMDAwMDAwWjATMREwDwYDVQQDDAhQU1MgUm9vdDCCASIwDQYJKoZIhvcN\n" +
            "AQEBBQADggEPADCCAQoCggEBAMfzuGX5KAkqd/nvFgjt5HxI57gTuTN1stlbHntu\n" +
            "E614t3h9/it/72gBrC9MWdVEwzusEs6YRthJwNisLO/KBmdTu31CKi8dsi4WKbJj\n" +
            "zhkiFIlDpfnSHLWfEoZ2p96FXdJ2YnvjcsXMkAyHb+RaHgy9n4eBw3LgyWFOS/Eg\n" +
            "z+6nDCDyKJxR80hCdOOE3l0c6Ky9uvgAxUrPwLWmIl4TmL6qLUD8V3Mx6oT/dap0\n" +
            "wu/QjnuTpi4l1tpy/mfpLpmNBaVkgMQZIh2Si3QHKTx8l954PsgxfDT7eousGfGe\n" +
            "gE8tKIOc6THCquTMccXfAOKFTKHkApEnAqIxvpYBcEb3FSECAwEAAaNCMEAwDwYD\n" +
            "VR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFLJK6Myktu+1\n" +
            "2iEwUsA53C9SDOalMEEGCSqGSIb3DQEBCjA0oA8wDQYJYIZIAWUDBAIBBQChHDAa\n" +
            "BgkqhkiG9w0BAQgwDQYJYIZIAWUDBAIBBQCiAwIBIAOCAQEAsrNNCGFLbZ2EAFiG\n" +
            "QgkewNLp5wR8s6KU+thQKwEgB3l4DSuL3kUhjWxfthgtzcQrJAMqfXuPR3KluQ7v\n" +
            "6MqO+34W0Dr/bYjjLtGBBNocGCmEAuZrTsDpgUiLBUvW0kCkST2+scGMCf+QdMCf\n" +
            "9wO42vPNoAyubZUHV7YlH3tVLTtF/3nHKRfpOQzfme+bzZoUgjAI4NkKOJWT/+72\n" +
            "uDK2G7RwbwFHIB8YJswWOQUFLINhsPaYIAXf4jR9CTNFRITbo5b37h6nLhQ3ud4m\n" +
            "TY42KYWlAsCg2XC8lKkdzYD7NVqBoF0cZ9MkkGeVPiLbWjej2eFKyDWstQi93Mnp\n" +
            "24/Org==\n" +
            "-----END CERTIFICATE-----\n";

    static final String P384_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIBrjCCATOgAwIBAgIUGWJA8C90Z+Y/VCJWlKq29e8VV3kwCgYIKoZIzj0EAwMw\n" +
            "FTETMBEGA1UEAwwKUC0zODQgUm9vdDAgFw0yNTAxMDEwMDAwMDBaGA8yMTI1MDEw\n" +
            "MTAwMDAwMFowFTETMBEGA1UEAwwKUC0zODQgUm9vdDB2MBAGByqGSM49AgEGBSuB\n" +
            "BAAiA2IABLRNowwoULl/H1HO44gmEbuN5QZYkW5FkdIKuZx8xo+Bo3h+nBj8Q5cq\n" +
            "5nf1gyNkS7xW4v4yQ9eH0NA1qksJ9uV8nZC4s5My7TAmm6Z+btj0RY5SUWU0gqGg\n" +
            "UoEj02p0LKNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwHQYD\n" +
            "VR0OBBYEFPhWj5prunBzM3ogiInkF2J3kSYnMAoGCCqGSM49BAMDA2kAMGYCMQCN\n" +
            "RQAuPcXNqNCssUtNrZ6RGFETbs2nz5OxgFbvlUKU8ojLmsaqg1r/3zDIHNTo0UkC\n" +
            "MQDC9VlhNhSzsIYlXpn4I9DjksELA+Eu1n5rtOTXuWowNeayvja2Q+jSk/niMAaM\n" +
            "dZw=\n" +
            "-----END CERTIFICATE-----\n";

    static final String SHA1_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIBbDCCAROgAwIBAgIUNaQnWbxaOyOZdl0LFmKORR4bdWcwCQYHKoZIzj0EATAU\n" +
            "MRIwEAYDVQQDDAlTSEExIFJvb3QwIBcNMjUwMTAxMDAwMDAwWhgPMjEyNTAxMDEw\n" +
            "MDAwMDBaMBQxEjAQBgNVBAMMCVNIQTEgUm9vdDBZMBMGByqGSM49AgEGCCqGSM49\n" +
            "AwEHA0IABBimcXlr2DRntHXslGllbyf2da33p45fkKWkx0IT08RtMPF2qJOiN9jq\n" +
            "w/sxepCBDSGXWhgAlsn/nvWDVV0JR+qjQjBAMA8GA1UdEwEB/wQFMAMBAf8wDgYD\n" +
            "VR0PAQH/BAQDAgEGMB0GA1UdDgQWBBT7d9nbKDqJR1ERr20fCGoJfeJhuTAJBgcq\n" +
            "hkjOPQQBA0gAMEUCIQDuLp1ja4uc+XBtN26yY6utEl99dWqhAaRIOQXex8RAQgIg\n" +
            "LMj18L2iPMCC08rvCLMqF0NkcPFGT4B2RlmxnTekDJQ=\n" +
            "-----END CERTIFICATE-----\n";

    static final String LEAF_KEY =
            "-----BEGIN PRIVATE KEY-----\n" +
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgUy2nKEMwBxmzFU+t\n" +
            "X8sT57m2EYUrM1KWtNgdmG2tRPahRANCAARppzDc+mmcp+kDUWh0p0h3AoLybuwl\n" +
            "MGW1wTW1tFVXtpWAU1qGe+w86C4ImkEtcMDchQksvwm752Dv6AEf+tzl\n" +
            "-----END PRIVATE KEY-----\n";

    static final String ROOT_KEY =
            "-----BEGIN PRIVATE KEY-----\n" +
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgvwBayVtulRcps69J\n" +
            "jgyI/5DPNXHVjpxZvBxVLM1nfZChRANCAAQYpnF5a9g0Z7R17JRpZW8n9nWt96eO\n" +
            "X5ClpMdCE9PEbTDxdqiTojfY6sP7MXqQgQ0hl1oYAJbJ/571g1VdCUfq\n" +
            "-----END PRIVATE KEY-----\n";

    static final String OPENSSL_CSR =
            "-----BEGIN CERTIFICATE REQUEST-----\n" +
            "MIIBEzCBugIBADAaMRgwFgYDVQQDDA9hcGkuZXhhbXBsZS5jb20wWTATBgcqhkjO\n" +
            "PQIBBggqhkjOPQMBBwNCAARppzDc+mmcp+kDUWh0p0h3AoLybuwlMGW1wTW1tFVX\n" +
            "tpWAU1qGe+w86C4ImkEtcMDchQksvwm752Dv6AEf+tzloD4wPAYJKoZIhvcNAQkO\n" +
            "MS8wLTArBgNVHREEJDAigg9hcGkuZXhhbXBsZS5jb22CD3d3dy5leGFtcGxlLmNv\n" +
            "bTAKBggqhkjOPQQDAgNIADBFAiAv9Uivt2Rh2ESUcms4Sc8XI23Le8eIVFxgPI1J\n" +
            "VTbYPwIhAIC+KR7YGYBN7DqA5Cr6RtaJH+uK4yA2eJKlATVwehp/\n" +
            "-----END CERTIFICATE REQUEST-----\n";

    /// Base64.
    static final String LEAF_P12 =
            "MIIIawIBAzCCCBkGCSqGSIb3DQEHAaCCCAoEgggGMIIIAjCCBpoGCSqGSIb3DQEHBqCCBoswggaH" +
            "AgEAMIIGgAYJKoZIhvcNAQcBMF8GCSqGSIb3DQEFDTBSMDEGCSqGSIb3DQEFDDAkBBDV+/vN1HUZ" +
            "vz/XTo77IM5mAgIIADAMBggqhkiG9w0CCQUAMB0GCWCGSAFlAwQBKgQQy9RAUx0OcwRxWGgtNm5z" +
            "n4CCBhDyUD02mBJW/0GSN//CsKlEIf1CNMKFFCeeB/5xCYROgNp70jW3eRL7CXAUyJ4GjoYjqa2D" +
            "ALYsJR7YXsdL2MziHFBPOgV8+lWoDj3jKQkGIvsXlXofA2rENUlnBLIu0k073J+h73ABV3P8k910" +
            "gTOXBEEg8yBpF+0eiTa+MUaWF8yBkA/0qoT0Dfba7j4qU4BGL+xLsp7Tb5SA0jBTbbEvKj5fC4m/" +
            "0sxSpVlFfDjo61WpSFYO4YgpxECG1A5JYsDVwCky3uwRTZl8K6isqFRKwxCYtYxsZiG/TyRzTSj+" +
            "YUgWz0A+QXPntjoMbBc9dKTe/qkOww0AEbSVuDLt+DEO0rIlJa4fq3DR/uoD3r16unP1gpLRyAF0" +
            "cUSBlW7kG3CXMOne/bMrbKoBmsibfU2TKDmaw/XQcVQKbCiElhXnZruJEanD0iMIldQVTbzj40zR" +
            "rPlKUn1XkYmOEQijU+Tfev54GeHpt0hRFyLQXb5x02IxleY7VrL+YA5PmZiUjbpn1Jy+vVZ85Dja" +
            "TRpsTL0EQVPeMEZNLnuh4g3Ly7OKyMEao1fJanQjqiv9+gKPI2XMzNSKFJc4BJvTOkACjkN0sotB" +
            "FxKWYy/hHBbrU/rIjp3V9o4KCrZzf1p14PQLTsMHQkjC+HgD/v8F6BiUvL0CoC9P/KaHwkWN1gW3" +
            "QwDY22TEc1CGAlmcroKhImCMNwi7QC8+77MIEPEqWgsgKygTco0mhisopQkBXhpReV0QYhTR7bLa" +
            "p7yseYyqgcIlNUVvQaVwKg5kFiGL3AdetOkeBsPuY0D0MaQJMc8SVcCy93rWAHM+P5hfCiLBF7sf" +
            "mKViWMszim+Q5aEM1TMqQyFLOgezTyTvI4cLZOcubMoRn/dki/wCz5XGW2x52raHxFuY+UpQJRRx" +
            "3TPxG6dtELfctvIPxDKL8ftZ9sMLODKT0OU7N5m+F2AbMKMmvy3Z67fecU+H0c3Wepe0a/tZj9DF" +
            "h/DpdEjBBSnCP7Di68y2dnGoOo70CVFPgP8r0NUcrX+5Vh5LsjOh2npp5wSCup/eFl3vt4OLOZG2" +
            "tT76xlWtAeV4cJEKMx0dqhDvoMFzcMLo7r1xUnDWVlgnTi9vkac34VsQKZZZz0wESpJIH643OEja" +
            "eqXYrM9hn+CP7Wl27jtHs9imiyP2VLIb0/+wZFLTN47TeSUeDp7iYStFo986toqKWTKTVS9bV+yt" +
            "jhQn4Xxu/ksjkpI0iQS3OtyYzb35vesyjXPaR8/XEXvkpq6KzmxoOlaqUfTNitA9xHawy1FIYs9j" +
            "s3NuuO6hjFqxX7LPPRUCAaiC3X5YDAmKBq2I3SdlI0kdFyKWlZQNmYxd43nH90EQXIeeXzeg1WM0" +
            "8KZQpvjUol2ZLgSREpTKx8M12mJjLVNod5dH25j6WRAXmvwc1iurliN/YqqSk+HE5njuZp709cb7" +
            "gxax7aJHVwI6W9T8GcmQjSu3LI8hzCTSSKrQz/pCYxUKW3cMP3Az4TMJQ9HBJtiPMGT89+DkSBV3" +
            "J68Yl+CMN3q21mqi9yP5BvR1qmmnstAIqZVPwtKMwu9IGR7DBeIVMkpCaTyFZuQRuIkBJBQjN2pw" +
            "hFf8RNAUBsEO78uhD4icH3Is9Fo0IanIwFT2KSRYpX0ccW5gp6sPDrqVKTow6dK8uEqnVaMkNp7w" +
            "ct+SnS7FFlwTirOB68OjSb+TRH4sUxqMKVG8Dnn5uBNolOYoeosYMN4oYVBR4VQq3XPBtX+HPNH0" +
            "x61/ZcrGgunDX3Pv43S+GREG5RD7dbXz93y1EIHzBJy8f8bjKXo0lm+CKcUQzpy0xBmJ9JCSxuzn" +
            "uB73liQZ48iSEH9ZrG3DHDXaA8b/hgx038+u8l54uOMi40smByZ0IC78Blbq5b+y7lID7MiKe5ic" +
            "d3wCPoYOZ4b3vPnXg18OaV0px8kh/A30IZRbI8puq9gvZGdGvjd5ZmbokEhBSm58hopMA8jgNDOn" +
            "482N6gqxIGzFDNbrnMVikZZp89HXjjMxrak4mIGIgY5DcZfiE6uGOZdSYd8A9wEgB/y72NdD6Jae" +
            "It9Ea9UgqSPDjz/CkDi3B58CMIIBYAYJKoZIhvcNAQcBoIIBUQSCAU0wggFJMIIBRQYLKoZIhvcN" +
            "AQwKAQKggfcwgfQwXwYJKoZIhvcNAQUNMFIwMQYJKoZIhvcNAQUMMCQEEEKaA3MViHCYf3CYTuOO" +
            "htQCAggAMAwGCCqGSIb3DQIJBQAwHQYJYIZIAWUDBAEqBBCIPpU5pwyOWTeOFdAoszlKBIGQ1U32" +
            "8UQiWdSDceMXG9Thw8mxrXCMwC2MwUFrwOhgAPeIoTyccD37zQ6pChtcN22LKl/UZsk+OuSu1zF4" +
            "Om7Urp7GhgqfNcbLRNpn/NjNuXSouMcb0cIFbxKYKYqad5JdEoXMVgw93C1KaOKSnBkZmMzRNe4F" +
            "y1EzhhlSK5DR4y1hJylZpLZFc6iKYPZxJxmBMTwwFQYJKoZIhvcNAQkUMQgeBgBhAHAAaTAjBgkq" +
            "hkiG9w0BCRUxFgQU7Hz0VIaI1hP6kTTMRc3U71D83bQwSTAxMA0GCWCGSAFlAwQCAQUABCDhKwjC" +
            "wvGZcySeRwhkor1rZF12UefUx4qMG6Mhwd5bWgQQjxsnyqG4RO8XmiO4boex5wICCAA=";

    /// Base64.
    static final String LEGACY_P12 =
            "MIIFDQIBAzCCBMsGCSqGSIb3DQEHAaCCBLwEggS4MIIEtDCCA48GCSqGSIb3DQEHBqCCA4AwggN8" +
            "AgEAMIIDdQYJKoZIhvcNAQcBMBwGCiqGSIb3DQEMAQYwDgQI83YRRqXTwRYCAggAgIIDSAxRcWVM" +
            "VzXLf7HkJJmQG5dg4V6UoaivoaCK4XFk8IKW+rLwqKxq3wqUZFDo0GBgdLR5vpHTNkK4hrP1kHHJ" +
            "L3Fvutq7IOfmMp/pcQtdn9X1H3vnDTb8nxRdbkKh8zrpU+C/HGmvmUD21lK2sBVKCzj8BHHv7kSg" +
            "jOorg5g6RLgcouES2oE/ChR6S+rZpPkH8DVv0Bsf/ljpxKcQxQ6N9v1DkTfQDGGH8l186T/y2SGO" +
            "qQU6aPoJaYC+l1+U7CyASrSYBtkCKc1rCftXto6rlfR404NUZGGLrw3V/sxAxuiXJI/BpoYgpaD7" +
            "L4V935ugC0onM4IJYTC5lqSYzd2LbV1jtaBBIOOhG6llUlJMyc/S8P/Ns1a6nQtrSMuduimUHtGb" +
            "dz/jJz0h7JgBoNLp5mEysUeNulV44IBDH0ZbusLu3LLKqW5qj++cEqxEQ1oPhWkRp1k4amgTEgJa" +
            "QoLKn+uw5jQU2dPMz8DWdWhWdohgqzFc9E3M9ODxX8+rnOyOQoRKnq4cb4emIoRtsxuO23zy5FWY" +
            "D+Fd9RHLYXAeWh48RoAS79Xkg0fosaKmgIWhowAaXZJAmLySCpHL0povfjfd7shHl1SOGa1dfoX5" +
            "2nbejO+8tNO8XVa0kjAcYJt6t8TkvwlyphkIsFSbNK/ah6qNKGmWvGg6WZYPqDX7mCXpoIkkBU4x" +
            "X/WS8+Lha0BWT+om5EEjrxhsIKlnP4Rw6KoyJpTQSE4gmZhOCL4K+xoNke2xoGpXkxxz1r27vg4f" +
            "d0B22yhBQwTecImZlbTxI7AcwR+WkKn+J6XrEWCzIKTEdP3nJO4lFgX76dKtTBlkqJaR6gTKuQdN" +
            "VL82GKoKJGsJep2sxXS+5eoX5+HWl8FptRGNNq+ddTHllpoywojvq2Ql8Wy0s0b2d0cRc6B/472e" +
            "DWuhEEdmzyT181hShmm1ztgEiqFNem6OIOD6q6dp1HKlDhkaII1qa60L656r7n7F81CgolnMYhkI" +
            "NnPyrtuseuVAb+4mmhnOnCDnsBs4F/mEN1OPy1fHe22+1N9/y/hbkK4rsMl4zqhUowuP/rESpm2s" +
            "4TkGjzI9IvQRGyo3O2AuYwAx2icYeKKp2YE8Lkkaw8HLuOsIdzCCAR0GCSqGSIb3DQEHAaCCAQ4E" +
            "ggEKMIIBBjCCAQIGCyqGSIb3DQEMCgECoIG0MIGxMBwGCiqGSIb3DQEMAQMwDgQIYWaPHDQQRKYC" +
            "AggABIGQJ6CUvk/UIqHPs6MGoaJFrbnE1RtCYilbVHelQv8kvbcBQR/sJwm2VvgqSR5Q42xUgmzM" +
            "7l1dhqb5+1p/+3d49mitKisBuxF0YRL8IlUA9bGdcHGpl085p25T8RB7cC16A9zKe4sY0Ffx6gVX" +
            "m9fr5O8iNxDNQCnpdr46s0Uz8DSJZvYoITxXN7M06e7syjwCMTwwFQYJKoZIhvcNAQkUMQgeBgBh" +
            "AHAAaTAjBgkqhkiG9w0BCRUxFgQU7Hz0VIaI1hP6kTTMRc3U71D83bQwOTAhMAkGBSsOAwIaBQAE" +
            "FGd4HS6DMCaS7yFTwucSh6UpxGu8BBAwtfwxVyyUK88wAvoPjWM1AgIIAA==";
}
