// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::appendix-goal-generate-grpc-java-001[]
@GrpcClient("helloworld.Greeter")
public interface GreeterGrpc {

    @Rpc("SayHello")
    void sayHello(HelloRequest request,
                  @Header("Authorization") String bearerToken,
                  OnComplete<GrpcResponse<HelloReply>> callback);

    static GreeterGrpc of(String baseUrl) {
        return GrpcClients.create(GreeterGrpc.class, baseUrl);
    }
}
// end::appendix-goal-generate-grpc-java-001[]

// tag::appendix-goal-generate-grpc-java-002[]
GreeterGrpc g = GreeterGrpc.of("https://api.example.com");
HelloRequest req = new HelloRequest();
req.name = "world";
g.sayHello(req, "Bearer " + token, response -> {
    if (response.isOk()) {
        renderGreeting(response.getResponseData().message);
    } else {
        showError("gRPC status " + response.getResponseCode()
                  + ": " + response.getResponseErrorMessage());
    }
});
// end::appendix-goal-generate-grpc-java-002[]
