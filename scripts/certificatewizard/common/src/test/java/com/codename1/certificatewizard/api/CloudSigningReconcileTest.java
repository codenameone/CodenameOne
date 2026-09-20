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
package com.codename1.certificatewizard.api;

import com.codename1.certificatewizard.api.SigningService.Result;
import com.codename1.certificatewizard.cloud.CertificatesApi;
import com.codename1.certificatewizard.cloud.ProfilesApi;
import com.codename1.certificatewizard.cloud.model.CertDTO;
import com.codename1.certificatewizard.cloud.model.CreateCertRequest;
import com.codename1.certificatewizard.cloud.model.CreateProfileRequest;
import com.codename1.certificatewizard.cloud.model.ProfileDTO;
import com.codename1.io.rest.Response;
import com.codename1.io.rest.Responses;
import com.codename1.util.OnComplete;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What "Sync with Apple" reports, which is not the same question as what it did.
///
/// Issue #5832: a developer deleted three provisioning profiles in the Apple portal, ran the
/// sync, was told "Synced with Apple", and watched all three stay on the screen. The profile
/// half of the sync was allowed to answer 404 or 405 and still be counted a success -- a
/// compatibility shim for "a wizard newer than the signing service", a window that never existed
/// in a release. 404 is also what the service answers when APPLE said 404, and the message being
/// discarded was literally "Apple no longer has this item ... Use Sync with Apple to bring your
/// Codename One account back in step".
class CloudSigningReconcileTest {

    private static final String TOKEN = "Bearer test";

    @Test
    void aProfileHalfThatAnsweredNotFoundIsAFailedSync() {
        Result<Void>[] seen = run(200, 404);

        assertNotNull(seen[0]);
        assertFalse(seen[0].ok, "a 404 from the profile reconcile must not read as a sync");
    }

    /// 405 was the other half of the same shim, and is no more a success than 404.
    @Test
    void aProfileHalfThatAnsweredMethodNotAllowedIsAFailedSync() {
        Result<Void>[] seen = run(200, 405);

        assertFalse(seen[0].ok);
    }

    /// The status the service now answers when it could not read Apple's inventory end to end,
    /// and so could not settle what Apple no longer holds. It was always reported; this pins that
    /// the report reaches the developer rather than being absorbed like the 404.
    @Test
    void aProfileHalfThatCouldNotCompleteIsAFailedSync() {
        Result<Void>[] seen = run(200, 502);

        assertFalse(seen[0].ok);
        assertNotNull(seen[0].error);
    }

    /// The certificate half is still reported first: its failure means the key is unusable, which
    /// is more useful than whichever of the two happened to answer first.
    @Test
    void aFailedCertificateHalfIsReportedWithoutAskingAboutProfiles() {
        boolean[] askedAboutProfiles = {false};
        Result<Void>[] seen = new Result[1];
        CloudSigningService.reconcile(TOKEN,
                certificatesAnswering(409),
                profilesAnswering(200, askedAboutProfiles),
                r -> seen[0] = r);

        assertFalse(seen[0].ok);
        assertFalse(askedAboutProfiles[0], "the profile half runs only after a clean certificate half");
    }

    /// And the half that keeps the rest honest: both clean is still a sync.
    @Test
    void bothHalvesCleanIsASync() {
        Result<Void>[] seen = run(200, 200);

        assertTrue(seen[0].ok);
    }

    /// 201 is a success too -- ok() is a 2xx test, not an equality against 200.
    @Test
    void anyTwoHundredFromEitherHalfIsASync() {
        Result<Void>[] seen = run(204, 201);

        assertTrue(seen[0].ok);
    }

    @SuppressWarnings("unchecked")
    private static Result<Void>[] run(int certStatus, int profileStatus) {
        Result<Void>[] seen = new Result[1];
        CloudSigningService.reconcile(TOKEN,
                certificatesAnswering(certStatus),
                profilesAnswering(profileStatus),
                r -> seen[0] = r);
        assertEquals(1, seen.length);
        assertNotNull(seen[0], "the callback must always be completed");
        return seen;
    }

    /// A CertificatesApi whose reconcile answers {@code status} and whose every other route is
    /// unreachable -- this test is about one call, and a fake that quietly answers the others
    /// would hide a change that started using them.
    private static CertificatesApi certificatesAnswering(int status) {
        return new CertificatesApi() {
            @Override
            public void reconcileCertificates(String bearerToken,
                    OnComplete<Response<List<CertDTO>>> callback) {
                assertEquals(TOKEN, bearerToken);
                callback.completed(status >= 200 && status < 300
                        ? Responses.<List<CertDTO>>of(status, List.of())
                        : Responses.<List<CertDTO>>failure(status, "no"));
            }

            @Override
            public void listCertificates(String bearerToken,
                    OnComplete<Response<List<CertDTO>>> callback) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void createCertificate(CreateCertRequest body, String bearerToken,
                    OnComplete<Response<CertDTO>> callback) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void downloadP12(Long id, String password, String bearerToken,
                    OnComplete<Response<String>> callback) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void revokeCertificate(Long id, String bearerToken,
                    OnComplete<Response<String>> callback) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static ProfilesApi profilesAnswering(int status) {
        return profilesAnswering(status, new boolean[1]);
    }

    /// {@code asked[0]} is set when the profile half is reached at all.
    private static ProfilesApi profilesAnswering(int status, boolean[] asked) {
        return new ProfilesApi() {
            @Override
            public void reconcileProfiles(String bearerToken,
                    OnComplete<Response<List<ProfileDTO>>> callback) {
                asked[0] = true;
                callback.completed(status >= 200 && status < 300
                        ? Responses.<List<ProfileDTO>>of(status, List.of())
                        : Responses.<List<ProfileDTO>>failure(status, "no"));
            }

            @Override
            public void listProfiles(String bearerToken,
                    OnComplete<Response<List<ProfileDTO>>> callback) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void createProfile(CreateProfileRequest body, String bearerToken,
                    OnComplete<Response<ProfileDTO>> callback) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void downloadProfile(Long id, String bearerToken,
                    OnComplete<Response<String>> callback) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void deleteProfile(Long id, String bearerToken,
                    OnComplete<Response<String>> callback) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
