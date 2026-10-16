/*
 * Copyright (c) 2012, Codename One and/or its affiliates.
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
package ${package};

import com.codename1.backend.annotations.Component;

/**
 * A service: business logic with no HTTP in it, which the controller is given.
 *
 * The build finds every &#64;Component, works out
 * what each one's constructor needs, and writes the `new` calls into the entry
 * point it generates -- so this reads like Spring, and there is no container, no
 * reflection and no start-up scan once it runs. A missing or ambiguous dependency
 * is a build error that names the injection point.
 *
 * The same pass handles &#64;Autowired fields, &#64;Value configuration
 * settings, &#64;Transactional and &#64;Async methods, &#64;Scheduled jobs and
 * &#64;McpTool methods an agent can call. See the Backend chapter of the developer
 * guide, or the backend reference in this project's agent skill.
 */
@Component
public class Greeter {
    /** Greets someone. */
    public String greet(String name) {
        return "Hello, " + name;
    }
}
