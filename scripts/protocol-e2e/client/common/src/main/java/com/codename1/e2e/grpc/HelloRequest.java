// Mirrors the output of cn1:generate-grpc for the e2e greeter.proto.
package com.codename1.e2e.grpc;

import com.codename1.annotations.grpc.ProtoField;
import com.codename1.annotations.grpc.ProtoMessage;

@ProtoMessage
public class HelloRequest {
    @ProtoField(tag = 1)
    public String name;

    public HelloRequest() {}
}
