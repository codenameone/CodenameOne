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

import com.codename1.backend.Backend;
import com.codename1.backend.Base64;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.Customizer;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.pki.Certificate;
import com.codename1.backend.security.saml2.InMemoryRelyingPartyRegistrationRepository;
import com.codename1.backend.security.saml2.RelyingPartyRegistration;
import com.codename1.backend.security.saml2.Saml2AuthenticationException;
import com.codename1.backend.security.saml2.Saml2ResponseValidator;
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.backend.WiringEnvironment;
import com.codename1.impl.backend.security.SecuritySupport;

/// A server people sign in to through a SAML identity provider: one chain
/// with `saml2Login` and one registration. Its binary must hold no login
/// form, no password hashing, no OAuth2 client, no authorization server and
/// no passkeys -- and it checks, translated, a response the JDK's own XML
/// Signature implementation signed: the strict reader, the exclusive
/// canonicalization and the RSA verification all run natively here.
public final class LinkSaml {
    /// The identity provider's certificate.
    private static final String CERT = "MIICvDCCAaSgAwIBAgIQVwocOLxZzCmj65b8roGD4jANBgkqhkiG9w0BAQsFADAaMRgwFgYDVQQDDA9pZHAuZXhh"
            + "bXBsZS5jb20wHhcNMjYxMTE2MDMyMTAzWhcNMjYxMjE2MDMyMTAzWjAaMRgwFgYDVQQDDA9pZHAuZXhhbXBsZS5j"
            + "b20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCyA9hx99yjVNvvzPWT2idqhA2d1HuXXxhGC1gcWx6b"
            + "Ewx5wDjCCCAYccHt60DMYYe/8C3xooFZPXByFxDbB79IAeDAJhj4WlWHN98aXw/4BERzaWlGurjfjbXh5taNuhCN"
            + "QkbTlS7CiTdeyjbEVI7vsTOw+N05jtT8pl6wpOzPLGz17nXgDqa0ArE2gr2tzGnb6nR4WKpEQ0Ra6RTqGM7msphn"
            + "4k2hsEx63Yn8Zo94Uahup8o7S/SF1gu+k0K9AARYZZ1RNwcomaLLT75klr78Y0H1eyUkUkPWV+gilJPWbKNaHbAD"
            + "O8CfkqOzQrPCD9DG+i+wG3cnz3W538Rf66GjAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAABuRFeGvczQypjoFodL"
            + "4kJnotMrc0frFnsbvyoJCi66uI8NIFOYh8P3F3E3kAHlpdfhxY45ZqH6JpRXVMJOyCVCUg2YNSjp+tHU+n9PJnFy"
            + "T/kfiCE/vogvmYZa4wI7mrdeQGUwgm8lMspIdsACgVHD8ZPA2ZQB/MHMlHhUE9+5iObZtHIaWl9jjhDsJ5UxlFEk"
            + "sBGrWqOwDq+yEskpoi/C+KfcnSPWdu+dpuLUm7PapWq+RAQxP6V/KtHqxgmRzvv0h90hYE8H0V4JMIAIRr7ux/tx"
            + "3Wyqzl0Fk+JCUWm6365siu8Eh2F9hpMyZlQd9FUGBK5t6HDGXo+74u/+cjw=";
    /// A response signed by the JDK at 2026-01-01T00:00:00Z, for alice.
    private static final String RESPONSE = "PHNhbWxwOlJlc3BvbnNlIHhtbG5zOnNhbWxwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wi"
            + "IHhtbG5zOnNhbWw9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iIERlc3RpbmF0aW9uPSJo"
            + "dHRwOi8vbG9jYWxob3N0L2xvZ2luL3NhbWwyL3Nzby9jb3JwIiBJRD0iX3Jlc3AxIiBJc3N1ZUluc3RhbnQ9IjIw"
            + "MjYtMDEtMDFUMDA6MDA6MDAuMDAwWiIgVmVyc2lvbj0iMi4wIj4KICA8c2FtbDpJc3N1ZXI+aHR0cHM6Ly9pZHAu"
            + "ZXhhbXBsZS5jb20vbWV0YWRhdGE8L3NhbWw6SXNzdWVyPgogIDxzYW1scDpTdGF0dXM+PHNhbWxwOlN0YXR1c0Nv"
            + "ZGUgVmFsdWU9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpzdGF0dXM6U3VjY2VzcyIvPjwvc2FtbHA6U3Rh"
            + "dHVzPgogIDxzYW1sOkFzc2VydGlvbiBJRD0iX2Fzc2VydDEiIElzc3VlSW5zdGFudD0iMjAyNi0wMS0wMVQwMDow"
            + "MDowMC4wMDBaIiBWZXJzaW9uPSIyLjAiPgogICAgPHNhbWw6SXNzdWVyPmh0dHBzOi8vaWRwLmV4YW1wbGUuY29t"
            + "L21ldGFkYXRhPC9zYW1sOklzc3Vlcj48ZHM6U2lnbmF0dXJlIHhtbG5zOmRzPSJodHRwOi8vd3d3LnczLm9yZy8y"
            + "MDAwLzA5L3htbGRzaWcjIj48ZHM6U2lnbmVkSW5mbz48ZHM6Q2Fub25pY2FsaXphdGlvbk1ldGhvZCBBbGdvcml0"
            + "aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIvPjxkczpTaWduYXR1cmVNZXRob2Qg"
            + "QWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzA0L3htbGRzaWctbW9yZSNyc2Etc2hhMjU2Ii8+PGRz"
            + "OlJlZmVyZW5jZSBVUkk9IiNfYXNzZXJ0MSI+PGRzOlRyYW5zZm9ybXM+PGRzOlRyYW5zZm9ybSBBbGdvcml0aG09"
            + "Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNlbnZlbG9wZWQtc2lnbmF0dXJlIi8+PGRzOlRyYW5z"
            + "Zm9ybSBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIvPjwvZHM6VHJh"
            + "bnNmb3Jtcz48ZHM6RGlnZXN0TWV0aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS8wNC94bWxl"
            + "bmMjc2hhMjU2Ii8+PGRzOkRpZ2VzdFZhbHVlPkp1RDF0WkdEUjRBWG5pK25IMm81NFdZQlZxZVdZT3F0VHVaYThY"
            + "Y2t6Z2c9PC9kczpEaWdlc3RWYWx1ZT48L2RzOlJlZmVyZW5jZT48L2RzOlNpZ25lZEluZm8+PGRzOlNpZ25hdHVy"
            + "ZVZhbHVlPlU3bEFDNHN5MGgzMFdENU4yY1ZVeDgxcHEwRjlIZ0tHSWZZSG9mM1VKRHhhVlpZTHluV2t4QjlrbXZw"
            + "dkV5VFN3VEdXV3JvMGQvcFAmIzEzOwpZQ2dkRkgzc1UwbWw2ajBwaEJqYm8xVVZmeTlaK3doV1V1Y0hUWWNPMkxv"
            + "cjBzZ2xIWHo3WHY4Z252ZEZWTkd4S3ZOTUJETGlLL2ZaJiMxMzsKSndDWnhGaHBjVTFoSGlmb24yNjc5ZGV5eElS"
            + "TjFTQnRzdVpEV0gxa0MveUF3ZFc1ZDc0WFJvc29MRHRGYnBpQXdwWDRjeVdlVFZoZSYjMTM7CmtiTmxFZE9aNE1s"
            + "WlptVDFVS2l4WERuVEpUQnF6azQ3S25QZnZPNUlqaEJhTWE0RUVCSnZVanpuNTc0ajcwN1VZN3BZUWhZejVuYjAm"
            + "IzEzOwpiRzA1U0N6VVB1RXlkS3JjV3d6SjZNV1JOcklDVHZ1Mm9lWXk1dz09PC9kczpTaWduYXR1cmVWYWx1ZT48"
            + "L2RzOlNpZ25hdHVyZT4KICAgIDxzYW1sOlN1YmplY3Q+PHNhbWw6TmFtZUlEIEZvcm1hdD0idXJuOm9hc2lzOm5h"
            + "bWVzOnRjOlNBTUw6MS4xOm5hbWVpZC1mb3JtYXQ6ZW1haWxBZGRyZXNzIj5hbGljZUBjb3JwLmV4YW1wbGU8L3Nh"
            + "bWw6TmFtZUlEPgogICAgICA8c2FtbDpTdWJqZWN0Q29uZmlybWF0aW9uIE1ldGhvZD0idXJuOm9hc2lzOm5hbWVz"
            + "OnRjOlNBTUw6Mi4wOmNtOmJlYXJlciI+PHNhbWw6U3ViamVjdENvbmZpcm1hdGlvbkRhdGEgTm90T25PckFmdGVy"
            + "PSIyMDI2LTAxLTAxVDAwOjA1OjAwLjAwMFoiIFJlY2lwaWVudD0iaHR0cDovL2xvY2FsaG9zdC9sb2dpbi9zYW1s"
            + "Mi9zc28vY29ycCIvPjwvc2FtbDpTdWJqZWN0Q29uZmlybWF0aW9uPgogICAgPC9zYW1sOlN1YmplY3Q+CiAgICA8"
            + "c2FtbDpDb25kaXRpb25zIE5vdEJlZm9yZT0iMjAyNS0xMi0zMVQyMzo1OTowMC4wMDBaIiBOb3RPbk9yQWZ0ZXI9"
            + "IjIwMjYtMDEtMDFUMDA6MDU6MDAuMDAwWiI+PHNhbWw6QXVkaWVuY2VSZXN0cmljdGlvbj48c2FtbDpBdWRpZW5j"
            + "ZT5odHRwOi8vbG9jYWxob3N0L3NhbWwyL3NlcnZpY2UtcHJvdmlkZXItbWV0YWRhdGEvY29ycDwvc2FtbDpBdWRp"
            + "ZW5jZT48L3NhbWw6QXVkaWVuY2VSZXN0cmljdGlvbj48L3NhbWw6Q29uZGl0aW9ucz4KICAgIDxzYW1sOkF1dGhu"
            + "U3RhdGVtZW50IEF1dGhuSW5zdGFudD0iMjAyNi0wMS0wMVQwMDowMDowMC4wMDBaIiBTZXNzaW9uSW5kZXg9Il9z"
            + "ZXNzaW9uMSI+PHNhbWw6QXV0aG5Db250ZXh0PjxzYW1sOkF1dGhuQ29udGV4dENsYXNzUmVmPnVybjpvYXNpczpu"
            + "YW1lczp0YzpTQU1MOjIuMDphYzpjbGFzc2VzOlBhc3N3b3JkUHJvdGVjdGVkVHJhbnNwb3J0PC9zYW1sOkF1dGhu"
            + "Q29udGV4dENsYXNzUmVmPjwvc2FtbDpBdXRobkNvbnRleHQ+PC9zYW1sOkF1dGhuU3RhdGVtZW50PgogICAgPHNh"
            + "bWw6QXR0cmlidXRlU3RhdGVtZW50PjxzYW1sOkF0dHJpYnV0ZSBOYW1lPSJncm91cHMiPjxzYW1sOkF0dHJpYnV0"
            + "ZVZhbHVlPmFkbWluczwvc2FtbDpBdHRyaWJ1dGVWYWx1ZT48c2FtbDpBdHRyaWJ1dGVWYWx1ZT5zdGFmZjwvc2Ft"
            + "bDpBdHRyaWJ1dGVWYWx1ZT48L3NhbWw6QXR0cmlidXRlPjwvc2FtbDpBdHRyaWJ1dGVTdGF0ZW1lbnQ+CiAgPC9z"
            + "YW1sOkFzc2VydGlvbj4KPC9zYW1scDpSZXNwb25zZT4=";
    private static final long SIGNED_AT = 1767225600000L;
    private static final String ACS = "http://localhost/login/saml2/sso/corp";
    private static final String SP = "http://localhost/saml2/service-provider-metadata/corp";

    private LinkSaml() {
    }

    public static void main(String[] args) throws Exception {
        final RelyingPartyRegistration registration = RelyingPartyRegistration
                .withRegistrationId("corp")
                .assertingPartyEntityId("https://idp.example.com/metadata")
                .singleSignOnServiceLocation("https://idp.example.com/sso")
                .verificationCertificate(Certificate.decode(Base64.decode(CERT)))
                .build();
        byte[] response = Base64.decode(RESPONSE);
        String name;
        try {
            name = new Saml2ResponseValidator().validate(response, registration, ACS, SP, null,
                    SIGNED_AT).getName();
        } catch (Saml2AuthenticationException refused) {
            name = refused.getErrorCode();
        }
        String text = new String(response, "UTF-8");
        byte[] tampered = text.replace("alice@", "admin@").getBytes("UTF-8");
        String forged;
        try {
            forged = new Saml2ResponseValidator().validate(tampered, registration, ACS, SP, null,
                    SIGNED_AT).getName();
        } catch (Saml2AuthenticationException refused) {
            forged = refused.getErrorCode();
        }

        Backend.Builder builder = Backend.builder().quiet().host("127.0.0.1").port(0);
        BackendAccess.get().application(builder, new LinkApp() {
            @Override
            void chains(WiringEnvironment environment) {
                HttpSecurity http = SecuritySupport.http(environment.getConfig(), new Object[0]);
                http.authorizeHttpRequests(auth -> auth
                            .requestMatchers("/open").permitAll()
                            .anyRequest().authenticated())
                    .saml2Login(saml -> saml.relyingPartyRegistrationRepository(
                            new InMemoryRelyingPartyRegistrationRepository(registration)))
                    .saml2Logout(Customizer.withDefaults());
                environment.registerSecurityFilterChain(http.build(), 1);
            }
        });
        BackendAccess.get().security(builder);
        Backend backend = builder.start();
        int open = LinkApp.status(backend, "GET", "/open");
        HttpServer.Response anonymous = LinkApp.send(backend, "GET", "/home", null,
                "Accept", "text/html");
        int redirect = BackendAccess.get().status(anonymous);
        String location = LinkApp.header(anonymous, "Location");
        HttpServer.Response start = LinkApp.send(backend, "GET", "/saml2/authenticate/corp", null);
        String sentTo = LinkApp.header(start, "Location");
        boolean asked = sentTo != null
                && sentTo.startsWith("https://idp.example.com/sso?SAMLRequest=");
        int metadata = LinkApp.status(backend, "GET", "/saml2/service-provider-metadata/corp");
        // A forged response posted to the ACS: refused, back to the login page.
        HttpServer.Response posted = LinkApp.send(backend, "POST", "/login/saml2/sso/corp",
                "SAMLResponse=" + Base64.encode(tampered).replace("+", "%2B")
                        .replace("/", "%2F").replace("=", "%3D"));
        String refused = LinkApp.header(posted, "Location");
        backend.stop();
        System.out.println("LINKCHECK saml open=" + open + " redirect=" + redirect + " to="
                + location + " asked=" + asked + " metadata=" + metadata + " verified=" + name
                + " forged=" + forged + " refused=" + refused);
        System.out.println(open == 200 && redirect == 302
                && "/saml2/authenticate/corp".equals(location) && asked && metadata == 200
                && "alice@corp.example".equals(name) && "invalid_signature".equals(forged)
                && "/login?error".equals(refused) ? "LINKCHECK OK" : "LINKCHECK FAILED");
    }
}
