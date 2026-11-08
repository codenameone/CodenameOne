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
package com.codenameone.developerguide.backend.cli;

import com.codename1.cli.CommandLine.Command;
import com.codename1.cli.Console;
import com.codename1.cli.ProgressBar;
import com.codename1.cli.Prompt;
import com.codename1.cli.Spinner;
import com.codename1.cli.Table;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/// Developer guide, command-line chapter: the console API.
@Command(name = "deploy", description = "Deploys a service.")
public class Deploy implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        // tag::cli-console-output[]
        Console.print(new Table("Service", "Status", "Replicas")
                .align(2, Table.Align.RIGHT)
                .addRow("api", Console.style("running", Console.Style.GREEN), 3)
                .addRow("worker", Console.style("degraded", Console.Style.YELLOW), 2)
                .render());

        try (ProgressBar bar = new ProgressBar("upload", 40).unit("files")) {
            for (int i = 0; i < 40; i++) {
                upload(i);
                bar.step();
            }
        }

        Spinner spinner = Spinner.start("waiting for health checks");
        waitForHealthy();
        spinner.succeed("healthy");
        // end::cli-console-output[]

        // tag::cli-console-prompts[]
        String env = Prompt.select("Environment", Arrays.asList("staging", "production"));
        List<String> regions = Prompt.multiSelect("Regions", Arrays.asList("us-east", "eu-west", "ap-south"));
        String token = Prompt.password("API token");
        if (!Prompt.confirm("Deploy to " + env + "?", false)) {
            return 1;
        }
        // end::cli-console-prompts[]
        Console.println("deploying to " + regions + " with a " + token.length() + " character token");
        return 0;
    }

    private static void upload(int file) throws InterruptedException {
        Thread.sleep(5);
    }

    private static void waitForHealthy() throws InterruptedException {
        Thread.sleep(100);
    }
}
