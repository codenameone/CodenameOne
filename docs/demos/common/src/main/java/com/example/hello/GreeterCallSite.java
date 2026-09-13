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
