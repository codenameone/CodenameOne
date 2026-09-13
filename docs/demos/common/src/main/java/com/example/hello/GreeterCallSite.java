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
package com.example.hello;

import com.codename1.components.ToastBar;
import java.util.Arrays;

/// Call site for the `@GrpcClient` interface `cn1:generate-grpc` emits.
/// Included by the developer guide's `generate-grpc` appendix.
class GreeterCallSite {

    // tag::appendix-goal-generate-grpc-java-002[]
    void sayHello(String bearerToken) {
        GreeterGrpc greeter = GreeterGrpc.of("https://grpc.example.com");
        HelloRequest request = new HelloRequest("Ada", Arrays.asList("ada", "countess"), Mood.HAPPY);

        greeter.sayHello(request, bearerToken, response -> {
            if (response.isOk() && response.getResponseData() != null) {
                ToastBar.showInfoMessage(response.getResponseData().message());
            } else if (response.isOk()) {
                ToastBar.showErrorMessage("The server returned an empty reply");
            } else {
                // getResponseCode() is the gRPC status, not the HTTP one --
                // a gRPC-Web call can carry a failure under HTTP 200.
                ToastBar.showErrorMessage("gRPC status " + response.getResponseCode()
                        + ": " + response.getResponseErrorMessage());
            }
        });
    }
    // end::appendix-goal-generate-grpc-java-002[]
}
