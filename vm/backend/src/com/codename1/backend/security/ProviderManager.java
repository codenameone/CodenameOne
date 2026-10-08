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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// An [AuthenticationManager] that asks a list of [AuthenticationProvider]s in
/// order and takes the first answer. A refusal that says the account itself is
/// unusable -- locked, disabled, expired -- ends the search; any other is
/// remembered while the remaining providers are tried, and thrown when none
/// accepts.
public class ProviderManager implements AuthenticationManager {
    private final List<AuthenticationProvider> providers;
    private final AuthenticationManager parent;
    private boolean eraseCredentialsAfterAuthentication = true;

    public ProviderManager(AuthenticationProvider... providers) {
        this(asList(providers), null);
    }

    public ProviderManager(List<AuthenticationProvider> providers) {
        this(providers, null);
    }

    /// @param parent asked when no provider here supports the token or accepts it
    public ProviderManager(List<AuthenticationProvider> providers, AuthenticationManager parent) {
        if (providers == null || (providers.isEmpty() && parent == null)) {
            throw new IllegalArgumentException("A parent AuthenticationManager or a list of "
                    + "AuthenticationProviders is required");
        }
        for (AuthenticationProvider provider : providers) {
            if (provider == null) {
                throw new IllegalArgumentException("providers list cannot contain null values");
            }
        }
        this.providers = Collections.unmodifiableList(new ArrayList<AuthenticationProvider>(providers));
        this.parent = parent;
    }

    private static List<AuthenticationProvider> asList(AuthenticationProvider[] providers) {
        List<AuthenticationProvider> out = new ArrayList<AuthenticationProvider>();
        if (providers != null) {
            for (AuthenticationProvider provider : providers) {
                out.add(provider);
            }
        }
        return out;
    }

    /// The providers, in the order they are asked.
    public List<AuthenticationProvider> getProviders() {
        return providers;
    }

    /// Whether an accepted token has its password dropped before it is returned;
    /// true unless changed.
    public void setEraseCredentialsAfterAuthentication(boolean erase) {
        this.eraseCredentialsAfterAuthentication = erase;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        Class<?> toTest = authentication.getClass();
        AuthenticationException last = null;
        Authentication result = null;
        for (AuthenticationProvider provider : providers) {
            if (!provider.supports(toTest)) {
                continue;
            }
            try {
                result = provider.authenticate(authentication);
                if (result != null) {
                    break;
                }
            } catch (AccountStatusException status) {
                // The account exists and may not be used: no other provider is
                // asked to let it in.
                throw status;
            } catch (AuthenticationServiceException broken) {
                throw broken;
            } catch (AuthenticationException refused) {
                last = refused;
            }
        }
        if (result == null && parent != null) {
            try {
                result = parent.authenticate(authentication);
            } catch (AuthenticationException refused) {
                last = refused;
            }
        }
        if (result != null) {
            if (eraseCredentialsAfterAuthentication
                    && result instanceof AbstractAuthenticationToken) {
                ((AbstractAuthenticationToken) result).eraseCredentials();
            }
            return result;
        }
        if (last == null) {
            last = new ProviderNotFoundException("No AuthenticationProvider found for "
                    + toTest.getName());
        }
        throw last;
    }
}
