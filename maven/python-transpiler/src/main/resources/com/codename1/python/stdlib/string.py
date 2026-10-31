# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Common string constants and the Formatter and Template classes."""

whitespace = " \t\n\r\x0b\x0c"
ascii_lowercase = "abcdefghijklmnopqrstuvwxyz"
ascii_uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
ascii_letters = ascii_lowercase + ascii_uppercase
digits = "0123456789"
hexdigits = digits + "abcdef" + "ABCDEF"
octdigits = "01234567"
punctuation = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
printable = digits + ascii_letters + punctuation + whitespace


def capwords(s, sep=None):
    return (sep or " ").join(map(str.capitalize, s.split(sep)))


class Template:
    delimiter = "$"

    def __init__(self, template):
        self.template = template

    def _sub(self, mapping, safe):
        t = self.template
        d = self.delimiter
        out = []
        i = 0
        n = len(t)
        while i < n:
            if not t.startswith(d, i):
                out.append(t[i])
                i += 1
                continue
            j = i + len(d)
            if t.startswith(d, j):
                out.append(d)
                i = j + len(d)
                continue
            braced = j < n and t[j] == "{"
            k = j + 1 if braced else j
            e = k
            while e < n and (t[e] == "_" or t[e].isalnum() and t[e].isascii()) and not (e == k and t[e].isdigit()):
                e += 1
            name = t[k:e]
            if not name or braced and (e >= n or t[e] != "}"):
                if safe:
                    out.append(d)
                    i = j
                    continue
                line = t.count("\n", 0, i) + 1
                col = i - (t.rfind("\n", 0, i) + 1) + 1
                raise ValueError("Invalid placeholder in string: line %d, col %d" % (line, col))
            end = e + 1 if braced else e
            if name in mapping:
                out.append(str(mapping[name]))
            elif safe:
                out.append(t[i:end])
            else:
                raise KeyError(name)
            i = end
        return "".join(out)

    def substitute(self, mapping=None, /, **kws):
        return self._sub(_merge(mapping, kws), False)

    def safe_substitute(self, mapping=None, /, **kws):
        return self._sub(_merge(mapping, kws), True)

    def get_identifiers(self):
        ids = []
        t = self.template
        d = self.delimiter
        i = t.find(d)
        while i >= 0:
            j = i + len(d)
            if t.startswith(d, j):
                i = t.find(d, j + len(d))
                continue
            k = j + 1 if j < len(t) and t[j] == "{" else j
            e = k
            while e < len(t) and (t[e] == "_" or t[e].isalnum()):
                e += 1
            name = t[k:e]
            if name and name not in ids:
                ids.append(name)
            i = t.find(d, max(e, j))
        return ids

    def is_valid(self):
        try:
            self._sub({}, False)
        except KeyError:
            return True
        except ValueError:
            return False
        return True


def _merge(mapping, kws):
    if mapping is None:
        return kws
    if not kws:
        return mapping
    merged = dict(mapping)
    merged.update(kws)
    return merged


def _formatter_parser(s):
    """Splits a format string as str.format reads it: (literal, field, spec, conversion) tuples."""
    i = 0
    n = len(s)
    literal = []
    while i < n:
        c = s[i]
        if c == "{":
            if i + 1 < n and s[i + 1] == "{":
                literal.append("{")
                i += 2
                # CPython ends a literal at each escaped brace.
                yield ("".join(literal), None, None, None)
                literal = []
                continue
            depth = 1
            j = i + 1
            while j < n and depth:
                if s[j] == "{":
                    depth += 1
                elif s[j] == "}":
                    depth -= 1
                j += 1
            if depth:
                raise ValueError("expected '}' before end of string")
            body = s[i + 1:j - 1]
            k = 0
            bracket = False
            while k < len(body):
                ch = body[k]
                if ch == "[":
                    bracket = True
                elif ch == "]":
                    bracket = False
                elif not bracket and (ch == "!" or ch == ":"):
                    break
                k += 1
            name = body[:k]
            conversion = None
            spec = ""
            if k < len(body) and body[k] == "!":
                if k + 1 >= len(body):
                    raise ValueError("end of string while looking for conversion specifier")
                conversion = body[k + 1]
                k += 2
                if k < len(body) and body[k] != ":":
                    raise ValueError("expected ':' after conversion specifier")
            if k < len(body) and body[k] == ":":
                spec = body[k + 1:]
            yield ("".join(literal), name, spec, conversion)
            literal = []
            i = j
        elif c == "}":
            if i + 1 < n and s[i + 1] == "}":
                literal.append("}")
                i += 2
                # CPython ends a literal at each escaped brace.
                yield ("".join(literal), None, None, None)
                literal = []
                continue
            raise ValueError("Single '}' encountered in format string")
        else:
            literal.append(c)
            i += 1
    if literal:
        yield ("".join(literal), None, None, None)


def _field_name_split(name):
    i = 0
    while i < len(name) and name[i] not in ".[":
        i += 1
    first = name[:i]
    if first.isdigit():
        first = int(first)
    rest = []
    while i < len(name):
        if name[i] == ".":
            j = i + 1
            while j < len(name) and name[j] not in ".[":
                j += 1
            if j == i + 1:
                raise ValueError("Empty attribute in format string")
            rest.append((True, name[i + 1:j]))
            i = j
        elif name[i] == "[":
            j = name.find("]", i)
            if j < 0:
                raise ValueError("Missing ']' in format string")
            key = name[i + 1:j]
            if key == "":
                raise ValueError("Empty attribute in format string")
            rest.append((False, int(key) if key.isdigit() else key))
            i = j + 1
        else:
            raise ValueError("Only '.' or '[' may follow ']' in format field specifier")
    return first, iter(rest)


class Formatter:
    def format(self, format_string, /, *args, **kwargs):
        return self.vformat(format_string, args, kwargs)

    def vformat(self, format_string, args, kwargs):
        used_args = set()
        result, _ = self._vformat(format_string, args, kwargs, used_args, 2)
        self.check_unused_args(used_args, args, kwargs)
        return result

    def _vformat(self, format_string, args, kwargs, used_args, recursion_depth, auto_arg_index=0):
        if recursion_depth < 0:
            raise ValueError("Max string recursion exceeded")
        result = []
        for literal_text, field_name, format_spec, conversion in self.parse(format_string):
            if literal_text:
                result.append(literal_text)
            if field_name is not None:
                if field_name == "":
                    if auto_arg_index is False:
                        raise ValueError("cannot switch from manual field specification to automatic field numbering")
                    field_name = str(auto_arg_index)
                    auto_arg_index += 1
                elif field_name.isdigit():
                    if auto_arg_index:
                        raise ValueError("cannot switch from automatic field numbering to manual field specification")
                    auto_arg_index = False
                obj, arg_used = self.get_field(field_name, args, kwargs)
                used_args.add(arg_used)
                obj = self.convert_field(obj, conversion)
                format_spec, auto_arg_index = self._vformat(format_spec, args, kwargs, used_args,
                                                            recursion_depth - 1, auto_arg_index=auto_arg_index)
                result.append(self.format_field(obj, format_spec))
        return "".join(result), auto_arg_index

    def get_value(self, key, args, kwargs):
        if isinstance(key, int):
            return args[key]
        return kwargs[key]

    def check_unused_args(self, used_args, args, kwargs):
        pass

    def format_field(self, value, format_spec):
        return format(value, format_spec)

    def convert_field(self, value, conversion):
        if conversion is None:
            return value
        elif conversion == "s":
            return str(value)
        elif conversion == "r":
            return repr(value)
        elif conversion == "a":
            return ascii(value)
        raise ValueError("Unknown conversion specifier {0!s}".format(conversion))

    def parse(self, format_string):
        return _formatter_parser(format_string)

    def get_field(self, field_name, args, kwargs):
        first, rest = _field_name_split(field_name)
        obj = self.get_value(first, args, kwargs)
        for is_attr, i in rest:
            if is_attr:
                obj = getattr(obj, i)
            else:
                obj = obj[i]
        return obj, first
