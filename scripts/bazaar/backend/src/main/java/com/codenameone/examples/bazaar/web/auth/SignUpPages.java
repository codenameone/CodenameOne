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
package com.codenameone.examples.bazaar.web.auth;

import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.mvc.Model;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.RegisterDto;
import com.codenameone.examples.bazaar.web.WebPages;
import com.codenameone.examples.bazaar.web.security.BotGuard;
import java.io.IOException;

/// A shopper creates an account on the web: `GET /auth/signup` draws the
/// form, `POST /auth/signup` opens the account with the same rules as the
/// app's register call (`Accounts.register`) and sends the shopper to sign
/// in. The post is in the web app chain, so it needs the CSRF token, and
/// [BotGuard] holds it to the fill-time token and the honeypot field the
/// sign-in form carries -- refused before this code runs.
@Controller
public class SignUpPages {
    private final BotGuard bots;
    private final Accounts accounts;

    public SignUpPages(BotGuard bots, Accounts accounts) {
        this.bots = bots;
        this.accounts = accounts;
    }

    @GetMapping("/auth/signup")
    public String form(Model model, HttpServer.Request request) throws IOException {
        return draw(new SignUpView(), model, request);
    }

    @PostMapping("/auth/signup")
    public String submit(Model model, HttpServer.Request request) throws IOException {
        SignUpView form = new SignUpView();
        form.email = field(request, "email");
        form.displayName = field(request, "displayName");
        form.phone = field(request, "phone");
        String password = field(request, "password");
        if (form.email.length() == 0 || form.email.indexOf('@') < 1 || form.email.length() > 200) {
            form.problem = "Enter your email address.";
        } else if (password.length() < 10 || password.length() > 200) {
            form.problem = "Choose a password of at least 10 characters.";
        } else if (!password.equals(field(request, "confirm"))) {
            form.problem = "The two passwords are not the same.";
        } else {
            try {
                accounts.register(new RegisterDto(form.email, password,
                        form.displayName.length() > 0 ? form.displayName : form.email, form.phone));
                return "redirect:/auth/signin?created=1";
            } catch (ResponseStatusException refused) {
                // An e-mail that is taken reads the same as the app's answer;
                // the reason is the account service's own wording.
                form.problem = refused.getReason() == null ? "That did not work."
                        : refused.getReason();
            }
        }
        return draw(form, model, request);
    }

    private String draw(SignUpView form, Model model, HttpServer.Request request)
            throws IOException {
        form.fillToken = bots.issue();
        model.addAttribute("form", form);
        com.codenameone.examples.bazaar.web.Shell shell =
                WebPages.anonymous(request, "Create an account");
        form.problem = com.codenameone.examples.bazaar.web.StoreText.translate(shell.t,
                form.problem);
        model.addAttribute("shell", shell);
        return "signup";
    }

    private static String field(HttpServer.Request request, String name) {
        String v;
        try {
            v = request.param(name);
        } catch (RuntimeException malformed) {
            v = null;
        }
        return v == null ? "" : v.trim();
    }
}
