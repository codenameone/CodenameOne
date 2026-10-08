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
package com.codename1.backend.security;

import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.core.userdetails.UserDetailsPasswordService;
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.core.userdetails.UsernameNotFoundException;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.crypto.PasswordEncoderFactories;
import java.util.concurrent.atomic.AtomicInteger;

/// Checks a username and password against a [UserDetailsService]: loads the user,
/// compares the password through a [PasswordEncoder], and refuses an account
/// that is locked, disabled or expired.
///
/// A username nobody has still costs one password comparison, against a hash
/// made for the purpose, so the time an answer takes does not say whether the
/// account exists. And a password stored in an older encoding is rewritten in
/// the current one on a successful sign-in, when a [UserDetailsPasswordService]
/// is there to store it.
public class DaoAuthenticationProvider implements AuthenticationProvider {
    private static final String USER_NOT_FOUND_PASSWORD = "userNotFoundPassword";

    private UserDetailsService userDetailsService;
    private PasswordEncoder passwordEncoder;
    private UserDetailsPasswordService userDetailsPasswordService;
    private boolean hideUserNotFoundExceptions = true;
    /// The hash a missing user's password is compared with; made on first use.
    private String userNotFoundEncodedPassword;
    /// Password checks under way in this process, whichever provider started
    /// them: the processor they compete for is one.
    private static final AtomicInteger CHECKING = new AtomicInteger();
    /// See [#setMaxConcurrentPasswordChecks]; 0 for no bound.
    private int maxConcurrentPasswordChecks;

    public DaoAuthenticationProvider() {
        this.passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    public DaoAuthenticationProvider(UserDetailsService userDetailsService) {
        this();
        this.userDetailsService = userDetailsService;
    }

    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public UserDetailsService getUserDetailsService() {
        return userDetailsService;
    }

    /// The encoder passwords are compared with; a delegating one unless set.
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        if (passwordEncoder == null) {
            throw new IllegalArgumentException("passwordEncoder cannot be null");
        }
        synchronized (this) {
            this.passwordEncoder = passwordEncoder;
            this.userNotFoundEncodedPassword = null;
        }
    }

    public synchronized PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    /// Where a password re-encoded on sign-in is stored; without one a password
    /// in an older encoding stays as it is.
    public void setUserDetailsPasswordService(UserDetailsPasswordService service) {
        this.userDetailsPasswordService = service;
    }

    /// The most password checks this provider lets run in the process at one
    /// time. A sign-in that would be one more is refused at once with a
    /// [ServiceBusyException], which a chain answers 503 with `Retry-After`;
    /// no bound unless set, or with `cn1.security.password.maxConcurrent` for
    /// the provider a chain makes itself.
    ///
    /// Checking a password is tens of milliseconds of processor that cannot be
    /// interrupted, on purpose. A request's thread is cheap while it waits on
    /// a socket, and is not while it hashes: every check holds one of the
    /// server's few host threads for its whole length. Without a bound, a burst
    /// of sign-ins -- or somebody guessing passwords -- takes them all, and
    /// every other request waits behind work it has nothing to do with. With
    /// one, the sign-ins beyond it are told to come back, and the rest of the
    /// server stays quick.
    ///
    /// The bound never queues: a queue of password checks is the same pile of
    /// work, served later to clients that have already given up. Something
    /// near the number of processors, less one or two for everything else, is
    /// a reasonable value.
    public synchronized void setMaxConcurrentPasswordChecks(int maxConcurrentPasswordChecks) {
        this.maxConcurrentPasswordChecks = maxConcurrentPasswordChecks < 0 ? 0
                : maxConcurrentPasswordChecks;
    }

    private synchronized int maxConcurrentPasswordChecks() {
        return maxConcurrentPasswordChecks;
    }

    /// Takes one of the places for a password check, or refuses.
    private void enterCheck() {
        int max = maxConcurrentPasswordChecks();
        if (CHECKING.incrementAndGet() > max && max > 0) {
            CHECKING.decrementAndGet();
            throw new ServiceBusyException("Too many sign-ins are being checked at once", 1);
        }
    }

    private static void leaveCheck() {
        CHECKING.decrementAndGet();
    }

    /// Whether an unknown username is reported as bad credentials, which is the
    /// default, rather than as a [UsernameNotFoundException] a client could tell
    /// apart from a wrong password.
    public void setHideUserNotFoundExceptions(boolean hide) {
        this.hideUserNotFoundExceptions = hide;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (userDetailsService == null) {
            throw new AuthenticationServiceException("A UserDetailsService must be set");
        }
        // Before the user is even looked up: a sign-in that will be turned away
        // should cost nothing, and say nothing about the account.
        enterCheck();
        try {
            return check(authentication);
        } finally {
            leaveCheck();
        }
    }

    private Authentication check(Authentication authentication) {
        String username = authentication.getPrincipal() == null ? "NONE_PROVIDED"
                : authentication.getName();
        Object credentials = authentication.getCredentials();
        String presented = credentials == null ? null : credentials.toString();
        PasswordEncoder encoder = getPasswordEncoder();
        UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException missing) {
            mitigateAgainstTimingAttack(encoder, presented);
            if (hideUserNotFoundExceptions) {
                // The cause stays on the exception, for the server's log; what
                // is answered is the message, which says no more than this.
                throw new BadCredentialsException("Bad credentials", missing);
            }
            throw missing;
        } catch (AuthenticationException refused) {
            throw refused;
        } catch (RuntimeException broken) {
            throw new AuthenticationServiceException(String.valueOf(broken.getMessage()), broken);
        }
        if (user == null) {
            throw new AuthenticationServiceException("UserDetailsService returned null, which "
                    + "is an interface contract violation");
        }
        // The account's own state first, as Spring orders it: a locked account
        // is reported as locked whatever password came with the request.
        if (!user.isAccountNonLocked()) {
            throw new LockedException("User account is locked");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("User is disabled");
        }
        if (!user.isAccountNonExpired()) {
            throw new AccountExpiredException("User account has expired");
        }
        if (presented == null || user.getPassword() == null
                || !encoder.matches(presented, user.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }
        if (!user.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("User credentials have expired");
        }
        if (userDetailsPasswordService != null && encoder.upgradeEncoding(user.getPassword())) {
            // The one moment the clear password is known: stored again in the
            // encoding new passwords get, so old hashes leave as users sign in.
            UserDetails upgraded = userDetailsPasswordService.updatePassword(user,
                    encoder.encode(presented));
            if (upgraded != null) {
                user = upgraded;
            }
        }
        UsernamePasswordAuthenticationToken result = UsernamePasswordAuthenticationToken
                .authenticated(user, credentials, user.getAuthorities());
        result.setDetails(authentication.getDetails());
        return result;
    }

    private void mitigateAgainstTimingAttack(PasswordEncoder encoder, String presented) {
        if (presented == null) {
            return;
        }
        String dummy;
        synchronized (this) {
            if (userNotFoundEncodedPassword == null) {
                userNotFoundEncodedPassword = encoder.encode(USER_NOT_FOUND_PASSWORD);
            }
            dummy = userNotFoundEncodedPassword;
        }
        encoder.matches(presented, dummy);
    }
}
