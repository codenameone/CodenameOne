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
package com.codenameone.examples.bazaar.merchant;

import com.codenameone.examples.bazaar.api.CheckoutSettingsDto;
import java.util.ArrayList;
import java.util.List;

/// The checkout settings' payment method switches: turning one type on or off
/// in the store's [CheckoutSettingsDto] while keeping its tips and fields as
/// they are. The server still refuses a type it does not know.
final class CheckoutMethods {
    private CheckoutMethods() {
    }

    /// Whether `settings` offers `type`.
    static boolean on(CheckoutSettingsDto settings, String type) {
        return settings != null && settings.methods() != null
                && settings.methods().contains(type);
    }

    /// `settings` with `type` switched to `wanted`, in the order the types
    /// were switched on; everything else is kept.
    static CheckoutSettingsDto switched(CheckoutSettingsDto settings, String type,
            boolean wanted) {
        List<String> methods = new ArrayList<String>();
        if (settings != null && settings.methods() != null) {
            for (int iter = 0; iter < settings.methods().size(); iter++) {
                String m = settings.methods().get(iter);
                if (!m.equals(type) && !methods.contains(m)) {
                    methods.add(m);
                }
            }
        }
        if (wanted) {
            methods.add(type);
        }
        if (settings == null) {
            return new CheckoutSettingsDto(methods, false, null, null);
        }
        return new CheckoutSettingsDto(methods, settings.tipsEnabled(), settings.tips(),
                settings.fields());
    }
}
