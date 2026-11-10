# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""CSV reading and writing, with the parsing rules of CPython's _csv."""

from io import StringIO

__all__ = ["QUOTE_MINIMAL", "QUOTE_ALL", "QUOTE_NONNUMERIC", "QUOTE_NONE", "QUOTE_STRINGS", "QUOTE_NOTNULL",
           "Error", "Dialect", "excel", "excel_tab", "unix_dialect", "field_size_limit", "reader", "writer",
           "register_dialect", "get_dialect", "list_dialects", "unregister_dialect", "DictReader", "DictWriter",
           "Sniffer"]

QUOTE_MINIMAL, QUOTE_ALL, QUOTE_NONNUMERIC, QUOTE_NONE, QUOTE_STRINGS, QUOTE_NOTNULL = range(6)
_QUOTINGS = (QUOTE_MINIMAL, QUOTE_ALL, QUOTE_NONNUMERIC, QUOTE_NONE, QUOTE_STRINGS, QUOTE_NOTNULL)

__version__ = "1.0"

_field_limit = 128 * 1024


class Error(Exception):
    pass


def field_size_limit(*args):
    global _field_limit
    if len(args) > 1:
        raise TypeError("field_size_limit expected at most 1 argument, got %d" % len(args))
    old = _field_limit
    if args:
        if not isinstance(args[0], int):
            raise TypeError("limit must be an integer")
        _field_limit = args[0]
    return old


class Dialect:
    _name = ""
    _valid = False
    delimiter = None
    quotechar = None
    escapechar = None
    doublequote = None
    skipinitialspace = None
    lineterminator = None
    quoting = None
    strict = None

    def __init__(self):
        if self.__class__ != Dialect:
            self._valid = True
        self._validate()

    def _validate(self):
        try:
            _Dialect(self)
        except TypeError as e:
            raise Error(str(e)) from None


class excel(Dialect):
    delimiter = ","
    quotechar = '"'
    doublequote = True
    skipinitialspace = False
    lineterminator = "\r\n"
    quoting = QUOTE_MINIMAL


class excel_tab(excel):
    delimiter = "\t"


class unix_dialect(Dialect):
    delimiter = ","
    quotechar = '"'
    doublequote = True
    skipinitialspace = False
    lineterminator = "\n"
    quoting = QUOTE_ALL


_dialects = {}


def _char(name, value, allow_none):
    if value is None:
        if allow_none:
            return None
        raise TypeError('"%s" must be a unicode character, not NoneType' % name)
    if not isinstance(value, str):
        raise TypeError('"%s" must be a unicode character or None, not %s' % (name, type(value).__name__)
                        if allow_none else '"%s" must be a unicode character, not %s' % (name, type(value).__name__))
    if len(value) != 1:
        raise TypeError('"%s" must be a unicode character or None, not a string of length %d' % (name, len(value))
                        if allow_none else '"%s" must be a unicode character, not a string of length %d'
                        % (name, len(value)))
    return value


class _Dialect:
    """The validated parameters a reader or writer works with."""

    def __init__(self, dialect=None, **kw):
        if isinstance(dialect, str):
            dialect = get_dialect(dialect)

        def pick(name, default):
            if name in kw:
                return kw[name]
            if dialect is not None and getattr(dialect, name, None) is not None:
                return getattr(dialect, name)
            return default

        self.delimiter = _char("delimiter", pick("delimiter", ","), False)
        self.doublequote = bool(pick("doublequote", True))
        self.escapechar = _char("escapechar", pick("escapechar", None), True)
        self.lineterminator = pick("lineterminator", "\r\n")
        if not isinstance(self.lineterminator, str):
            raise TypeError('"lineterminator" must be a string, not %s' % type(self.lineterminator).__name__)
        quoting = pick("quoting", QUOTE_MINIMAL)
        if quoting not in _QUOTINGS:
            raise TypeError("bad \"quoting\" value")
        self.quoting = quoting
        qc = pick("quotechar", '"')
        if qc is None and "quotechar" not in kw and (dialect is None or getattr(dialect, "quotechar", 0) is not None):
            qc = '"'
        self.quotechar = _char("quotechar", qc, True)
        self.skipinitialspace = bool(pick("skipinitialspace", False))
        self.strict = bool(pick("strict", False))
        if self.quotechar is None and self.quoting != QUOTE_NONE:
            if "quoting" in kw or (dialect is not None and getattr(dialect, "quoting", None) is not None):
                raise TypeError("quotechar must be set if quoting enabled")
            self.quoting = QUOTE_NONE
        if self.delimiter == " " and self.skipinitialspace:
            raise ValueError("bad delimiter or skipinitialspace value")
        if self.delimiter in ("\r", "\n"):
            raise ValueError("bad delimiter value")
        if self.quotechar in ("\r", "\n"):
            raise ValueError("bad quotechar value")
        if self.escapechar in ("\r", "\n"):
            raise ValueError("bad escapechar value")
        if "\r" in self.lineterminator or "\n" in self.lineterminator:
            pass
        if self.escapechar is not None and self.escapechar == self.delimiter:
            raise ValueError("bad delimiter or escapechar value")
        if self.quotechar is not None and self.quotechar == self.delimiter:
            raise ValueError("bad delimiter or quotechar value")
        if self.escapechar is not None and self.escapechar == self.quotechar:
            raise ValueError("bad escapechar or quotechar value")


def register_dialect(name, dialect=None, **fmtparams):
    if not isinstance(name, str):
        raise TypeError("dialect name must be a string")
    d = _Dialect(dialect, **fmtparams)
    _dialects[name] = _Frozen(d)


def unregister_dialect(name):
    if name not in _dialects:
        raise Error("unknown dialect")
    del _dialects[name]


def get_dialect(name):
    try:
        return _dialects[name]
    except (KeyError, TypeError):
        raise Error("unknown dialect")


def list_dialects():
    return list(_dialects)


class _Frozen:
    __slots__ = ("delimiter", "quotechar", "escapechar", "doublequote", "skipinitialspace", "lineterminator",
                 "quoting", "strict")

    def __init__(self, d):
        for n in self.__slots__:
            object.__setattr__(self, n, getattr(d, n))

    def __setattr__(self, name, value):
        raise AttributeError("attribute '%s' of '_csv.Dialect' objects is not writable" % name)


register_dialect("excel", excel)
register_dialect("excel-tab", excel_tab)
register_dialect("unix", unix_dialect)

(_START_RECORD, _START_FIELD, _ESCAPED_CHAR, _IN_FIELD, _IN_QUOTED_FIELD, _ESCAPE_IN_QUOTED_FIELD,
 _QUOTE_IN_QUOTED_FIELD, _EAT_CRNL, _AFTER_ESCAPED_CRNL) = range(9)


class _Reader:
    def __init__(self, iterable, dialect="excel", **fmtparams):
        self._it = iter(iterable)
        self.dialect = _Frozen(_Dialect(dialect, **fmtparams))
        self._d = self.dialect
        self.line_num = 0

    def __iter__(self):
        return self

    def _parse_reset(self):
        self._fields = []
        self._field = []
        self._state = _START_RECORD
        self._unquoted = True
        self._quoted_once = False

    def _save_field(self):
        field = "".join(self._field)
        self._field = []
        d = self._d
        if self._unquoted and d.quoting != QUOTE_NONE:
            if d.quoting == QUOTE_NONNUMERIC and field:
                field = float(field)
            elif d.quoting in (QUOTE_NOTNULL, QUOTE_STRINGS) and not field and not self._quoted_once:
                field = None
            elif d.quoting == QUOTE_STRINGS and field:
                field = float(field)
        self._fields.append(field)
        self._unquoted = True
        self._quoted_once = False

    def _add_char(self, c):
        if len(self._field) >= _field_limit:
            raise Error("field larger than field limit (%d)" % _field_limit)
        self._field.append(c)

    def _process_char(self, c):
        d = self._d
        s = self._state
        if s == _START_RECORD:
            if c is None:
                return
            elif c == "\n" or c == "\r":
                self._state = _EAT_CRNL
                return
            s = self._state = _START_FIELD
        if s == _START_FIELD:
            if c is None or c in ("\n", "\r"):
                self._save_field()
                self._state = _START_RECORD if c is None else _EAT_CRNL
            elif c == d.quotechar and d.quoting != QUOTE_NONE:
                self._unquoted = False
                self._quoted_once = True
                self._state = _IN_QUOTED_FIELD
            elif c == d.escapechar:
                self._state = _ESCAPED_CHAR
            elif c == " " and d.skipinitialspace:
                pass
            elif c == d.delimiter:
                self._save_field()
            else:
                if d.quoting == QUOTE_NONNUMERIC:
                    self._numeric = True
                self._add_char(c)
                self._state = _IN_FIELD
        elif s == _ESCAPED_CHAR:
            if c in ("\n", "\r"):
                self._add_char(c)
                self._state = _AFTER_ESCAPED_CRNL
                return
            if c is None:
                c = "\n"
            self._add_char(c)
            self._state = _IN_FIELD
        elif s == _AFTER_ESCAPED_CRNL:
            if c is None:
                return
            self._state = _IN_FIELD
            self._process_char(c)
        elif s == _IN_FIELD:
            if c is None or c in ("\n", "\r"):
                self._save_field()
                self._state = _START_RECORD if c is None else _EAT_CRNL
            elif c == d.escapechar:
                self._state = _ESCAPED_CHAR
            elif c == d.delimiter:
                self._save_field()
                self._state = _START_FIELD
            else:
                self._add_char(c)
        elif s == _IN_QUOTED_FIELD:
            if c is None:
                pass
            elif c == d.escapechar:
                self._state = _ESCAPE_IN_QUOTED_FIELD
            elif c == d.quotechar and d.quoting != QUOTE_NONE:
                if d.doublequote:
                    self._state = _QUOTE_IN_QUOTED_FIELD
                else:
                    self._state = _IN_FIELD
            else:
                self._add_char(c)
        elif s == _ESCAPE_IN_QUOTED_FIELD:
            if c is None:
                c = "\n"
            self._add_char(c)
            self._state = _IN_QUOTED_FIELD
        elif s == _QUOTE_IN_QUOTED_FIELD:
            if d.quoting != QUOTE_NONE and c == d.quotechar:
                self._add_char(c)
                self._state = _IN_QUOTED_FIELD
            elif c == d.delimiter:
                self._save_field()
                self._state = _START_FIELD
            elif c is None or c in ("\n", "\r"):
                self._save_field()
                self._state = _START_RECORD if c is None else _EAT_CRNL
            elif not d.strict:
                self._add_char(c)
                self._state = _IN_FIELD
            else:
                raise Error("'%c' expected after '%c'" % (d.delimiter, d.quotechar))
        elif s == _EAT_CRNL:
            if c in ("\n", "\r"):
                pass
            elif c is None:
                self._state = _START_RECORD
            else:
                raise Error("new-line character seen in unquoted field - do you need to open the file with "
                            "newline=''?")

    def __next__(self):
        self._parse_reset()
        while True:
            try:
                line = next(self._it)
            except StopIteration:
                if len(self._field) != 0 or self._state == _IN_QUOTED_FIELD:
                    if self._d.strict:
                        raise Error("unexpected end of data")
                    self._save_field()
                    break
                if self._fields or self._state == _START_FIELD and False:
                    break
                raise
            if not isinstance(line, str):
                raise Error("iterator should return strings, not %.200s (the file should be opened in text mode)"
                            % type(line).__name__)
            self.line_num += 1
            for c in line:
                self._process_char(c)
            self._process_char(None)
            if self._state == _START_RECORD:
                break
        fields = self._fields
        self._fields = []
        return fields


def reader(csvfile, dialect="excel", **fmtparams):
    return _Reader(csvfile, dialect, **fmtparams)


class _Writer:
    def __init__(self, fileobj, dialect="excel", **fmtparams):
        if not hasattr(fileobj, "write"):
            raise TypeError("argument 1 must have a \"write\" method")
        self._write = fileobj.write
        self.dialect = _Frozen(_Dialect(dialect, **fmtparams))
        self._d = self.dialect

    def _join_append(self, out, field, quoted):
        d = self._d
        need_escape = []
        specials = (d.delimiter, d.escapechar, d.quotechar, "\n", "\r")
        if field and (field[0] == " " or field[-1] == " ") and False:
            quoted = True
        chars = []
        for c in field:
            want_escape = False
            if c in specials and c is not None:
                if d.quoting == QUOTE_NONE:
                    want_escape = True
                else:
                    if c == d.quotechar:
                        if d.doublequote:
                            chars.append(d.quotechar)
                        else:
                            want_escape = True
                    elif c == d.escapechar:
                        want_escape = True
                    if not want_escape:
                        quoted = True
                if want_escape:
                    if d.escapechar is None:
                        raise Error("need to escape, but no escapechar set")
                    chars.append(d.escapechar)
            chars.append(c)
        body = "".join(chars)
        if not field and d.quoting == QUOTE_NONE and len(self._row_fields) == 1:
            pass
        if quoted:
            body = d.quotechar + body + d.quotechar
        out.append(body)

    def writerow(self, row):
        d = self._d
        try:
            it = iter(row)
        except TypeError:
            raise Error("iterable expected, not %s" % type(row).__name__) from None
        self._row_fields = list(it)
        out = []
        n = len(self._row_fields)
        for i, field in enumerate(self._row_fields):
            if i:
                out.append(d.delimiter)
            if d.quoting == QUOTE_NONNUMERIC:
                quoted = not isinstance(field, (int, float)) or isinstance(field, bool) and False
                if field is None:
                    quoted = True
            elif d.quoting == QUOTE_ALL:
                quoted = True
            elif d.quoting == QUOTE_STRINGS:
                quoted = isinstance(field, str)
            elif d.quoting == QUOTE_NOTNULL:
                quoted = field is not None
            else:
                quoted = False
            if field is None:
                text = ""
            elif isinstance(field, str):
                text = field
            else:
                text = str(field)
            if (d.quoting == QUOTE_MINIMAL and not quoted and not text and n == 1):
                quoted = True
            if d.quoting == QUOTE_NONE and not text and n == 1:
                raise Error("single empty field record must be quoted")
            self._join_append(out, text, quoted)
        out.append(d.lineterminator)
        return self._write("".join(out))

    def writerows(self, rows):
        for row in rows:
            self.writerow(row)


def writer(csvfile, dialect="excel", **fmtparams):
    return _Writer(csvfile, dialect, **fmtparams)


class DictReader:
    def __init__(self, f, fieldnames=None, restkey=None, restval=None, dialect="excel", *args, **kwds):
        if fieldnames is not None and iter(fieldnames) is fieldnames:
            fieldnames = list(fieldnames)
        self._fieldnames = fieldnames
        self.restkey = restkey
        self.restval = restval
        self.reader = reader(f, dialect, *args, **kwds)
        self.dialect = dialect
        self.line_num = 0

    def __iter__(self):
        return self

    @property
    def fieldnames(self):
        if self._fieldnames is None:
            try:
                self._fieldnames = next(self.reader)
            except StopIteration:
                pass
        self.line_num = self.reader.line_num
        return self._fieldnames

    @fieldnames.setter
    def fieldnames(self, value):
        self._fieldnames = value

    def __next__(self):
        if self.line_num == 0:
            self.fieldnames
        row = next(self.reader)
        self.line_num = self.reader.line_num
        while row == []:
            row = next(self.reader)
        d = dict(zip(self.fieldnames, row))
        lf = len(self.fieldnames)
        lr = len(row)
        if lf < lr:
            d[self.restkey] = row[lf:]
        elif lf > lr:
            for key in self.fieldnames[lr:]:
                d[key] = self.restval
        return d


class DictWriter:
    def __init__(self, f, fieldnames, restval="", extrasaction="raise", dialect="excel", *args, **kwds):
        if fieldnames is not None and iter(fieldnames) is fieldnames:
            fieldnames = list(fieldnames)
        self.fieldnames = fieldnames
        self.restval = restval
        extrasaction = extrasaction.lower()
        if extrasaction not in ("raise", "ignore"):
            raise ValueError("extrasaction (%s) must be 'raise' or 'ignore'" % extrasaction)
        self.extrasaction = extrasaction
        self.writer = writer(f, dialect, *args, **kwds)

    def writeheader(self):
        header = dict(zip(self.fieldnames, self.fieldnames))
        return self.writerow(header)

    def _dict_to_list(self, rowdict):
        if self.extrasaction == "raise":
            wrong_fields = rowdict.keys() - self.fieldnames
            if wrong_fields:
                raise ValueError("dict contains fields not in fieldnames: " + ", ".join([repr(x) for x in wrong_fields]))
        return (rowdict.get(key, self.restval) for key in self.fieldnames)

    def writerow(self, rowdict):
        return self.writer.writerow(self._dict_to_list(rowdict))

    def writerows(self, rowdicts):
        return self.writer.writerows(map(self._dict_to_list, rowdicts))


class Sniffer:
    def __init__(self):
        self.preferred = [",", "\t", ";", " ", ":"]

    def sniff(self, sample, delimiters=None):
        quotechar, doublequote, delimiter, skipinitialspace = self._guess_quote_and_delimiter(sample, delimiters)
        if not delimiter:
            delimiter, skipinitialspace = self._guess_delimiter(sample, delimiters)
        if not delimiter:
            raise Error("Could not determine delimiter")

        class dialect(Dialect):
            _name = "sniffed"
            lineterminator = "\r\n"
            quoting = QUOTE_MINIMAL

        dialect.doublequote = doublequote
        dialect.delimiter = delimiter
        dialect.quotechar = quotechar or '"'
        dialect.skipinitialspace = skipinitialspace
        return dialect

    def _guess_quote_and_delimiter(self, data, delimiters):
        import re
        matches = []
        for restr in (r'(?P<delim>[^\w\n"\'])(?P<space> ?)(?P<quote>["\']).*?(?P=quote)(?P=delim)',
                      r'(?:^|\n)(?P<quote>["\']).*?(?P=quote)(?P<delim>[^\w\n"\'])(?P<space> ?)',
                      r'(?P<delim>[^\w\n"\'])(?P<space> ?)(?P<quote>["\']).*?(?P=quote)(?:$|\n)',
                      r'(?:^|\n)(?P<quote>["\']).*?(?P=quote)(?:$|\n)'):
            regexp = re.compile(restr, re.DOTALL | re.MULTILINE)
            matches = regexp.findall(data)
            if matches:
                break
        if not matches:
            return ("", False, None, 0)
        quotes = {}
        delims = {}
        spaces = 0
        groupindex = regexp.groupindex
        for m in matches:
            n = groupindex["quote"] - 1
            key = m[n]
            if key:
                quotes[key] = quotes.get(key, 0) + 1
            try:
                n = groupindex["delim"] - 1
                key = m[n]
            except KeyError:
                continue
            if key and (delimiters is None or key in delimiters):
                delims[key] = delims.get(key, 0) + 1
            try:
                n = groupindex["space"] - 1
            except KeyError:
                continue
            if m[n]:
                spaces += 1
        quotechar = max(quotes, key=quotes.get)
        if delims:
            delim = max(delims, key=delims.get)
            skipinitialspace = delims[delim] == spaces
            if delim == "\n":
                delim = ""
        else:
            delim = ""
            skipinitialspace = 0
        dq_regexp = re.compile(r"((%(delim)s)|^)\W*%(quote)s[^%(delim)s\n]*%(quote)s[^%(delim)s\n]*%(quote)s\W*((%(delim)s)|$)"
                               % {"delim": re.escape(delim), "quote": quotechar}, re.MULTILINE)
        doublequote = bool(dq_regexp.search(data))
        return (quotechar, doublequote, delim, skipinitialspace)

    def _guess_delimiter(self, data, delimiters):
        data = list(filter(None, data.split("\n")))
        ascii = [chr(c) for c in range(127)]
        chunkLength = min(10, len(data))
        iteration = 0
        charFrequency = {}
        modes = {}
        delims = {}
        start, end = 0, chunkLength
        while start < len(data):
            iteration += 1
            for line in data[start:end]:
                for char in ascii:
                    metaFrequency = charFrequency.get(char, {})
                    freq = line.count(char)
                    metaFrequency[freq] = metaFrequency.get(freq, 0) + 1
                    charFrequency[char] = metaFrequency
            for char in charFrequency.keys():
                items = list(charFrequency[char].items())
                if len(items) == 1 and items[0][0] == 0:
                    continue
                if len(items) > 1:
                    modes[char] = max(items, key=lambda x: x[1])
                    items.remove(modes[char])
                    modes[char] = (modes[char][0], modes[char][1] - sum(item[1] for item in items))
                else:
                    modes[char] = items[0]
            modeList = modes.items()
            total = float(min(chunkLength * iteration, len(data)))
            consistency = 1.0
            threshold = 0.9
            while len(delims) == 0 and consistency >= threshold:
                for k, v in modeList:
                    if v[0] > 0 and v[1] > 0:
                        if ((v[1] / total) >= consistency and (delimiters is None or k in delimiters)):
                            delims[k] = v
                consistency -= 0.01
            if len(delims) == 1:
                delim = list(delims.keys())[0]
                skipinitialspace = (data[0].count(delim) == data[0].count("%c " % delim))
                return (delim, skipinitialspace)
            start = end
            end += chunkLength
        if not delims:
            return ("", 0)
        if len(delims) > 1:
            for d in self.preferred:
                if d in delims.keys():
                    skipinitialspace = (data[0].count(d) == data[0].count("%c " % d))
                    return (d, skipinitialspace)
        items = [(v, k) for (k, v) in delims.items()]
        items.sort()
        delim = items[-1][1]
        skipinitialspace = (data[0].count(delim) == data[0].count("%c " % delim))
        return (delim, skipinitialspace)

    def has_header(self, sample):
        rdr = reader(StringIO(sample), self.sniff(sample))
        header = next(rdr)
        columns = len(header)
        columnTypes = {}
        for i in range(columns):
            columnTypes[i] = None
        checked = 0
        for row in rdr:
            if checked > 20:
                break
            checked += 1
            if len(row) != columns:
                continue
            for col in list(columnTypes.keys()):
                thisType = complex
                try:
                    thisType(row[col])
                except (ValueError, OverflowError):
                    thisType = len(row[col])
                if thisType != columnTypes[col]:
                    if columnTypes[col] is None:
                        columnTypes[col] = thisType
                    else:
                        del columnTypes[col]
        hasHeader = 0
        for col, colType in columnTypes.items():
            if isinstance(colType, int):
                if len(header[col]) != colType:
                    hasHeader += 1
                else:
                    hasHeader -= 1
            else:
                try:
                    colType(header[col])
                except (ValueError, TypeError):
                    hasHeader += 1
                else:
                    hasHeader -= 1
        return hasHeader > 0
