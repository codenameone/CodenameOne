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

import com.codename1.backend.annotations.Service;

/**
 * A service: business logic with no HTTP in it, which the controller is given.
 *
 * The build finds every &#64;Service, &#64;Component and &#64;Repository, works out
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
@Service
public class Greeter {
    /** Greets someone. */
    public String greet(String name) {
        return "Hello, " + name;
    }
}
