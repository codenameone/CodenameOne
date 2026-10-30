/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.Header;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// The account a customer signs in to: opening one, reading it, and the
/// second factor that protects it.
///
/// Signing in itself is not here. It is the OAuth 2 authorization-code flow
/// with PKCE against `/login` and `/oauth2/**`, and passkeys against
/// `/webauthn/**`: none of it is a JSON call with a token, so none of it
/// belongs in a REST contract.
@RestClient
public interface AccountApi {
    /// Opens an account, and a US dollar checking account with it. The one
    /// call here that takes no token.
    @POST("/api/account/register")
    void register(@Body RegisterDto request, OnComplete<Response<UserDto>> callback);

    @GET("/api/me")
    void me(OnComplete<Response<UserDto>> callback);

    @POST("/api/account/password")
    void changePassword(@Body PasswordChangeDto change, OnComplete<Response<UserDto>> callback);

    /// Starts adding an authenticator app. Nothing changes at sign-in until
    /// [#confirmTotp] proves the app was set up. Both take the password after
    /// a password-only sign-in ([TotpStartDto]).
    @POST("/api/account/mfa/totp")
    void startTotp(@Body TotpStartDto start, OnComplete<Response<TotpEnrollmentDto>> callback);

    /// Finishes adding the app with a code it shows, and answers the recovery
    /// codes.
    @POST("/api/account/mfa/totp/confirm")
    void confirmTotp(@Body TotpCodeDto code, OnComplete<Response<RecoveryCodesDto>> callback);

    /// Replaces the recovery codes. Takes a current code from the app, so a
    /// stolen token alone cannot mint a way past the second factor.
    @POST("/api/account/mfa/recovery-codes")
    void newRecoveryCodes(@Body TotpCodeDto code,
            OnComplete<Response<RecoveryCodesDto>> callback);

    /// The account's passkeys. Adding one is a WebAuthn ceremony against the
    /// signed-in session (`/webauthn/register`), not a call here.
    @GET("/api/account/passkeys")
    void passkeys(OnComplete<Response<List<PasskeyDto>>> callback);

    /// Removes a passkey and answers the ones left. Signed by a device of the
    /// account or, with none, carrying the password ([PasskeyRemoveDto]).
    @POST("/api/account/passkeys/{id}/remove")
    void removePasskey(@Path("id") String id, @Header("Bastion-Signature") String signature,
            @Body PasskeyRemoveDto request, OnComplete<Response<List<PasskeyDto>>> callback);
}
