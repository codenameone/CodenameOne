# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Command-line parsing: the argparse API.

Parses, reports errors and formats help the way CPython's argparse does, so a
program written for it runs unchanged, and its usage, help and error text read
the same. Errors exit with status 2, --help and --version with 0, through
SystemExit, which a packaged program turns into its exit status.

Not provided: parse_intermixed_args, and coloured help.
"""

import os
import re
import sys
import textwrap

__all__ = [
    "ArgumentParser", "ArgumentError", "ArgumentTypeError", "BooleanOptionalAction",
    "FileType", "HelpFormatter", "ArgumentDefaultsHelpFormatter", "RawDescriptionHelpFormatter",
    "RawTextHelpFormatter", "MetavarTypeHelpFormatter", "Namespace", "Action", "ONE_OR_MORE",
    "OPTIONAL", "PARSER", "REMAINDER", "SUPPRESS", "ZERO_OR_MORE",
]

SUPPRESS = "==SUPPRESS=="
OPTIONAL = "?"
ZERO_OR_MORE = "*"
ONE_OR_MORE = "+"
PARSER = "A..."
REMAINDER = "..."
_UNRECOGNIZED_ARGS_ATTR = "_unrecognized_args"


def _terminal_width():
    try:
        columns = int(os.environ.get("COLUMNS", ""))
        if columns > 0:
            return columns
    except (ValueError, TypeError):
        pass
    return 80


class Namespace:
    """Holds the parsed values as attributes."""

    def __init__(self, **kwargs):
        for name in kwargs:
            setattr(self, name, kwargs[name])

    def __repr__(self):
        parts = []
        star = {}
        for name, value in self.__dict__.items():
            if name.isidentifier():
                parts.append("%s=%r" % (name, value))
            else:
                star[name] = value
        if star:
            parts.append("**%s" % repr(star))
        return "%s(%s)" % (type(self).__name__, ", ".join(parts))

    def __eq__(self, other):
        if not isinstance(other, Namespace):
            return NotImplemented
        return self.__dict__ == other.__dict__

    def __contains__(self, key):
        return key in self.__dict__


def _action_name(argument):
    if argument is None:
        return None
    if argument.option_strings:
        return "/".join(argument.option_strings)
    if argument.metavar not in (None, SUPPRESS):
        metavar = argument.metavar
        if isinstance(metavar, tuple):
            return "/".join(metavar)
        return metavar
    if argument.dest not in (None, SUPPRESS):
        return argument.dest
    if argument.choices:
        return "{" + ",".join([str(c) for c in argument.choices]) + "}"
    return None


class ArgumentError(Exception):
    """An argument that cannot be parsed, naming the argument."""

    def __init__(self, argument, message):
        Exception.__init__(self, message)
        self.argument_name = _action_name(argument)
        self.message = message

    def __str__(self):
        if self.argument_name is None:
            return self.message
        return "argument %s: %s" % (self.argument_name, self.message)


class ArgumentTypeError(Exception):
    """Raised by a type function to report an invalid value in its own words."""
    pass


# ---------------------------------------------------------------- formatting

class HelpFormatter:
    """Formats usage and help text."""

    def __init__(self, prog, indent_increment=2, max_help_position=24, width=None):
        if width is None:
            width = _terminal_width() - 2
        self._prog = prog
        self._indent_increment = indent_increment
        self._max_help_position = min(max_help_position, max(width - 20, indent_increment * 2))
        self._width = width
        self._current_indent = 0
        self._action_max_length = 0
        self._root = _Section(None, None)
        self._section = self._root

    def _indent(self):
        self._current_indent += self._indent_increment

    def _dedent(self):
        self._current_indent -= self._indent_increment

    def start_section(self, heading):
        self._indent()
        section = _Section(self._section, heading)
        self._section.items.append((section.format, (self,)))
        self._section = section

    def end_section(self):
        self._section = self._section.parent
        self._dedent()

    def add_text(self, text):
        if text is not SUPPRESS and text is not None:
            self._section.items.append((self._format_text, (text,)))

    def add_usage(self, usage, actions, groups, prefix=None):
        if usage is not SUPPRESS:
            self._section.items.append((self._format_usage, (usage, actions, groups, prefix)))

    def add_argument(self, action):
        if action.help is SUPPRESS:
            return
        invocations = [self._format_action_invocation(action)]
        self._indent()
        for sub in action._get_subactions():
            invocations.append(" " * self._current_indent + self._format_action_invocation(sub))
        self._dedent()
        longest = 0
        for s in invocations:
            if len(s) > longest:
                longest = len(s)
        self._action_max_length = max(self._action_max_length, longest + self._current_indent)
        self._section.items.append((self._format_action, (action,)))

    def add_arguments(self, actions):
        for action in actions:
            self.add_argument(action)

    def format_help(self):
        text = self._root.format(self)
        if text:
            text = re.sub(r"\n\n\n+", "\n\n", text)
            text = text.strip("\n") + "\n"
        return text

    # -- usage

    def _format_usage(self, usage, actions, groups, prefix):
        if prefix is None:
            prefix = "usage: "
        if usage is not None:
            usage = usage % {"prog": self._prog}
        elif not actions:
            usage = self._prog
        else:
            prog = self._prog
            optionals = [a for a in actions if a.option_strings]
            positionals = [a for a in actions if not a.option_strings]
            opt_parts = self._usage_parts(optionals, groups)
            pos_parts = self._usage_parts(positionals, groups)
            usage = " ".join([s for s in [prog] + opt_parts + pos_parts if s])
            text_width = self._width - self._current_indent
            if len(prefix) + len(usage) > text_width:
                if len(prefix) + len(prog) <= 0.75 * text_width:
                    indent = " " * (len(prefix) + len(prog) + 1)
                    if opt_parts:
                        lines = _usage_lines([prog] + opt_parts, indent, text_width, prefix)
                        lines.extend(_usage_lines(pos_parts, indent, text_width))
                    elif pos_parts:
                        lines = _usage_lines([prog] + pos_parts, indent, text_width, prefix)
                    else:
                        lines = [prog]
                else:
                    indent = " " * len(prefix)
                    lines = _usage_lines(opt_parts + pos_parts, indent, text_width)
                    if len(lines) > 1:
                        lines = _usage_lines(opt_parts, indent, text_width)
                        lines.extend(_usage_lines(pos_parts, indent, text_width))
                    lines = [prog] + lines
                usage = "\n".join(lines)
        return "%s%s\n\n" % (prefix, usage)

    def _usage_parts(self, actions, groups):
        # A mutually exclusive group whose actions are all here, side by
        # side, is shown as one part: [-a | -b], or (-a | -b) when required.
        grouped = {}
        for group in groups:
            members = group._group_actions
            if not members:
                continue
            try:
                start = actions.index(members[0])
            except ValueError:
                continue
            if actions[start:start + len(members)] == members:
                grouped[start] = group
        parts = []
        i = 0
        while i < len(actions):
            if i in grouped:
                group = grouped[i]
                inner = []
                for action in group._group_actions:
                    if action.help is not SUPPRESS:
                        inner.append(self._usage_part(action, True))
                if inner:
                    text = " | ".join(inner)
                    if group.required:
                        if len(inner) > 1:
                            text = "(" + text + ")"
                    else:
                        text = "[" + text + "]"
                    parts.append(text)
                i += len(group._group_actions)
                continue
            action = actions[i]
            i += 1
            if action.help is SUPPRESS:
                continue
            part = self._usage_part(action, False)
            if part:
                parts.append(part)
        return parts

    def _usage_part(self, action, in_group):
        if not action.option_strings:
            part = self._format_args(action, self._default_metavar_for_positional(action))
            if in_group and part.startswith("[") and part.endswith("]"):
                part = part[1:-1]
            return part
        if action.nargs == 0:
            part = action.format_usage()
        else:
            part = "%s %s" % (action.option_strings[0],
                              self._format_args(action, self._default_metavar_for_optional(action)))
        if not action.required and not in_group:
            part = "[%s]" % part
        return part

    # -- text and actions

    def _format_text(self, text):
        if "%(prog)" in text:
            text = text % {"prog": self._prog}
        text_width = max(self._width - self._current_indent, 11)
        indent = " " * self._current_indent
        return self._fill_text(text, text_width, indent) + "\n\n"

    def _format_action(self, action):
        help_position = min(self._action_max_length + 2, self._max_help_position)
        help_width = max(self._width - help_position, 11)
        action_width = help_position - self._current_indent - 2
        header = self._format_action_invocation(action)
        pad = " " * self._current_indent
        indent_first = 0
        if not action.help:
            header = pad + header + "\n"
        elif len(header) <= action_width:
            header = pad + header.ljust(action_width) + "  "
        else:
            header = pad + header + "\n"
            indent_first = help_position
        parts = [header]
        if action.help and action.help.strip():
            text = self._expand_help(action)
            if text:
                lines = self._split_lines(text, help_width)
                if lines:
                    parts.append(" " * indent_first + lines[0] + "\n")
                    for line in lines[1:]:
                        parts.append(" " * help_position + line + "\n")
                elif not header.endswith("\n"):
                    parts.append("\n")
        elif not header.endswith("\n"):
            parts.append("\n")
        self._indent()
        for sub in action._get_subactions():
            parts.append(self._format_action(sub))
        self._dedent()
        return "".join(parts)

    def _format_action_invocation(self, action):
        if not action.option_strings:
            return self._metavar(action, self._default_metavar_for_positional(action), 1)[0]
        if action.nargs == 0:
            return ", ".join(action.option_strings)
        args = self._format_args(action, self._default_metavar_for_optional(action))
        return ", ".join(action.option_strings) + " " + args

    def _metavar(self, action, default, size):
        if action.metavar is not None:
            result = action.metavar
        elif action.choices is not None:
            result = "{%s}" % ",".join([str(c) for c in action.choices])
        else:
            result = default
        if isinstance(result, tuple):
            return result
        return (result,) * size

    def _format_args(self, action, default):
        nargs = action.nargs
        if nargs is None:
            return "%s" % self._metavar(action, default, 1)
        if nargs == OPTIONAL:
            return "[%s]" % self._metavar(action, default, 1)
        if nargs == ZERO_OR_MORE:
            m = self._metavar(action, default, 1)
            if len(m) == 2:
                return "[%s [%s ...]]" % m
            return "[%s ...]" % m[0]
        if nargs == ONE_OR_MORE:
            m = self._metavar(action, default, 2)
            return "%s [%s ...]" % (m[0], m[1])
        if nargs == REMAINDER:
            return "..."
        if nargs == PARSER:
            return "%s ..." % self._metavar(action, default, 1)
        if nargs == SUPPRESS:
            return ""
        return " ".join(self._metavar(action, default, nargs))

    def _expand_help(self, action):
        params = {
            "option_strings": action.option_strings, "dest": action.dest, "nargs": action.nargs,
            "const": action.const, "default": action.default, "type": action.type,
            "choices": action.choices, "required": action.required, "help": action.help,
            "metavar": action.metavar, "prog": self._prog,
        }
        for name in list(params):
            value = params[name]
            if value is SUPPRESS:
                del params[name]
            elif hasattr(value, "__name__"):
                params[name] = value.__name__
        if params.get("choices") is not None:
            params["choices"] = ", ".join([str(c) for c in params["choices"]])
        return self._get_help_string(action) % params

    def _get_help_string(self, action):
        return action.help

    def _split_lines(self, text, width):
        text = re.sub(r"\s+", " ", text).strip()
        return textwrap.wrap(text, width)

    def _fill_text(self, text, width, indent):
        text = re.sub(r"\s+", " ", text).strip()
        return textwrap.fill(text, width, initial_indent=indent, subsequent_indent=indent)

    def _default_metavar_for_optional(self, action):
        return action.dest.upper()

    def _default_metavar_for_positional(self, action):
        return action.dest


def _usage_lines(parts, indent, text_width, prefix=None):
    lines = []
    line = []
    indent_length = len(indent)
    if prefix is not None:
        line_len = len(prefix) - 1
    else:
        line_len = indent_length - 1
    for part in parts:
        if line_len + 1 + len(part) > text_width and line:
            lines.append(indent + " ".join(line))
            line = []
            line_len = indent_length - 1
        line.append(part)
        line_len += len(part) + 1
    if line:
        lines.append(indent + " ".join(line))
    if prefix is not None:
        lines[0] = lines[0][len(indent):]
    return lines


class _Section:
    def __init__(self, parent, heading):
        self.parent = parent
        self.heading = heading
        self.items = []

    def format(self, formatter):
        if self.parent is not None:
            formatter._indent()
        text = "".join([func(*args) for func, args in self.items])
        if self.parent is not None:
            formatter._dedent()
        if not text:
            return ""
        heading = ""
        if self.heading is not SUPPRESS and self.heading is not None:
            heading = " " * formatter._current_indent + self.heading + ":\n"
        return "\n" + heading + text + "\n"


class RawDescriptionHelpFormatter(HelpFormatter):
    """Keeps the description and epilog as written."""

    def _fill_text(self, text, width, indent):
        return "".join([indent + line for line in text.splitlines(True)])


class RawTextHelpFormatter(RawDescriptionHelpFormatter):
    """Keeps every help text as written."""

    def _split_lines(self, text, width):
        return text.splitlines()


class ArgumentDefaultsHelpFormatter(HelpFormatter):
    """Adds each argument's default to its help."""

    def _get_help_string(self, action):
        text = action.help
        if text is None:
            text = ""
        if "%(default)" not in text and action.default is not SUPPRESS:
            if action.option_strings or action.nargs in [OPTIONAL, ZERO_OR_MORE]:
                text += " (default: %(default)s)"
        return text


class MetavarTypeHelpFormatter(HelpFormatter):
    """Names each value by its type rather than its destination."""

    def _default_metavar_for_optional(self, action):
        return action.type.__name__

    def _default_metavar_for_positional(self, action):
        return action.type.__name__


# ---------------------------------------------------------------- actions

class Action:
    """What an argument does when it is seen. Subclass it and implement
    __call__(self, parser, namespace, values, option_string=None)."""

    def __init__(self, option_strings, dest, nargs=None, const=None, default=None, type=None,
                 choices=None, required=False, help=None, metavar=None, deprecated=False):
        self.option_strings = option_strings
        self.dest = dest
        self.nargs = nargs
        self.const = const
        self.default = default
        self.type = type
        self.choices = choices
        self.required = required
        self.help = help
        self.metavar = metavar
        self.deprecated = deprecated

    def _get_subactions(self):
        return []

    def format_usage(self):
        return self.option_strings[0]

    def __call__(self, parser, namespace, values, option_string=None):
        raise NotImplementedError("Action subclasses must implement __call__")


class BooleanOptionalAction(Action):
    """A flag with a --no- form: --color and --no-color."""

    def __init__(self, option_strings, dest, default=None, required=False, help=None,
                 deprecated=False):
        strings = []
        for option_string in option_strings:
            strings.append(option_string)
            if option_string.startswith("--"):
                strings.append("--no-" + option_string[2:])
        Action.__init__(self, strings, dest, nargs=0, default=default, required=required,
                        help=help, deprecated=deprecated)

    def __call__(self, parser, namespace, values, option_string=None):
        if option_string in self.option_strings:
            setattr(namespace, self.dest, not option_string.startswith("--no-"))

    def format_usage(self):
        return " | ".join(self.option_strings)


class _StoreAction(Action):
    def __init__(self, option_strings, dest, nargs=None, const=None, default=None, type=None,
                 choices=None, required=False, help=None, metavar=None, deprecated=False):
        if nargs == 0:
            raise ValueError("nargs for store actions must be != 0; if you have nothing to store, "
                             "actions such as store true or store const may be more appropriate")
        if const is not None and nargs != OPTIONAL:
            raise ValueError("nargs must be %r to supply const" % OPTIONAL)
        Action.__init__(self, option_strings, dest, nargs, const, default, type, choices,
                        required, help, metavar, deprecated)

    def __call__(self, parser, namespace, values, option_string=None):
        setattr(namespace, self.dest, values)


class _StoreConstAction(Action):
    def __init__(self, option_strings, dest, const=None, default=None, required=False, help=None,
                 metavar=None, deprecated=False):
        Action.__init__(self, option_strings, dest, 0, const, default, None, None, required,
                        help, metavar, deprecated)

    def __call__(self, parser, namespace, values, option_string=None):
        setattr(namespace, self.dest, self.const)


class _StoreTrueAction(_StoreConstAction):
    def __init__(self, option_strings, dest, default=False, required=False, help=None,
                 deprecated=False):
        _StoreConstAction.__init__(self, option_strings, dest, True, default, required, help,
                                   None, deprecated)


class _StoreFalseAction(_StoreConstAction):
    def __init__(self, option_strings, dest, default=True, required=False, help=None,
                 deprecated=False):
        _StoreConstAction.__init__(self, option_strings, dest, False, default, required, help,
                                   None, deprecated)


def _copy_items(items):
    if items is None:
        return []
    return list(items)


class _AppendAction(Action):
    def __init__(self, option_strings, dest, nargs=None, const=None, default=None, type=None,
                 choices=None, required=False, help=None, metavar=None, deprecated=False):
        if nargs == 0:
            raise ValueError("nargs for append actions must be != 0; if arg strings are not "
                             "supplying the value to append, the append const action may be "
                             "more appropriate")
        if const is not None and nargs != OPTIONAL:
            raise ValueError("nargs must be %r to supply const" % OPTIONAL)
        Action.__init__(self, option_strings, dest, nargs, const, default, type, choices,
                        required, help, metavar, deprecated)

    def __call__(self, parser, namespace, values, option_string=None):
        items = _copy_items(getattr(namespace, self.dest, None))
        items.append(values)
        setattr(namespace, self.dest, items)


class _AppendConstAction(Action):
    def __init__(self, option_strings, dest, const=None, default=None, required=False, help=None,
                 metavar=None, deprecated=False):
        Action.__init__(self, option_strings, dest, 0, const, default, None, None, required,
                        help, metavar, deprecated)

    def __call__(self, parser, namespace, values, option_string=None):
        items = _copy_items(getattr(namespace, self.dest, None))
        items.append(self.const)
        setattr(namespace, self.dest, items)


class _CountAction(Action):
    def __init__(self, option_strings, dest, default=None, required=False, help=None,
                 deprecated=False):
        Action.__init__(self, option_strings, dest, 0, None, default, None, None, required,
                        help, None, deprecated)

    def __call__(self, parser, namespace, values, option_string=None):
        count = getattr(namespace, self.dest, None)
        if count is None:
            count = 0
        setattr(namespace, self.dest, count + 1)


class _ExtendAction(_AppendAction):
    def __call__(self, parser, namespace, values, option_string=None):
        items = _copy_items(getattr(namespace, self.dest, None))
        items.extend(values)
        setattr(namespace, self.dest, items)


class _HelpAction(Action):
    def __init__(self, option_strings, dest=SUPPRESS, default=SUPPRESS, help=None,
                 deprecated=False):
        Action.__init__(self, option_strings, dest, 0, None, default, None, None, False, help,
                        None, deprecated)

    def __call__(self, parser, namespace, values, option_string=None):
        parser.print_help()
        parser.exit()


class _VersionAction(Action):
    def __init__(self, option_strings, version=None, dest=SUPPRESS, default=SUPPRESS,
                 help="show program's version number and exit", deprecated=False):
        Action.__init__(self, option_strings, dest, 0, None, default, None, None, False, help,
                        None, deprecated)
        self.version = version

    def __call__(self, parser, namespace, values, option_string=None):
        version = self.version
        if version is None:
            version = parser.version
        formatter = parser._get_formatter()
        formatter.add_text(version)
        parser._print_message(formatter.format_help(), sys.stdout)
        parser.exit()


class _ChoicesPseudoAction(Action):
    def __init__(self, name, aliases, help):
        metavar = name
        if aliases:
            metavar += " (%s)" % ", ".join(aliases)
        Action.__init__(self, [], name, help=help, metavar=metavar)


class _SubParsersAction(Action):
    def __init__(self, option_strings, prog, parser_class, dest=SUPPRESS, required=False,
                 help=None, metavar=None):
        self._prog_prefix = prog
        self._parser_class = parser_class
        self._name_parser_map = {}
        self._choices_actions = []
        self._deprecated = set()
        Action.__init__(self, option_strings, dest, PARSER, choices=self._name_parser_map,
                        required=required, help=help, metavar=metavar)

    def add_parser(self, name, **kwargs):
        if kwargs.get("prog") is None:
            kwargs["prog"] = "%s %s" % (self._prog_prefix, name)
        aliases = kwargs.pop("aliases", ())
        if name in self._name_parser_map:
            raise ArgumentError(self, "conflicting subparser: %s" % name)
        for alias in aliases:
            if alias in self._name_parser_map:
                raise ArgumentError(self, "conflicting subparser alias: %s" % alias)
        deprecated = kwargs.pop("deprecated", False)
        if "help" in kwargs:
            self._choices_actions.append(_ChoicesPseudoAction(name, aliases, kwargs.pop("help")))
        parser = self._parser_class(**kwargs)
        self._name_parser_map[name] = parser
        for alias in aliases:
            self._name_parser_map[alias] = parser
        if deprecated:
            self._deprecated.add(name)
            for alias in aliases:
                self._deprecated.add(alias)
        return parser

    def _get_subactions(self):
        return self._choices_actions

    def __call__(self, parser, namespace, values, option_string=None):
        parser_name = values[0]
        arg_strings = values[1:]
        if self.dest is not SUPPRESS:
            setattr(namespace, self.dest, parser_name)
        if parser_name not in self._name_parser_map:
            choices = ", ".join(self._name_parser_map)
            raise ArgumentError(self, "unknown parser %r (choices: %s)" % (parser_name, choices))
        sub = self._name_parser_map[parser_name]
        if parser_name in self._deprecated:
            parser._warning("command '%s' is deprecated" % parser_name)
        subnamespace, arg_strings = sub.parse_known_args(arg_strings, None)
        for key, value in subnamespace.__dict__.items():
            setattr(namespace, key, value)
        if arg_strings:
            if _UNRECOGNIZED_ARGS_ATTR not in namespace.__dict__:
                setattr(namespace, _UNRECOGNIZED_ARGS_ATTR, [])
            getattr(namespace, _UNRECOGNIZED_ARGS_ATTR).extend(arg_strings)


class FileType:
    """A type that opens the named file; "-" is standard input or output."""

    def __init__(self, mode="r", bufsize=-1, encoding=None, errors=None):
        self._mode = mode
        self._encoding = encoding
        self._errors = errors

    def __call__(self, string):
        if string == "-":
            if "r" in self._mode:
                return sys.stdin
            if "w" in self._mode or "a" in self._mode or "x" in self._mode:
                return sys.stdout
            raise ValueError("argument \"-\" with mode %r" % self._mode)
        try:
            if "b" in self._mode:
                return open(string, self._mode)
            return open(string, self._mode, encoding=self._encoding, errors=self._errors)
        except OSError as e:
            raise ArgumentTypeError("can't open '%s': %s" % (string, e))


_ACTIONS = {
    None: _StoreAction, "store": _StoreAction, "store_const": _StoreConstAction,
    "store_true": _StoreTrueAction, "store_false": _StoreFalseAction, "append": _AppendAction,
    "append_const": _AppendConstAction, "count": _CountAction, "help": _HelpAction,
    "version": _VersionAction, "parsers": _SubParsersAction, "extend": _ExtendAction,
}

_NEGATIVE_NUMBER = re.compile(r"^-\d+$|^-\d*\.\d+$")


# ---------------------------------------------------------------- containers

class _ActionsContainer:
    def add_argument(self, *args, **kwargs):
        """add_argument(dest, ..., name=value, ...) or
        add_argument(option_string, option_string, ..., name=value, ...)"""
        chars = self._parser_ref().prefix_chars
        if not args or len(args) == 1 and args[0] and args[0][0] not in chars:
            if args and "dest" in kwargs:
                raise TypeError("dest supplied twice for positional argument, did you mean metavar?")
            kwargs = self._positional_kwargs(*args, **kwargs)
        else:
            kwargs = self._optional_kwargs(*args, **kwargs)
        parser = self._parser_ref()
        if "default" not in kwargs:
            dest = kwargs["dest"]
            if dest in parser._defaults:
                kwargs["default"] = parser._defaults[dest]
            elif parser.argument_default is not None:
                kwargs["default"] = parser.argument_default
        action_class = kwargs.pop("action", None)
        if isinstance(action_class, str) or action_class is None:
            if action_class not in _ACTIONS:
                raise ValueError("unknown action %r" % (action_class,))
            action_class = _ACTIONS[action_class]
        if not callable(action_class):
            raise ValueError("unknown action %r" % (action_class,))
        action = action_class(**kwargs)
        type_func = action.type
        if type_func is not None and not callable(type_func):
            raise TypeError("%r is not callable" % (type_func,))
        if action.nargs == 0 and not action.option_strings and action_class is _StoreAction:
            raise ValueError("nargs for positionals must be != 0")
        return self._add_action(action)

    def _positional_kwargs(self, dest, **kwargs):
        if "required" in kwargs:
            raise TypeError("'required' is an invalid argument for positionals")
        nargs = kwargs.get("nargs")
        if nargs == 0:
            raise ValueError("nargs for positionals must be != 0")
        if nargs not in [OPTIONAL, ZERO_OR_MORE, REMAINDER, SUPPRESS]:
            kwargs["required"] = True
        kwargs["dest"] = dest
        kwargs["option_strings"] = []
        return kwargs

    def _optional_kwargs(self, *args, **kwargs):
        chars = self._parser_ref().prefix_chars
        option_strings = []
        long_strings = []
        for option_string in args:
            if not option_string or option_string[0] not in chars:
                raise ValueError("invalid option string %r: must start with a character %r"
                                 % (option_string, chars))
            option_strings.append(option_string)
            if len(option_string) > 1 and option_string[1] in chars:
                long_strings.append(option_string)
        dest = kwargs.pop("dest", None)
        if dest is None:
            if long_strings:
                first = long_strings[0]
            else:
                first = option_strings[0]
            dest = first.lstrip(chars)
            if not dest:
                raise ValueError("dest= is required for options like %r" % (first,))
            dest = dest.replace("-", "_")
        kwargs["dest"] = dest
        kwargs["option_strings"] = option_strings
        return kwargs

    def add_argument_group(self, *args, **kwargs):
        group = _ArgumentGroup(self._parser_ref(), *args, **kwargs)
        self._parser_ref()._action_groups.append(group)
        return group

    def add_mutually_exclusive_group(self, required=False):
        group = _MutuallyExclusiveGroup(self, required)
        self._parser_ref()._mutually_exclusive_groups.append(group)
        return group

    def set_defaults(self, **kwargs):
        parser = self._parser_ref()
        parser._defaults.update(kwargs)
        for action in parser._actions:
            if action.dest in kwargs:
                action.default = kwargs[action.dest]

    def get_default(self, dest):
        parser = self._parser_ref()
        for action in parser._actions:
            if action.dest == dest and action.default is not None:
                return action.default
        return parser._defaults.get(dest, None)


class _ArgumentGroup(_ActionsContainer):
    def __init__(self, parser, title=None, description=None, **kwargs):
        self._parser = parser
        self.title = title
        self.description = description
        self._group_actions = []

    def _parser_ref(self):
        return self._parser

    def _add_action(self, action):
        self._parser._register(action)
        self._group_actions.append(action)
        return action


class _MutuallyExclusiveGroup(_ActionsContainer):
    def __init__(self, container, required=False):
        self._container = container
        self.required = required
        self._group_actions = []

    def _parser_ref(self):
        return self._container._parser_ref()

    def _add_action(self, action):
        if action.required:
            raise ValueError("mutually exclusive arguments must be optional")
        self._container._add_action(action)
        self._group_actions.append(action)
        return action

    def add_mutually_exclusive_group(self, required=False):
        raise ValueError("mutually exclusive groups cannot be nested")


# ---------------------------------------------------------------- the parser

class ArgumentParser(_ActionsContainer):
    """Parses a command line into a Namespace."""

    def __init__(self, prog=None, usage=None, description=None, epilog=None, parents=[],
                 formatter_class=HelpFormatter, prefix_chars="-", fromfile_prefix_chars=None,
                 argument_default=None, conflict_handler="error", add_help=True,
                 allow_abbrev=True, exit_on_error=True, suggest_on_error=False, color=True):
        if prog is None:
            prog = os.path.basename(sys.argv[0]) if sys.argv and sys.argv[0] else "main"
        self.prog = prog
        self.usage = usage
        self.description = description
        self.epilog = epilog
        self.formatter_class = formatter_class
        self.prefix_chars = prefix_chars
        self.fromfile_prefix_chars = fromfile_prefix_chars
        self.argument_default = argument_default
        self.conflict_handler = conflict_handler
        self.add_help = add_help
        self.allow_abbrev = allow_abbrev
        self.exit_on_error = exit_on_error
        self.suggest_on_error = suggest_on_error
        self.version = None
        self._actions = []
        self._option_string_actions = {}
        self._action_groups = []
        self._mutually_exclusive_groups = []
        self._defaults = {}
        self._has_negative_number_optionals = []
        self._subparsers = None
        self._positionals = self.add_argument_group("positional arguments")
        self._optionals = self.add_argument_group("options")
        default_prefix = "-" if "-" in prefix_chars else prefix_chars[0]
        if add_help:
            self.add_argument(default_prefix + "h", default_prefix * 2 + "help", action="help",
                              default=SUPPRESS, help="show this help message and exit")
        for parent in parents:
            self._add_container_actions(parent)
            self._defaults.update(parent._defaults)

    def _parser_ref(self):
        return self

    def _add_container_actions(self, parent):
        groups = {}
        for group in parent._action_groups:
            if group is parent._positionals:
                groups[id(group)] = self._positionals
            elif group is parent._optionals:
                groups[id(group)] = self._optionals
            else:
                groups[id(group)] = self.add_argument_group(group.title, group.description)
        owner = {}
        for group in parent._action_groups:
            for action in group._group_actions:
                owner[id(action)] = groups[id(group)]
        exclusive = {}
        for group in parent._mutually_exclusive_groups:
            target = self.add_mutually_exclusive_group(required=group.required)
            for action in group._group_actions:
                exclusive[id(action)] = target
        for action in parent._actions:
            if action.option_strings and self.add_help and action.option_strings[0] in ("-h", "--help") \
                    and isinstance(action, _HelpAction):
                continue
            target = owner.get(id(action), self._optionals)
            self._register(action)
            target._group_actions.append(action)
            if id(action) in exclusive:
                exclusive[id(action)]._group_actions.append(action)

    def _add_action(self, action):
        if action.option_strings:
            return self._optionals._add_action(action)
        return self._positionals._add_action(action)

    def _register(self, action):
        conflicts = [s for s in action.option_strings if s in self._option_string_actions]
        if conflicts:
            if self.conflict_handler == "resolve":
                for option_string in conflicts:
                    other = self._option_string_actions.pop(option_string)
                    other.option_strings.remove(option_string)
                    if not other.option_strings:
                        self._remove_action(other)
            else:
                raise ArgumentError(action, "conflicting option string%s: %s"
                                    % ("s" if len(conflicts) > 1 else "", ", ".join(conflicts)))
        self._actions.append(action)
        for option_string in action.option_strings:
            self._option_string_actions[option_string] = action
            if _NEGATIVE_NUMBER.match(option_string) and not self._has_negative_number_optionals:
                self._has_negative_number_optionals.append(True)

    def _remove_action(self, action):
        self._actions.remove(action)
        for group in self._action_groups:
            if action in group._group_actions:
                group._group_actions.remove(action)

    def add_subparsers(self, **kwargs):
        if self._subparsers is not None:
            raise ValueError("cannot have multiple subparser arguments")
        kwargs.setdefault("parser_class", type(self))
        if "title" in kwargs or "description" in kwargs:
            title = kwargs.pop("title", None)
            if title is None:
                title = "subcommands"
            description = kwargs.pop("description", None)
            self._subparsers = self.add_argument_group(title, description)
        else:
            self._subparsers = self._positionals
        if kwargs.get("prog") is None:
            formatter = self._get_formatter()
            positionals = [a for a in self._actions if not a.option_strings]
            formatter.add_usage(None, positionals, self._mutually_exclusive_groups, "")
            kwargs["prog"] = formatter.format_help().strip()
        action_class = kwargs.pop("action", _SubParsersAction)
        action = action_class(option_strings=[], **kwargs)
        self._subparsers._add_action(action)
        return action

    # -- parsing

    def parse_args(self, args=None, namespace=None):
        args, argv = self.parse_known_args(args, namespace)
        if argv:
            self.error("unrecognized arguments: %s" % " ".join(argv))
        return args

    def parse_known_args(self, args=None, namespace=None):
        if args is None:
            args = sys.argv[1:]
        else:
            args = list(args)
        if namespace is None:
            namespace = Namespace()
        for action in self._actions:
            if action.dest is not SUPPRESS and action.default is not SUPPRESS:
                if not hasattr(namespace, action.dest):
                    setattr(namespace, action.dest, action.default)
        for dest in self._defaults:
            if not hasattr(namespace, dest):
                setattr(namespace, dest, self._defaults[dest])
        if self.exit_on_error:
            try:
                namespace, args = self._parse_known_args(args, namespace)
            except ArgumentError as err:
                self.error(str(err))
        else:
            namespace, args = self._parse_known_args(args, namespace)
        if _UNRECOGNIZED_ARGS_ATTR in namespace.__dict__:
            args.extend(getattr(namespace, _UNRECOGNIZED_ARGS_ATTR))
            delattr(namespace, _UNRECOGNIZED_ARGS_ATTR)
        return namespace, args

    def _parse_known_args(self, arg_strings, namespace):
        if self.fromfile_prefix_chars is not None:
            arg_strings = self._read_args_from_files(arg_strings)

        conflicts = {}
        for group in self._mutually_exclusive_groups:
            members = group._group_actions
            for i in range(len(members)):
                others = conflicts.setdefault(id(members[i]), [])
                others.extend(members[:i])
                others.extend(members[i + 1:])

        option_indices = {}
        pattern_parts = []
        rest_are_args = False
        for i in range(len(arg_strings)):
            arg = arg_strings[i]
            if rest_are_args:
                pattern_parts.append("A")
            elif arg == "--":
                pattern_parts.append("-")
                rest_are_args = True
            else:
                found = self._parse_optional(arg)
                if found is None:
                    pattern_parts.append("A")
                else:
                    option_indices[i] = found
                    pattern_parts.append("O")
        pattern = "".join(pattern_parts)

        seen = []
        seen_non_default = []
        extras = []
        positionals = [a for a in self._actions if not a.option_strings]

        def take_action(action, argument_strings, option_string=None):
            seen.append(action)
            values = self._get_values(action, argument_strings)
            if values is not action.default:
                seen_non_default.append(action)
                for other in conflicts.get(id(action), []):
                    if other in seen_non_default:
                        raise ArgumentError(action, "not allowed with argument %s" % _action_name(other))
            if values is not SUPPRESS:
                if action.deprecated and option_string is not None:
                    self._warning("option '%s' is deprecated" % option_string)
                action(self, namespace, values, option_string)

        def consume_optional(start_index):
            action, option_string, sep, explicit_arg = option_indices[start_index]
            tuples = []
            while True:
                if action is None:
                    extras.append(arg_strings[start_index])
                    return start_index + 1
                if explicit_arg is not None:
                    count = self._match_argument(action, "A")
                    chars = self.prefix_chars
                    if count == 0 and option_string[1] not in chars and explicit_arg != "":
                        if sep or explicit_arg[0] in chars:
                            raise ArgumentError(action, "ignored explicit argument %r" % explicit_arg)
                        tuples.append((action, [], option_string))
                        option_string = option_string[0] + explicit_arg[0]
                        explicit_arg = explicit_arg[1:] or None
                        if option_string in self._option_string_actions:
                            action = self._option_string_actions[option_string]
                        else:
                            raise ArgumentError(action, "ignored explicit argument %r"
                                                % (option_string[1] + (explicit_arg or "")))
                    elif count == 1:
                        stop = start_index + 1
                        tuples.append((action, [explicit_arg], option_string))
                        break
                    else:
                        raise ArgumentError(action, "ignored explicit argument %r" % explicit_arg)
                else:
                    start = start_index + 1
                    count = self._match_argument(action, pattern[start:])
                    stop = start + count
                    tuples.append((action, arg_strings[start:stop], option_string))
                    break
            for action, args, option_string in tuples:
                take_action(action, args, option_string)
            return stop

        def consume_positionals(start_index):
            counts = self._match_arguments_partial(positionals, pattern[start_index:])
            # A positional that may be empty is not given its empty match
            # while an option still follows: it waits, so arguments after the
            # option can still fill it.
            if "O" in pattern[start_index:]:
                while counts and counts[-1] == 0 and positionals[len(counts) - 1].nargs in (OPTIONAL, ZERO_OR_MORE):
                    counts.pop()
            for i in range(len(counts)):
                action = positionals[i]
                count = counts[i]
                args = arg_strings[start_index:start_index + count]
                if action.nargs == PARSER:
                    if pattern[start_index] == "-":
                        args.remove("--")
                elif action.nargs != REMAINDER:
                    if "-" in pattern[start_index:start_index + count]:
                        args.remove("--")
                start_index += count
                take_action(action, args)
            del positionals[:len(counts)]
            return start_index

        start_index = 0
        max_option_index = max(option_indices) if option_indices else -1
        while start_index <= max_option_index:
            next_option = min([i for i in option_indices if i >= start_index])
            if start_index != next_option:
                end = consume_positionals(start_index)
                if end > start_index:
                    start_index = end
                    continue
                start_index = end
            if start_index not in option_indices:
                extras.extend(arg_strings[start_index:next_option])
                start_index = next_option
            start_index = consume_optional(start_index)

        stop_index = consume_positionals(start_index)
        extras.extend(arg_strings[stop_index:])

        required = []
        for action in self._actions:
            if action not in seen:
                if action.required:
                    required.append(_action_name(action))
                elif (action.default is not None and isinstance(action.default, str)
                      and hasattr(namespace, action.dest)
                      and action.default is getattr(namespace, action.dest)):
                    setattr(namespace, action.dest, self._get_value(action, action.default))
        if required:
            raise ArgumentError(None, "the following arguments are required: %s" % ", ".join(required))

        for group in self._mutually_exclusive_groups:
            if group.required:
                found = False
                for action in group._group_actions:
                    if action in seen_non_default:
                        found = True
                        break
                if not found:
                    names = [_action_name(a) for a in group._group_actions if a.help is not SUPPRESS]
                    raise ArgumentError(None, "one of the arguments %s is required" % " ".join(names))
        return namespace, extras

    def _read_args_from_files(self, arg_strings):
        result = []
        for arg in arg_strings:
            if not arg or arg[0] not in self.fromfile_prefix_chars:
                result.append(arg)
                continue
            try:
                with open(arg[1:]) as f:
                    lines = f.read().splitlines()
            except OSError as err:
                raise ArgumentError(None, str(err))
            expanded = []
            for line in lines:
                expanded.extend(self.convert_arg_line_to_args(line))
            result.extend(self._read_args_from_files(expanded))
        return result

    def convert_arg_line_to_args(self, arg_line):
        return [arg_line]

    def _match_argument(self, action, pattern):
        match = re.match(self._nargs_pattern(action), pattern)
        if match is None:
            nargs = action.nargs
            if nargs is None:
                message = "expected one argument"
            elif nargs == OPTIONAL:
                message = "expected at most one argument"
            elif nargs == ONE_OR_MORE:
                message = "expected at least one argument"
            elif nargs == 1:
                message = "expected 1 argument"
            else:
                message = "expected %s arguments" % nargs
            raise ArgumentError(action, message)
        return len(match.group(1))

    def _match_arguments_partial(self, actions, pattern):
        for i in range(len(actions), 0, -1):
            regex = "".join([self._nargs_pattern(a) for a in actions[:i]])
            match = re.match(regex, pattern)
            if match is not None:
                return [len(g) for g in match.groups()]
        return []

    def _nargs_pattern(self, action):
        nargs = action.nargs
        if nargs is None:
            result = "(-*A-*)"
        elif nargs == OPTIONAL:
            result = "(-*A?-*)"
        elif nargs == ZERO_OR_MORE:
            result = "(-*[A-]*)"
        elif nargs == ONE_OR_MORE:
            result = "(-*A[A-]*)"
        elif nargs == REMAINDER:
            result = "([AO]*)" if action.option_strings else "([-AO]*)"
        elif nargs == PARSER:
            result = "(-*A[-AO]*)"
        elif nargs == SUPPRESS:
            result = "(-*-*)"
        else:
            result = "(-*%s-*)" % "-*".join(["A"] * nargs)
        if action.option_strings:
            result = result.replace("-*", "").replace("-", "")
        return result

    def _parse_optional(self, arg):
        if not arg or arg[0] not in self.prefix_chars:
            return None
        if arg in self._option_string_actions:
            return (self._option_string_actions[arg], arg, None, None)
        if len(arg) == 1:
            return None
        if "=" in arg:
            option_string, sep, explicit_arg = arg.partition("=")
            if option_string in self._option_string_actions:
                return (self._option_string_actions[option_string], option_string, sep, explicit_arg)
        matches = self._option_tuples(arg)
        if len(matches) > 1:
            names = ", ".join([m[1] for m in matches])
            raise ArgumentError(None, "ambiguous option: %s could match %s" % (arg, names))
        if len(matches) == 1:
            return matches[0]
        if _NEGATIVE_NUMBER.match(arg) and not self._has_negative_number_optionals:
            return None
        if " " in arg:
            return None
        return (None, arg, None, None)

    def _option_tuples(self, arg):
        result = []
        chars = self.prefix_chars
        if arg[0] in chars and arg[1] in chars:
            if self.allow_abbrev:
                prefix, sep, explicit_arg = arg.partition("=")
                if not sep:
                    sep = None
                    explicit_arg = None
                for option_string in self._option_string_actions:
                    if option_string.startswith(prefix):
                        result.append((self._option_string_actions[option_string], option_string, sep,
                                       explicit_arg))
        elif arg[0] in chars:
            prefix, sep, explicit_arg = arg.partition("=")
            if not sep:
                sep = None
                explicit_arg = None
            short_prefix = arg[:2]
            short_explicit = arg[2:]
            for option_string in self._option_string_actions:
                if option_string == short_prefix:
                    result.append((self._option_string_actions[option_string], option_string, "",
                                   short_explicit))
                elif self.allow_abbrev and option_string.startswith(prefix):
                    result.append((self._option_string_actions[option_string], option_string, sep,
                                   explicit_arg))
        return result

    def _get_values(self, action, arg_strings):
        nargs = action.nargs
        if not arg_strings and nargs == OPTIONAL:
            value = action.const if action.option_strings else action.default
            if isinstance(value, str) and value is not SUPPRESS:
                value = self._get_value(action, value)
        elif not arg_strings and nargs == ZERO_OR_MORE and not action.option_strings:
            if action.default is not None:
                value = action.default
            else:
                value = []
        elif len(arg_strings) == 1 and nargs in [None, OPTIONAL]:
            value = self._get_value(action, arg_strings[0])
            self._check_value(action, value)
        elif nargs == REMAINDER:
            value = [self._get_value(action, v) for v in arg_strings]
        elif nargs == PARSER:
            value = [self._get_value(action, v) for v in arg_strings]
            self._check_value(action, value[0])
        elif nargs == SUPPRESS:
            value = SUPPRESS
        else:
            value = [self._get_value(action, v) for v in arg_strings]
            for v in value:
                self._check_value(action, v)
        return value

    def _get_value(self, action, arg_string):
        type_func = action.type
        if type_func is None:
            return arg_string
        try:
            return type_func(arg_string)
        except ArgumentTypeError as err:
            raise ArgumentError(action, str(err))
        except (TypeError, ValueError):
            name = getattr(type_func, "__name__", None)
            if name is None:
                name = repr(type_func)
            raise ArgumentError(action, "invalid %s value: %r" % (name, arg_string))

    def _check_value(self, action, value):
        choices = action.choices
        if choices is None:
            return
        if isinstance(choices, str):
            choices = list(choices)
        if value not in choices:
            names = ", ".join([repr(c) for c in choices])
            raise ArgumentError(action, "invalid choice: %r (choose from %s)" % (value, names))

    # -- help and errors

    def _get_formatter(self):
        return self.formatter_class(prog=self.prog)

    def format_usage(self):
        formatter = self._get_formatter()
        formatter.add_usage(self.usage, self._actions, self._mutually_exclusive_groups)
        return formatter.format_help()

    def format_help(self):
        formatter = self._get_formatter()
        formatter.add_usage(self.usage, self._actions, self._mutually_exclusive_groups)
        formatter.add_text(self.description)
        for group in self._action_groups:
            formatter.start_section(group.title)
            formatter.add_text(group.description)
            formatter.add_arguments(group._group_actions)
            formatter.end_section()
        formatter.add_text(self.epilog)
        return formatter.format_help()

    def print_usage(self, file=None):
        if file is None:
            file = sys.stdout
        self._print_message(self.format_usage(), file)

    def print_help(self, file=None):
        if file is None:
            file = sys.stdout
        self._print_message(self.format_help(), file)

    def _print_message(self, message, file=None):
        if message:
            if file is None:
                file = sys.stderr
            file.write(message)

    def exit(self, status=0, message=None):
        if message:
            self._print_message(message, sys.stderr)
        sys.exit(status)

    def error(self, message):
        """Prints the usage and the message to standard error and exits with
        status 2."""
        self.print_usage(sys.stderr)
        self.exit(2, "%s: error: %s\n" % (self.prog, message))

    def _warning(self, message):
        self._print_message("%s: warning: %s\n" % (self.prog, message), sys.stderr)
