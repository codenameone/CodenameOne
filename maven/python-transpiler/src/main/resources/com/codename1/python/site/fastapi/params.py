# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Parameter declarations: Query, Path, Header, Cookie, Body, Form, Depends."""


class _Undef:
    def __repr__(self):
        return "PydanticUndefined"


Undefined = _Undef()


class Param:
    location = "query"

    def __init__(self, default=Undefined, *, alias=None, title=None, description=None, gt=None,
                 ge=None, lt=None, le=None, min_length=None, max_length=None, pattern=None,
                 regex=None, multiple_of=None, example=None, examples=None, deprecated=None,
                 include_in_schema=True, default_factory=None, **kw):
        if default is Ellipsis:
            default = Undefined
        self.default = default
        self.default_factory = default_factory
        self.alias = alias
        self.title = title
        self.description = description
        self.gt = gt
        self.ge = ge
        self.lt = lt
        self.le = le
        self.min_length = min_length
        self.max_length = max_length
        self.pattern = pattern if pattern is not None else regex
        self.multiple_of = multiple_of
        self.example = example
        self.examples = examples
        self.deprecated = deprecated
        self.include_in_schema = include_in_schema

    def constraints(self):
        out = {}
        for k in ("gt", "ge", "lt", "le", "min_length", "max_length", "pattern", "multiple_of"):
            v = getattr(self, k)
            if v is not None:
                out[k] = v
        return out

    def __repr__(self):
        return "%s(%r)" % (type(self).__name__, self.default)


class Path(Param):
    location = "path"

    def __init__(self, default=Undefined, **kw):
        Param.__init__(self, ... if default is Undefined else default, **kw)
        self.default = Undefined


class Query(Param):
    location = "query"


class Header(Param):
    location = "header"

    def __init__(self, default=Undefined, *, convert_underscores=True, **kw):
        Param.__init__(self, default, **kw)
        self.convert_underscores = convert_underscores


class Cookie(Param):
    location = "cookie"


class Body(Param):
    location = "body"

    def __init__(self, default=Undefined, *, embed=None, media_type="application/json", **kw):
        Param.__init__(self, default, **kw)
        self.embed = embed
        self.media_type = media_type


class Form(Body):
    def __init__(self, default=Undefined, **kw):
        Body.__init__(self, default, media_type="application/x-www-form-urlencoded", **kw)


class File(Form):
    pass


class Depends:
    def __init__(self, dependency=None, *, use_cache=True):
        self.dependency = dependency
        self.use_cache = use_cache

    def __repr__(self):
        return "Depends(%r)" % (getattr(self.dependency, "__name__", self.dependency),)


Security = Depends
