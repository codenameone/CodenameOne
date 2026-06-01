// Mirrors the output of cn1:generate-grpc for the e2e greeter.proto.
package com.codename1.e2e.grpc;

import com.codename1.annotations.grpc.GrpcClient;
import com.codename1.annotations.grpc.Rpc;
import com.codename1.annotations.rest.Header;
import com.codename1.io.grpc.GrpcClients;
import com.codename1.io.grpc.GrpcResponse;
import com.codename1.util.OnComplete;

@GrpcClient("e2e.Greeter")
public interface GreeterGrpc {

    @Rpc("SayHello")
    void sayHello(HelloRequest request,
                  @Header("Authorization") String bearerToken,
                  OnComplete<GrpcResponse<HelloReply>> callback);

    static GreeterGrpc of(String baseUrl) {
        return GrpcClients.create(GreeterGrpc.class, baseUrl);
    }
}
