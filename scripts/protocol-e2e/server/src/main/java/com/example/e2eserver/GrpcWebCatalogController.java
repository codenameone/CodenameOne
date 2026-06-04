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
import java.util.List;

/**
 * gRPC-Web endpoint for the e2e test (no Envoy). The CN1 gRPC client speaks
 * application/grpc-web+proto, so the gRPC-Web framing and the (standard)
 * protobuf wire format are handled directly. Exercises an enum field, a
 * repeated scalar, nested/repeated messages, and a double.
 *
 * Frame: [0x00][len BE32][message]  then  [0x80][len BE32][trailer].
 */
@RestController
public class GrpcWebCatalogController {

    private static final String CONTENT_TYPE = "application/grpc-web+proto";

    @PostMapping("/grpc/e2e.Catalog/GetProduct")
    public void getProduct(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long id = readVarintField1(readAll(request.getInputStream()));
        Catalog.Product p = Catalog.byId(id);
        byte[] msg = p == null ? new byte[0] : encodeProduct(p);
        respond(response, msg);
    }

    @PostMapping("/grpc/e2e.Catalog/ListProducts")
    public void listProducts(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long cat = readVarintField1(readAll(request.getInputStream()));
        List<Catalog.Product> products = cat == 0
                ? Catalog.all()
                : Catalog.byCategory(categoryForNumber((int) cat));
        ByteArrayOutputStream list = new ByteArrayOutputStream();
        for (Catalog.Product p : products) {
            writeMessageField(list, 1, encodeProduct(p));
        }
        respond(response, list.toByteArray());
    }

    // -- protobuf encoding -------------------------------------------

    private static byte[] encodeProduct(Catalog.Product p) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        writeVarintField(o, 1, p.id());
        writeStringField(o, 2, p.name());
        writeVarintField(o, 3, categoryNumber(p.category()));
        for (String tag : p.tags()) {
            writeStringField(o, 4, tag);
        }
        writeDoubleField(o, 5, p.rating());
        return o.toByteArray();
    }

    private static int categoryNumber(Catalog.Category c) {
        switch (c) {
            case BOOKS: return 1;
            case ELECTRONICS: return 2;
            case TOYS: return 3;
            default: return 0;
        }
    }

    private static Catalog.Category categoryForNumber(int n) {
        switch (n) {
            case 1: return Catalog.Category.BOOKS;
            case 2: return Catalog.Category.ELECTRONICS;
            case 3: return Catalog.Category.TOYS;
            default: return Catalog.Category.BOOKS;
        }
    }

    private static void writeVarintField(ByteArrayOutputStream out, int field, long value) {
        out.write((field << 3) | 0);
        writeVarint(out, value);
    }

    private static void writeStringField(ByteArrayOutputStream out, int field, String value) {
        byte[] b = value.getBytes(StandardCharsets.UTF_8);
        out.write((field << 3) | 2);
        writeVarint(out, b.length);
        out.write(b, 0, b.length);
    }

    private static void writeMessageField(ByteArrayOutputStream out, int field, byte[] msg) {
        out.write((field << 3) | 2);
        writeVarint(out, msg.length);
        out.write(msg, 0, msg.length);
    }

    private static void writeDoubleField(ByteArrayOutputStream out, int field, double value) {
        out.write((field << 3) | 1); // wire type I64
        long bits = Double.doubleToLongBits(value);
        for (int i = 0; i < 8; i++) {
            out.write((int) ((bits >>> (8 * i)) & 0xFF)); // little-endian
        }
    }

    private static void writeVarint(ByteArrayOutputStream out, long value) {
        long v = value;
        while ((v & ~0x7FL) != 0) {
            out.write((int) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        out.write((int) v);
    }

    /** Reads protobuf field 1 (a varint) from the gRPC-Web data frame body. */
    private static long readVarintField1(byte[] body) {
        if (body == null || body.length < 5) {
            return 0;
        }
        int len = ((body[1] & 0xFF) << 24) | ((body[2] & 0xFF) << 16)
                | ((body[3] & 0xFF) << 8) | (body[4] & 0xFF);
        int pos = 5;
        int end = Math.min(body.length, pos + len);
        while (pos < end) {
            int tag = body[pos++] & 0xFF;
            int field = tag >>> 3;
            int wire = tag & 0x7;
            if (wire == 0) {
                long[] r = readVarint(body, pos);
                pos = (int) r[1];
                if (field == 1) {
                    return r[0];
                }
            } else if (wire == 2) {
                long[] r = readVarint(body, pos);
                pos = (int) r[1] + (int) r[0];
            } else if (wire == 1) {
                pos += 8;
            } else {
                break;
            }
        }
        return 0;
    }

    private static long[] readVarint(byte[] b, int pos) {
        long value = 0;
        int shift = 0;
        while (pos < b.length) {
            int x = b[pos++] & 0xFF;
            value |= (long) (x & 0x7F) << shift;
            if ((x & 0x80) == 0) {
                break;
            }
            shift += 7;
        }
        return new long[] { value, pos };
    }

    // -- gRPC-Web framing --------------------------------------------

    private void respond(HttpServletResponse response, byte[] message) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeFrame(out, 0x00, message);
        writeFrame(out, 0x80, "grpc-status:0\r\ngrpc-message:\r\n".getBytes(StandardCharsets.UTF_8));
        byte[] payload = out.toByteArray();
        response.setStatus(200);
        response.setContentType(CONTENT_TYPE);
        response.setContentLength(payload.length);
        OutputStream os = response.getOutputStream();
        os.write(payload);
        os.flush();
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
