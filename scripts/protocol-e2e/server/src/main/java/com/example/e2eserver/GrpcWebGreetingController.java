package com.example.e2eserver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Minimal gRPC-Web endpoint for the e2e test. The CN1 gRPC client
 * (com.codename1.io.grpc.GrpcWeb) speaks application/grpc-web+proto, so rather
 * than run a real gRPC server behind an Envoy bridge we decode the gRPC-Web
 * framing and the (single-field) protobuf messages by hand.
 *
 * Request framing  : [0x00][len BE32][HelloRequest protobuf]
 * HelloRequest     : field 1 (string name)
 * Response framing : [0x00][len BE32][HelloReply protobuf]
 *                    [0x80][len BE32]["grpc-status:0\r\n..."] (trailer frame)
 * HelloReply       : field 1 (string message)
 *
 * Mapped at /grpc/{service}/{method}; the CN1 client posts to
 * <baseUrl>/e2e.Greeter/SayHello with baseUrl = http://host:8080/grpc.
 */
@RestController
public class GrpcWebGreetingController {

    private static final String CONTENT_TYPE = "application/grpc-web+proto";

    @PostMapping("/grpc/e2e.Greeter/SayHello")
    public void sayHello(HttpServletRequest request, HttpServletResponse response) throws IOException {
        byte[] body = readAll(request.getInputStream());
        String name = decodeName(body);
        if (name == null || name.isEmpty()) {
            name = "world";
        }

        byte[] reply = encodeStringField(1, "Hello, " + name + "!");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeFrame(out, 0x00, reply);
        writeFrame(out, 0x80, "grpc-status:0\r\ngrpc-message:\r\n".getBytes(StandardCharsets.UTF_8));

        response.setStatus(200);
        response.setContentType(CONTENT_TYPE);
        byte[] payload = out.toByteArray();
        response.setContentLength(payload.length);
        OutputStream os = response.getOutputStream();
        os.write(payload);
        os.flush();
    }

    /** Reads the gRPC-Web data frame and extracts protobuf field 1 (string). */
    private static String decodeName(byte[] body) {
        if (body == null || body.length < 5) {
            return null;
        }
        int len = ((body[1] & 0xFF) << 24) | ((body[2] & 0xFF) << 16)
                | ((body[3] & 0xFF) << 8) | (body[4] & 0xFF);
        int pos = 5;
        int end = Math.min(body.length, pos + len);
        while (pos < end) {
            int tag = body[pos++] & 0xFF;
            int field = tag >>> 3;
            int wire = tag & 0x7;
            if (wire == 2) { // length-delimited
                int[] r = readVarint(body, pos);
                int fieldLen = r[0];
                pos = r[1];
                if (field == 1) {
                    return new String(body, pos, fieldLen, StandardCharsets.UTF_8);
                }
                pos += fieldLen;
            } else if (wire == 0) { // varint
                pos = readVarint(body, pos)[1];
            } else {
                break;
            }
        }
        return null;
    }

    private static int[] readVarint(byte[] b, int pos) {
        int value = 0;
        int shift = 0;
        while (pos < b.length) {
            int x = b[pos++] & 0xFF;
            value |= (x & 0x7F) << shift;
            if ((x & 0x80) == 0) {
                break;
            }
            shift += 7;
        }
        return new int[] { value, pos };
    }

    /** Encodes a protobuf message with one length-delimited string field. */
    private static byte[] encodeStringField(int field, String value) {
        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write((field << 3) | 2); // tag: field, wire type LEN
        writeVarint(out, utf8.length);
        out.write(utf8, 0, utf8.length);
        return out.toByteArray();
    }

    private static void writeVarint(ByteArrayOutputStream out, int value) {
        int v = value;
        while ((v & ~0x7F) != 0) {
            out.write((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        out.write(v);
    }

    private static void writeFrame(ByteArrayOutputStream out, int flag, byte[] payload) {
        out.write(flag);
        out.write((payload.length >>> 24) & 0xFF);
        out.write((payload.length >>> 16) & 0xFF);
        out.write((payload.length >>> 8) & 0xFF);
        out.write(payload.length & 0xFF);
        out.write(payload, 0, payload.length);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
