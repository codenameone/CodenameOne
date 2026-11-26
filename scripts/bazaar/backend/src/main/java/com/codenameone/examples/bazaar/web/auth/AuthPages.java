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
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.mvc.Model;
import com.codenameone.examples.bazaar.web.Shell;
import com.codenameone.examples.bazaar.web.StoreText;
import com.codenameone.examples.bazaar.web.WebPages;

/// The sign-in pages of the web app. The posts are the security chain's own
/// ([com.codenameone.examples.bazaar.web.security.WebSecurity]): the password
/// at `POST /auth/signin`, the authenticator code at `POST /auth/2fa`, signing
/// out at `POST /auth/signout`. These only draw the forms.
@Controller
public class AuthPages {
    private final com.codenameone.examples.bazaar.web.security.BotGuard bots;

    public AuthPages(com.codenameone.examples.bazaar.web.security.BotGuard bots) {
        this.bots = bots;
    }

    @GetMapping("/auth/signin")
    public String signIn(@RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "locked", required = false) String locked,
            Model model, HttpServer.Request request) throws java.io.IOException {
        SignInView form = new SignInView();
        form.fillToken = bots.issue();
        if (locked != null) {
            form.problem = "Too many wrong passwords for that account. Try again in a few "
                    + "minutes.";
        } else if (error != null) {
            form.problem = "That email and password do not match an account.";
        }
        model.addAttribute("form", form);
        Shell shell = WebPages.anonymous(request, "Sign in");
        form.problem = StoreText.translate(shell.t, form.problem);
        model.addAttribute("shell", shell);
        return "signin";
    }

    @GetMapping("/auth/2fa")
    public String secondFactor(@RequestParam(value = "error", required = false) String error,
            Model model, HttpServer.Request request) {
        SignInView form = new SignInView();
        form.secondFactor = true;
        if (error != null) {
            form.problem = "That code is not right, or has been used. Try the next one.";
        }
        model.addAttribute("form", form);
        Shell shell = WebPages.anonymous(request, "Two-step verification");
        form.problem = StoreText.translate(shell.t, form.problem);
        model.addAttribute("shell", shell);
        return "signin";
    }
}
