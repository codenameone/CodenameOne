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
package com.codename1.backend.security.oauth2.client;

import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.SimpleGrantedAuthority;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.core.userdetails.UserDetailsManager;
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.core.userdetails.UsernameNotFoundException;
import com.codename1.backend.security.oauth2.core.OAuth2AuthenticationException;
import com.codename1.backend.security.oauth2.core.OAuth2Error;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Signs the user of an identity provider in as a user of the application's
/// own.
///
/// ```java
/// LinkingOAuth2UserService linking = new LinkingOAuth2UserService(identities, users);
/// linking.setCreateUsers(true);
/// http.oauth2Login(oauth2 -> oauth2
///         .userService(linking)
///         .oidcUserService(linking.oidc()));
/// ```
///
/// Who the provider's user is here is decided in this order:
///
/// 1. An identity -- this provider, this subject -- that is tied to a local
///    user already signs in as that user. Nothing else about the provider's
///    answer is consulted: not the email, which may have changed since.
/// 2. Otherwise the provider must say the user's email address is one it has
///    **verified**. An address it does not vouch for is refused with
///    [#EMAIL_NOT_VERIFIED], whether or not anybody here has it: tying an
///    account to an address somebody merely typed at a provider would hand
///    that account to them.
///
///    A provider whose registration names a
///    [ClientRegistration.ProviderDetails#getUserEmailsUri] -- GitHub, whose
///    user carries no such flag and often no address -- is asked there
///    instead, with the user's access token: the address is the one the list
///    marks both `primary` and `verified`, and whatever the user info said of
///    an address is not consulted. No such entry, or a list that cannot be
///    read (the `user:email` scope was not granted), is refused the same way.
/// 3. A verified address that is the name of a local user ties the identity to
///    that user, and signs in as them.
/// 4. A verified address nobody here has makes a new local user of that name
///    -- when [#setCreateUsers] allows it and the users are a
///    [UserDetailsManager] -- and is refused with [#ACCOUNT_NOT_FOUND]
///    otherwise.
///
/// The user that comes back is named after the local account and has its
/// authorities, with the provider's attributes beside them; a second factor
/// and remember-me, which go by name, then apply to it exactly as they do to a
/// sign-in with a password.
///
/// A new user has a password nobody knows: 256 random bits under an encoding
/// id no encoder has. They sign in through the provider until they set one.
public final class LinkingOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    /// The provider does not vouch for the user's email address.
    public static final String EMAIL_NOT_VERIFIED = "email_not_verified";
    /// Nobody here has the address, and new users are not made.
    public static final String ACCOUNT_NOT_FOUND = "account_not_found";
    /// The local user the identity is tied to cannot sign in.
    public static final String ACCOUNT_UNAVAILABLE = "account_unavailable";

    private final FederatedIdentityRepository identities;
    private final UserDetailsService users;
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2Delegate =
            new DefaultOAuth2UserService();
    private OAuth2UserService<OidcUserRequest, OidcUser> oidcDelegate = new OidcUserService();
    private boolean createUsers;
    private String emailAttribute = "email";
    private String emailVerifiedAttribute = "email_verified";
    private List<GrantedAuthority> newUserAuthorities = Arrays.<GrantedAuthority>asList(
            new SimpleGrantedAuthority("ROLE_USER"));

    /// @param users the application's users; a [UserDetailsManager] to let
    /// new ones be made
    public LinkingOAuth2UserService(FederatedIdentityRepository identities,
                                    UserDetailsService users) {
        if (identities == null || users == null) {
            throw new IllegalArgumentException("The identities and the users are required");
        }
        this.identities = identities;
        this.users = users;
    }

    /// Whether a verified address nobody here has becomes a new local user.
    /// Off unless set.
    public void setCreateUsers(boolean createUsers) {
        if (createUsers && !(users instanceof UserDetailsManager)) {
            throw new IllegalArgumentException("New users are made through a "
                    + "UserDetailsManager, and the users given are not one");
        }
        this.createUsers = createUsers;
    }

    /// What a new user is granted; `ROLE_USER` unless set.
    public void setNewUserAuthorities(Collection<? extends GrantedAuthority> authorities) {
        this.newUserAuthorities = new ArrayList<GrantedAuthority>(authorities);
    }

    /// The attributes that hold the address and whether it is verified;
    /// `email` and `email_verified` unless set.
    public void setEmailAttributes(String emailAttribute, String emailVerifiedAttribute) {
        this.emailAttribute = emailAttribute;
        this.emailVerifiedAttribute = emailVerifiedAttribute;
    }

    /// What reads the user from a provider without OpenID Connect.
    public void setOAuth2UserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> service) {
        this.oauth2Delegate = service;
    }

    /// What reads the user from an OpenID Connect provider.
    public void setOidcUserService(OAuth2UserService<OidcUserRequest, OidcUser> service) {
        this.oidcDelegate = service;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        if (userRequest instanceof OidcUserRequest) {
            return oidc().loadUser((OidcUserRequest) userRequest);
        }
        OAuth2User remote = oauth2Delegate.loadUser(userRequest);
        ClientRegistration registration = userRequest.getClientRegistration();
        Map<String, Object> attributes = remote.getAttributes();
        if (registration.getProviderDetails().getUserEmailsUri() != null) {
            // The list decides, and only the list: an address in the user info
            // is one the user chose to show, which says nothing of whose it is.
            attributes = new LinkedHashMap<String, Object>(attributes);
            attributes.remove(emailAttribute);
            attributes.remove(emailVerifiedAttribute);
            // Not asked for a user who is tied already: rule 1 needs no address.
            String address = identities.findUsername(registration.getRegistrationId(),
                    remote.getName()) != null ? null
                    : DefaultOAuth2UserService.primaryVerifiedEmail(registration,
                            userRequest.getAccessToken());
            if (address != null) {
                attributes.put(emailAttribute, address);
                attributes.put(emailVerifiedAttribute, Boolean.TRUE);
            }
        }
        UserDetails local = resolve(registration.getRegistrationId(), remote.getName(),
                attributes);
        return new DefaultOAuth2User(local.getUsername(), merge(local, remote), attributes);
    }

    /// This service for a provider with OpenID Connect.
    public OAuth2UserService<OidcUserRequest, OidcUser> oidc() {
        return new OAuth2UserService<OidcUserRequest, OidcUser>() {
            @Override
            public OidcUser loadUser(OidcUserRequest userRequest) {
                OidcUser remote = oidcDelegate.loadUser(userRequest);
                // The subject, whatever attribute the registration names users
                // by: it is the one thing of a user a provider never reissues.
                UserDetails local = resolve(
                        userRequest.getClientRegistration().getRegistrationId(),
                        remote.getSubject(), remote.getClaims());
                return new DefaultOidcUser(local.getUsername(), merge(local, remote),
                        remote.getIdToken(), remote.getClaims());
            }
        };
    }

    private static List<GrantedAuthority> merge(UserDetails local, OAuth2User remote) {
        List<GrantedAuthority> out = new ArrayList<GrantedAuthority>(local.getAuthorities());
        for (GrantedAuthority authority : remote.getAuthorities()) {
            boolean held = false;
            for (GrantedAuthority has : out) {
                held |= has.getAuthority().equals(authority.getAuthority());
            }
            if (!held) {
                out.add(authority);
            }
        }
        return out;
    }

    /// The local user of this identity, by the rules of the class.
    UserDetails resolve(String provider, String subject, Map<String, Object> attributes) {
        if (subject == null || subject.length() == 0) {
            throw refused("invalid_user_info_response", "The provider named no subject");
        }
        String linked = identities.findUsername(provider, subject);
        if (linked != null) {
            return usable(load(linked));
        }
        Object email = attributes.get(emailAttribute);
        Object verified = attributes.get(emailVerifiedAttribute);
        if (!(email instanceof String) || ((String) email).length() == 0
                || !(Boolean.TRUE.equals(verified) || "true".equals(verified))) {
            throw refused(EMAIL_NOT_VERIFIED, "The provider does not vouch for an email address "
                    + "of this user");
        }
        String address = (String) email;
        UserDetails local = load(address);
        if (local == null) {
            if (!createUsers) {
                throw refused(ACCOUNT_NOT_FOUND, "No local user has the provider's address");
            }
            ((UserDetailsManager) users).createUser(new User(address,
                    "{federated}" + OAuth2Parameters.random(32), newUserAuthorities));
            local = load(address);
            if (local == null) {
                throw refused(ACCOUNT_NOT_FOUND, "The new user could not be read back");
            }
        }
        usable(local);
        String tied = identities.link(provider, subject, local.getUsername());
        if (!tied.equals(local.getUsername())) {
            // Another request tied the identity first, and to someone else.
            local = usable(load(tied));
        }
        return local;
    }

    private UserDetails load(String username) {
        try {
            return users.loadUserByUsername(username);
        } catch (UsernameNotFoundException none) {
            return null;
        }
    }

    private static UserDetails usable(UserDetails user) {
        if (user == null || !user.isEnabled() || !user.isAccountNonLocked()
                || !user.isAccountNonExpired()) {
            throw refused(ACCOUNT_UNAVAILABLE, "The local user of this identity cannot sign in");
        }
        return user;
    }

    private static OAuth2AuthenticationException refused(String code, String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), message);
    }
}
