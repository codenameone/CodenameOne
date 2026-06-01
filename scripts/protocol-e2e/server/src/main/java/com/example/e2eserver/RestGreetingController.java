package com.example.e2eserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST transport, described by openapi.json on the client side. The CN1
 * @RestClient generated from that spec calls these endpoints.
 */
@RestController
public class RestGreetingController {

    @GetMapping("/api/greeting")
    public Map<String, Object> greeting(@RequestParam(name = "name", defaultValue = "world") String name) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("message", "Hello, " + name + "!");
        m.put("transport", "rest");
        return m;
    }

    /** Echoes the Authorization header back so the client can assert bearer-token plumbing. */
    @PostMapping("/api/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> body,
                                    @RequestHeader(name = "Authorization", required = false) String auth) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", String.valueOf(body.get("name")));
        m.put("message", "echo:" + body.get("name"));
        m.put("transport", "rest");
        m.put("authorization", auth);
        return m;
    }
}
