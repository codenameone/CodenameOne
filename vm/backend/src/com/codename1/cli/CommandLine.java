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
package com.codename1.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Parses a command line into an annotated command and runs it.
///
/// A command is a class annotated [Command] that implements `Runnable` or
/// `java.util.concurrent.Callable<Integer>`. Its fields annotated [Option] and
/// [Parameters] receive the arguments:
///
/// ```java
/// @Command(name = "greet", mixinStandardHelpOptions = true, version = "greet 1.0",
///         description = "Prints a greeting.")
/// public class Greet implements Runnable {
///     @Option(names = {"-n", "--count"}, description = "How many times.",
///             defaultValue = "1")
///     private int count;
///
///     @Parameters(paramLabel = "<name>", description = "Who to greet.")
///     private String name;
///
///     public void run() {
///         for (int i = 0; i < count; i++) {
///             System.out.println("Hello " + name);
///         }
///     }
/// }
/// ```
///
/// The names follow picocli, so a picocli command usually needs only its imports
/// changed. Unlike picocli nothing is read by reflection: the build reads the
/// annotations and generates the parser's bindings, so a mistake in a
/// declaration is a build error, and the command runs the same on the JVM and
/// as a native executable.
///
/// The build also writes `main`. With one top-level command it runs that
/// command; with several, they become the subcommands of one named after the
/// application. In a backend that also has controllers, the server is the
/// `serve` subcommand and the command line with no arguments still starts it.
///
/// Exit codes: 0 for success, the command's own code from a `Callable`, 1 when
/// the command throws, 2 for a command line that does not parse.
public final class CommandLine {
    /// The exit code for a command line that does not parse.
    public static final int USAGE_ERROR = 2;

    /// What [#serve] answers when the server ran rather than a command. Every
    /// int a command can return is a valid exit code, negative ones included,
    /// so the marker is the one value whose exit status is indistinguishable
    /// from 0 anyway (`Integer.MIN_VALUE & 0xff` is 0).
    public static final int SERVED = Integer.MIN_VALUE;

    /// The exit code for a command that threw.
    public static final int EXECUTION_ERROR = 1;

    /// Marks a command class.
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    public @interface Command {
        /// The name the command is invoked by. Defaults to the class name in
        /// lower case.
        String name() default "";

        /// Other names the command answers to as a subcommand.
        String[] aliases() default {};

        /// Lines of help text.
        String[] description() default {};

        /// Lines `--version` prints.
        String[] version() default {};

        /// Adds `-h`/`--help` and `-V`/`--version`.
        boolean mixinStandardHelpOptions() default false;

        /// Command classes that are subcommands of this one.
        Class<?>[] subcommands() default {};
    }

    /// Marks a field that receives an option.
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    public @interface Option {
        /// The option's names, such as `-v` and `--verbose`.
        String[] names();

        /// Help text.
        String[] description() default {};

        /// Whether a command line without the option is an error.
        boolean required() default false;

        /// How many values one occurrence takes: `1`, `0..1`, `1..*`.
        String arity() default "";

        /// The placeholder printed for the value.
        String paramLabel() default "";

        /// The value used when the option is absent. `${env:NAME}` reads an
        /// environment variable.
        String defaultValue() default "";

        /// Whether `--no-<name>` sets a boolean to false.
        boolean negatable() default false;

        /// A separator that splits one value into several, such as `,`.
        String split() default "";

        /// Leaves the option out of the help.
        boolean hidden() default false;

        /// A converter for a type the build does not convert by itself.
        Class<?> converter() default void.class;
    }

    /// Marks a field that receives positional parameters.
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    public @interface Parameters {
        /// The positions it takes: `0`, `1..3`, `1..*`. Defaults to the next
        /// position, or the rest for an array or a list.
        String index() default "";

        /// Help text.
        String[] description() default {};

        /// How many values it takes.
        String arity() default "";

        /// The placeholder printed for it.
        String paramLabel() default "";

        /// The value used when it is absent.
        String defaultValue() default "";

        /// A separator that splits one value into several.
        String split() default "";

        /// Leaves it out of the help.
        boolean hidden() default false;

        /// A converter for a type the build does not convert by itself.
        Class<?> converter() default void.class;
    }

    /// Marks a field of a subcommand that receives its enclosing command.
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    public @interface ParentCommand {
    }

    /// Marks a [CommandSpec] field that receives the command's own model.
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    public @interface Spec {
    }

    /// Converts the text of an argument to a type the build does not know.
    ///
    /// An implementation needs a public no-argument constructor.
    public interface ITypeConverter<K> {
        /// Converts `value`; any exception reports an invalid value.
        K convert(String value) throws Exception;
    }

    /// Implemented by a `Runnable` command that has an exit code to report.
    public interface IExitCodeGenerator {
        /// The exit code of the command that just ran.
        int getExitCode();
    }

    /// Starts the server of a backend that also has commands. Generated code
    /// implements it; applications do not.
    public interface Server {
        /// Starts the server and blocks until it stops.
        void serve() throws Exception;
    }

    /// A command line that does not parse.
    public static class ParameterException extends RuntimeException {
        private final transient CommandSpec spec;

        /// Creates the exception for `spec`'s command line.
        public ParameterException(CommandSpec spec, String message) {
            super(message);
            this.spec = spec;
        }

        /// Creates the exception for `spec`'s command line, caused by `cause`.
        public ParameterException(CommandSpec spec, String message, Throwable cause) {
            super(message, cause);
            this.spec = spec;
        }

        /// The command whose arguments were wrong.
        public CommandSpec getCommandSpec() {
            return spec;
        }
    }

    private final CommandSpec spec;
    private PrintStream out = System.out;
    private PrintStream err = System.err;

    /// Creates a command line for the command `spec` describes.
    public CommandLine(CommandSpec spec) {
        this.spec = spec;
    }

    /// The model of the top-level command.
    public CommandSpec getCommandSpec() {
        return spec;
    }

    /// Where help, version and the command's output are printed. Defaults to
    /// `System.out`.
    public CommandLine setOut(PrintStream out) {
        this.out = out;
        return this;
    }

    /// Where errors are printed. Defaults to `System.err`.
    public CommandLine setErr(PrintStream err) {
        this.err = err;
        return this;
    }

    /// Parses `args`, runs the command they name, and answers the exit code.
    /// Never throws: a command line that does not parse prints the error and
    /// the usage and answers [#USAGE_ERROR]; a command that throws prints the
    /// error and answers [#EXECUTION_ERROR].
    public int execute(String... args) {
        String[] expanded;
        try {
            expanded = expandArgFiles(args == null ? new String[0] : args);
        } catch (ParameterException e) {
            err.println(e.getMessage());
            return USAGE_ERROR;
        }
        Parse parse = new Parse();
        try {
            parse(spec, null, expanded, 0, parse);
        } catch (ParameterException e) {
            err.println(e.getMessage());
            CommandSpec where = e.getCommandSpec() == null ? spec : e.getCommandSpec();
            usage(where, err);
            return USAGE_ERROR;
        } catch (Exception e) {
            err.println(e.getMessage() == null ? e.toString() : e.getMessage());
            return EXECUTION_ERROR;
        }
        if (parse.helpFor != null) {
            usage(parse.helpFor, out);
            out.flush();
            return 0;
        }
        if (parse.versionFor != null) {
            String[] lines = parse.versionFor.version();
            for (String line : lines) {
                out.println(line);
            }
            out.flush();
            return 0;
        }
        CommandSpec last = parse.command;
        CommandBinding binding = last.binding();
        if (binding == null || !binding.runnable()) {
            // A group of subcommands with no body of its own: invoked bare, it
            // has nothing to do but say what it can do.
            err.println("Missing required subcommand");
            usage(last, err);
            return USAGE_ERROR;
        }
        try {
            int code = binding.call(parse.instance);
            out.flush();
            return code;
        } catch (ParameterException e) {
            err.println(e.getMessage());
            usage(e.getCommandSpec() == null ? last : e.getCommandSpec(), err);
            return USAGE_ERROR;
        } catch (Prompt.Cancelled e) {
            // Ctrl-C at a prompt: the shell's convention for an interrupted
            // command, 128 + SIGINT, with nothing printed -- the prompt already
            // said it was cancelled.
            out.flush();
            return Prompt.CANCELLED_EXIT_CODE;
        } catch (Throwable t) {
            String m = t.getMessage();
            err.println("Error: " + (m == null ? t.toString() : m));
            return EXECUTION_ERROR;
        }
    }

    /// Prints the command's usage help to `System.out`.
    public void usage() {
        usage(spec, out);
    }

    /// Prints the usage help of `command` to `stream`.
    public static void usage(CommandSpec command, PrintStream stream) {
        stream.print(Help.render(command, Console.width()));
        stream.flush();
    }

    /// The entry point the build generates for an application whose top-level
    /// commands are `commands`: runs the command line and answers the exit code.
    ///
    /// One command is the top level itself; several become the subcommands of
    /// a group named `appName`.
    public static int run(String appName, CommandSpec[] commands, String[] args) {
        return new CommandLine(root(appName, commands, null)).execute(args);
    }

    /// The entry point the build generates for a backend. With no arguments it
    /// starts the server, as a backend always has. Otherwise the server is the
    /// `serve` subcommand beside `commands`.
    ///
    /// When there are no commands, only `-h`/`--help` is recognized, and any
    /// other argument starts the server as before, so an existing deployment
    /// that passes arguments a backend used to ignore keeps working.
    ///
    /// #### Returns
    ///
    /// the exit code, or [#SERVED] when the server ran and the process should
    /// end the way a server's does.
    public static int serve(String appName, CommandSpec[] commands, Server server,
                            String[] args) throws Exception {
        if (args == null || args.length == 0
                || ((commands == null || commands.length == 0)
                    && !isHelp(args))) {
            server.serve();
            return SERVED;
        }
        ServeBinding serve = new ServeBinding(server);
        CommandSpec root = root(appName, commands, serve.spec());
        int code = new CommandLine(root).execute(args);
        return serve.ran ? SERVED : code;
    }

    private static boolean isHelp(String[] args) {
        return args.length == 1 && ("-h".equals(args[0]) || "--help".equals(args[0]));
    }

    private static CommandSpec root(String appName, CommandSpec[] commands,
                                    CommandSpec serve) {
        int count = commands == null ? 0 : commands.length;
        if (count == 1 && serve == null) {
            return commands[0];
        }
        if (count == 1 && commands[0].binding() != null
                && commands[0].subcommand("serve") == null) {
            commands[0].addSubcommand(serve);
            return commands[0];
        }
        // A generated group has no @Command of its own to carry a version, so
        // it prints the first one its commands declare.
        String[] version = new String[0];
        for (int i = 0; i < count && version.length == 0; i++) {
            version = commands[i].version();
        }
        CommandSpec group = new CommandSpec(appName, new String[0], version, true, null);
        if (serve != null) {
            group.addSubcommand(serve);
        }
        for (int i = 0; i < count; i++) {
            group.addSubcommand(commands[i]);
        }
        return group;
    }

    /// The generated `serve` subcommand: no options of its own, and a body
    /// that starts the server.
    private static final class ServeBinding implements CommandBinding {
        private final Server server;
        private boolean ran;

        ServeBinding(Server server) {
            this.server = server;
        }

        @Override
        public CommandSpec spec() {
            return new CommandSpec("serve", new String[] {"Starts the server."},
                    new String[0], true, this);
        }

        @Override
        public Object create() {
            return this;
        }

        @Override
        public Object convert(int slot, String text) {
            return text;
        }

        @Override
        public void set(Object command, int slot, Object value) {
        }

        @Override
        public void inject(Object command, Object parent, CommandSpec spec) {
        }

        @Override
        public boolean runnable() {
            return true;
        }

        @Override
        public int call(Object command) throws Exception {
            ran = true;
            server.serve();
            return 0;
        }
    }

    /// What a parse produced: the command that runs, its instance, and whether
    /// help or version was asked for instead.
    private static final class Parse {
        CommandSpec command;
        Object instance;
        CommandSpec helpFor;
        CommandSpec versionFor;
    }

    private static void parse(CommandSpec cmd, Object parentInstance, String[] args, int start,
                              Parse result) throws Exception {
        CommandBinding binding = cmd.binding();
        Object instance = binding == null ? null : binding.create();
        if (instance != null) {
            binding.inject(instance, parentInstance, cmd);
        }
        result.command = cmd;
        result.instance = instance;
        List<OptionSpec> options = allOptions(cmd);
        Map<ArgSpec, List<Object>> values = new HashMap<ArgSpec, List<Object>>();
        List<String> positional = new ArrayList<String>();
        boolean endOfOptions = false;
        int i = start;
        while (i < args.length) {
            String arg = args[i];
            i++;
            if (!endOfOptions && "--".equals(arg)) {
                endOfOptions = true;
                continue;
            }
            if (!endOfOptions) {
                CommandSpec sub = cmd.subcommand(arg);
                if (sub != null) {
                    // The subcommand is what runs, so only its own required
                    // arguments are enforced; the parent's are bound if given.
                    finish(cmd, binding, instance, options, values, positional, result, false);
                    if (result.helpFor != null || result.versionFor != null) {
                        return;
                    }
                    parse(sub, instance, args, i, result);
                    return;
                }
            }
            if (!endOfOptions && arg.startsWith("-") && arg.length() > 1
                    && !(isNumber(arg) && find(options, arg) == null)) {
                i = option(cmd, binding, options, values, args, arg, i, result);
                continue;
            }
            positional.add(arg);
        }
        finish(cmd, binding, instance, options, values, positional, result, true);
    }

    private static List<OptionSpec> allOptions(CommandSpec cmd) {
        List<OptionSpec> all = new ArrayList<OptionSpec>(cmd.options());
        addStandardOptions(cmd, all);
        return all;
    }

    /// Adds the `--help` and `--version` options a command recognizes to
    /// `options`, its declared ones. The parser and the help both call this, so
    /// the help never lists an option the parser rejects. The build refuses a
    /// declared option that reuses one of their names; the checks here cover a
    /// spec built by hand. `--version` is left out of a generated group that has
    /// no version to print, rather than printing nothing.
    static void addStandardOptions(CommandSpec cmd, List<OptionSpec> options) {
        if (!cmd.standardHelpOptions()) {
            return;
        }
        boolean help = find(options, "-h") == null && find(options, "--help") == null;
        boolean version = find(options, "-V") == null && find(options, "--version") == null
                && (cmd.binding() != null || cmd.version().length > 0);
        if (help) {
            options.add(OptionSpec.helpOption());
        }
        if (version) {
            options.add(OptionSpec.versionOption());
        }
    }

    private static OptionSpec find(List<OptionSpec> options, String name) {
        for (OptionSpec o : options) {
            String[] names = o.names();
            for (String n : names) {
                if (n.equals(name)) {
                    return o;
                }
            }
        }
        return null;
    }

    private static OptionSpec findNegated(List<OptionSpec> options, String name) {
        if (!name.startsWith("--no-")) {
            return null;
        }
        OptionSpec o = find(options, "--" + name.substring(5));
        return o != null && o.negatable() && o.flag() ? o : null;
    }

    private static boolean isNumber(String arg) {
        if (arg.length() < 2) {
            return false;
        }
        boolean digit = false;
        for (int i = 1; i < arg.length(); i++) {
            char c = arg.charAt(i);
            if (c >= '0' && c <= '9') {
                digit = true;
            } else if (c != '.' && c != 'e' && c != 'E') {
                return false;
            }
        }
        return digit;
    }

    /// Consumes one option argument, which may be `--name=value`, `--name value`,
    /// `-n value`, `-nvalue` or a cluster of short flags such as `-xvf file`.
    /// Answers the index of the next unread argument.
    private static int option(CommandSpec cmd, CommandBinding binding, List<OptionSpec> options,
                              Map<ArgSpec, List<Object>> values, String[] args, String arg,
                              int next, Parse result) throws Exception {
        String name = arg;
        String attached = null;
        int eq = arg.indexOf('=');
        if (eq > 0) {
            name = arg.substring(0, eq);
            attached = arg.substring(eq + 1);
        }
        OptionSpec o = find(options, name);
        if (o == null && attached == null) {
            OptionSpec negated = findNegated(options, name);
            if (negated != null) {
                record(cmd, binding, negated, values, Boolean.FALSE);
                return next;
            }
        }
        if (o != null) {
            return occurrence(cmd, binding, o, options, values, args, attached, next, result);
        }
        if (!arg.startsWith("--") && arg.length() > 2) {
            // A cluster: every character a short option, until one takes a value
            // and the rest of the cluster is that value.
            int c = 1;
            OptionSpec s = null;
            while (c < arg.length()) {
                s = find(options, "-" + arg.charAt(c));
                if (s == null) {
                    throw new ParameterException(cmd, "Unknown option: '-" + arg.charAt(c)
                            + "' (while processing option: '" + arg + "')");
                }
                if (s.arityMax() != 0) {
                    break;
                }
                occurrence(cmd, binding, s, options, values, args, null, next, result);
                c++;
            }
            if (c >= arg.length()) {
                return next;
            }
            // The rest of the cluster is the value of the first option that takes one.
            // `-c=2` and `-lc=2` give the value 2, as `--count=2` does.
            // `-c=` is an empty value, as `--count=` is; a bare `-c` takes the next.
            String rest = arg.substring(c + 1);
            boolean equals = rest.startsWith("=");
            if (equals) {
                rest = rest.substring(1);
            }
            return occurrence(cmd, binding, s, options, values, args,
                    rest.length() == 0 && !equals ? null : rest, next, result);
        }
        throw new ParameterException(cmd, "Unknown option: '" + name + "'");
    }

    private static int occurrence(CommandSpec cmd, CommandBinding binding, OptionSpec o,
                                  List<OptionSpec> options, Map<ArgSpec, List<Object>> values,
                                  String[] args, String attached, int next, Parse result)
            throws Exception {
        if (o.usageHelp()) {
            if (result.helpFor == null) {
                result.helpFor = cmd;
            }
            return next;
        }
        if (o.versionHelp()) {
            if (result.versionFor == null) {
                result.versionFor = cmd;
            }
            return next;
        }
        if (o.flag()) {
            if (attached != null) {
                record(cmd, binding, o, values, parseBoolean(cmd, o, attached));
            } else if (o.arityMin() > 0) {
                // arity = "1": the value is required, and must be true or false.
                if (next >= args.length) {
                    throw new ParameterException(cmd, "Missing required parameter for option '"
                            + o.longestName() + "' (" + o.label() + ")");
                }
                record(cmd, binding, o, values, parseBoolean(cmd, o, args[next]));
                return next + 1;
            } else if (o.arityMax() > 0 && next < args.length
                    && ("true".equalsIgnoreCase(args[next])
                        || "false".equalsIgnoreCase(args[next]))) {
                record(cmd, binding, o, values, parseBoolean(cmd, o, args[next]));
                return next + 1;
            } else {
                record(cmd, binding, o, values, Boolean.TRUE);
            }
            return next;
        }
        int taken = 0;
        if (attached != null) {
            convertAndRecord(cmd, binding, o, values, attached);
            taken = 1;
        }
        while (taken < o.arityMax() && next < args.length) {
            String candidate = args[next];
            boolean looksLikeOption = candidate.startsWith("-") && candidate.length() > 1
                    && !isNumber(candidate);
            if (taken >= o.arityMin() && (looksLikeOption
                    || find(options, candidate) != null || cmd.subcommand(candidate) != null)) {
                break;
            }
            if (taken < o.arityMin() && looksLikeOption && find(options, candidate) != null) {
                break;
            }
            convertAndRecord(cmd, binding, o, values, candidate);
            taken++;
            next++;
        }
        if (taken < o.arityMin()) {
            throw new ParameterException(cmd, "Missing required parameter for option '"
                    + o.longestName() + "' (" + o.label() + ")");
        }
        return next;
    }

    private static Boolean parseBoolean(CommandSpec cmd, OptionSpec o, String text) {
        if ("true".equalsIgnoreCase(text)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(text)) {
            return Boolean.FALSE;
        }
        throw new ParameterException(cmd, "Invalid value for option '" + o.longestName()
                + "': '" + text + "' is not a boolean");
    }

    private static void convertAndRecord(CommandSpec cmd, CommandBinding binding, ArgSpec a,
                                         Map<ArgSpec, List<Object>> values, String text)
            throws Exception {
        String[] parts = a.split() == null ? new String[] {text} : split(text, a.split());
        for (String part : parts) {
            record(cmd, binding, a, values, convert(cmd, binding, a, part));
        }
    }

    private static Object convert(CommandSpec cmd, CommandBinding binding, ArgSpec a,
                                  String text) {
        try {
            return binding.convert(a.slot(), text);
        } catch (Exception e) {
            String what = a instanceof OptionSpec
                    ? "option '" + ((OptionSpec) a).longestName() + "'"
                    : "positional parameter " + a.label();
            String reason = e.getMessage();
            throw new ParameterException(cmd, "Invalid value for " + what + ": '" + text + "'"
                    + (reason == null || reason.length() == 0 ? "" : " (" + reason + ")"), e);
        }
    }

    private static void record(CommandSpec cmd, CommandBinding binding, ArgSpec a,
                               Map<ArgSpec, List<Object>> values, Object value) {
        if (binding == null) {
            return;
        }
        List<Object> list = values.get(a);
        if (list == null) {
            list = new ArrayList<Object>();
            values.put(a, list);
        } else if (!a.multiple() && a instanceof OptionSpec && !a.flag()) {
            throw new ParameterException(cmd, "option '" + ((OptionSpec) a).longestName()
                    + "' (" + a.label() + ") should be specified only once");
        }
        if (!a.multiple()) {
            list.clear();
        }
        list.add(value);
    }

    private static String[] split(String text, String separator) {
        List<String> parts = new ArrayList<String>();
        int from = 0;
        while (true) {
            int at = text.indexOf(separator, from);
            if (at < 0) {
                parts.add(text.substring(from));
                break;
            }
            parts.add(text.substring(from, at));
            from = at + separator.length();
        }
        return parts.toArray(new String[parts.size()]);
    }

    private static void finish(CommandSpec cmd, CommandBinding binding, Object instance,
                               List<OptionSpec> options, Map<ArgSpec, List<Object>> values,
                               List<String> positional, Parse result, boolean validate)
            throws Exception {
        if (binding == null) {
            if (!positional.isEmpty()) {
                throw new ParameterException(cmd, "Unmatched argument at index 0: '"
                        + positional.get(0) + "'");
            }
            return;
        }
        List<PositionalParamSpec> params = cmd.positionals();
        boolean[] used = new boolean[positional.size()];
        for (PositionalParamSpec spec : params) {
            int to = Math.min(spec.indexTo(), positional.size() - 1);
            int limit = spec.arityMax() == Integer.MAX_VALUE ? Integer.MAX_VALUE
                    : spec.indexFrom() + (spec.multiple() ? spec.arityMax() : 1) - 1;
            to = Math.min(to, limit);
            for (int i = spec.indexFrom(); i <= to; i++) {
                convertAndRecord(cmd, binding, spec, values, positional.get(i));
                used[i] = true;
            }
        }
        for (int i = 0; i < used.length; i++) {
            if (!used[i]) {
                throw new ParameterException(cmd, "Unmatched argument at index " + i + ": '"
                        + positional.get(i) + "'");
            }
        }
        boolean help = !validate || result.helpFor != null || result.versionFor != null;
        for (OptionSpec o : options) {
            if (o.slot() < 0) {
                continue;
            }
            if (!values.containsKey(o)) {
                if (o.required() && !help) {
                    throw new ParameterException(cmd, "Missing required option: '"
                            + o.longestName() + (o.flag() ? "" : "=" + o.label()) + "'");
                }
                applyDefault(cmd, binding, o, values);
            }
        }
        for (PositionalParamSpec spec : params) {
            List<Object> got = values.get(spec);
            int count = got == null ? 0 : got.size();
            boolean needed = spec.required() || (spec.arityMin() > 0 && spec.defaultValue() == null
                    && !spec.multiple());
            if (spec.multiple() && spec.arityMin() > count && !help) {
                throw new ParameterException(cmd, "Missing required parameter: '"
                        + spec.label() + "'");
            }
            if (count == 0) {
                if (needed && !help) {
                    throw new ParameterException(cmd, "Missing required parameter: '"
                            + spec.label() + "'");
                }
                applyDefault(cmd, binding, spec, values);
            }
        }
        for (Map.Entry<ArgSpec, List<Object>> e : values.entrySet()) {
            ArgSpec a = e.getKey();
            List<Object> list = e.getValue();
            binding.set(instance, a.slot(), a.multiple() ? list : list.get(list.size() - 1));
        }
    }

    private static void applyDefault(CommandSpec cmd, CommandBinding binding, ArgSpec a,
                                     Map<ArgSpec, List<Object>> values) throws Exception {
        String text = resolveDefault(a.defaultValue());
        if (text == null) {
            return;
        }
        if (a.flag()) {
            record(cmd, binding, a, values, Boolean.valueOf("true".equalsIgnoreCase(text)));
            return;
        }
        convertAndRecord(cmd, binding, a, values, text);
    }

    /// Resolves `${env:NAME}` and `${env:NAME:-fallback}` in a default value.
    static String resolveDefault(String value) {
        if (value == null) {
            return null;
        }
        int at = value.indexOf("${env:");
        if (at < 0) {
            return value;
        }
        int end = value.indexOf('}', at);
        if (end < 0) {
            return value;
        }
        String body = value.substring(at + 6, end);
        String fallback = null;
        int dash = body.indexOf(":-");
        if (dash >= 0) {
            fallback = body.substring(dash + 2);
            body = body.substring(0, dash);
        }
        String env = System.getenv(body);
        if (env == null) {
            env = fallback;
        }
        if (env == null) {
            return at == 0 && end == value.length() - 1 ? null
                    : value.substring(0, at) + value.substring(end + 1);
        }
        return value.substring(0, at) + env + value.substring(end + 1);
    }

    /// Replaces every `@file` argument with the arguments in that file, split on
    /// white space, with single or double quotes around an argument that holds
    /// some. `@@x` is the literal argument `@x`; nothing after `--` is expanded.
    static String[] expandArgFiles(String[] args) {
        List<String> out = new ArrayList<String>();
        boolean end = false;
        for (String a : args) {
            if (!end && "--".equals(a)) {
                end = true;
            }
            if (end || !a.startsWith("@") || a.length() < 2) {
                out.add(a);
            } else if (a.startsWith("@@")) {
                out.add(a.substring(1));
            } else {
                tokenize(read(a.substring(1)), out);
            }
        }
        return out.toArray(new String[out.size()]);
    }

    private static String read(String path) {
        File f = new File(path);
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                bytes.write(buf, 0, n);
            }
            return new String(bytes.toByteArray(), "UTF-8");
        } catch (IOException e) {
            throw new ParameterException(null, "Could not read argument file " + path + ": "
                    + e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // Nothing was written; a failed close of a read loses nothing.
                }
            }
        }
    }

    private static void tokenize(String text, List<String> out) {
        StringBuilder current = null;
        char quote = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                } else {
                    current.append(c);
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
                if (current == null) {
                    current = new StringBuilder();
                }
            } else if (c == '#' && current == null) {
                while (i < text.length() && text.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                if (current != null) {
                    out.add(current.toString());
                    current = null;
                }
            } else {
                if (current == null) {
                    current = new StringBuilder();
                }
                current.append(c);
            }
        }
        if (current != null) {
            out.add(current.toString());
        }
    }
}
