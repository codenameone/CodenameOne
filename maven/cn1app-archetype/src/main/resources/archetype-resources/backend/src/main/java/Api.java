/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package ${package};

import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The server side of this app.
 *
 * Routes are methods. The annotations are Spring's, under Codename One's package
 * names, and the build turns them into a router that matches on the request's own
 * bytes plus the `main` that serves them -- so there is no server lifecycle to
 * write here and no route table to keep in step by hand.
 *
 * While developing, run it with
 *
 *     mvn -pl backend -Dcodename1.platform=backend cn1:backend
 *
 * which starts on this JVM in a couple of seconds against the minute and a half a
 * native build takes, and whose protocol layer is the same source that ships. The
 * property is not optional: the backend module lives in a profile, so without it
 * Maven cannot see it in the reactor. Package it with
 *
 *     mvn -pl backend -Dcodename1.platform=backend cn1:backend-package
 *
 * to get a single native binary with no JVM to install beneath it.
 *
 * The local run deliberately does not terminate TLS, and therefore does not serve
 * HTTP/2. Build the binary when those are what you need to exercise.
 *
 * Settings come from application.properties in this module's ROOT -- next to its
 * pom.xml, not under src/main/resources -- overridden by
 * application-&lt;profile&gt;.properties and then by the environment -- which is what
 * lets this run against an in-memory SQLite database here and a managed
 * PostgreSQL in production without a branch in the code:
 *
 *     CN1_PROFILE=dev mvn -pl backend -Dcodename1.platform=backend cn1:backend
 *
 * A controller is a bean like any other, so its constructor says what it needs
 * and the generated entry point passes it in: another bean such as the Greeter
 * below, a com.codename1.backend.DataSource for SQL, or a
 * com.codename1.backend.orm.EntityManager for the daos generated from the
 * project's &#64;Entity classes.
 *
 * On the dev profile the running server also answers MCP at /mcp, with tools
 * that list its routes and beans, call its endpoints and query its database --
 * see the backend reference in this project's agent skill.
 */
@RestController
public class Api {
    private final Greeter greeter;

    public Api(Greeter greeter) {
        this.greeter = greeter;
    }

    /** What a load balancer polls. A String answer is sent as text. */
    @GetMapping("/healthz")
    public String health() {
        return "ok";
    }

    /** Delegates to the injected service. */
    @GetMapping("/greet/{name}")
    public String greet(@PathVariable("name") String name) {
        return greeter.greet(name);
    }

    /** Anything that is not a String is sent as JSON. */
    @GetMapping("/echo")
    public Map echo(@RequestParam(value = "say", defaultValue = "hello") String say) {
        Map out = new LinkedHashMap();
        out.put("say", say);
        return out;
    }
}
