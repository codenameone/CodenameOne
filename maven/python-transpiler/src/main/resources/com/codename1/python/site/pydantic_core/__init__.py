# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""pydantic_core: the names code imports from it beside pydantic.

The real package is a Rust extension that compiles a model into a core
schema. Here pydantic validates in Python and has no core schema, so only
the sentinel and the error types exist; importing CoreSchema or
core_schema raises ImportError, which is how libraries that build their own
schemas (the openai SDK) detect a pydantic without one.
"""

from pydantic import PydanticUndefined, ValidationError

PydanticUndefinedType = type(PydanticUndefined)


class PydanticSerializationError(ValueError):
    pass


class PydanticCustomError(ValueError):
    def __init__(self, error_type, message_template, context=None):
        ValueError.__init__(self, message_template.format(**(context or {})))
        self.type = error_type
        self.message_template = message_template
        self.context = context


__version__ = "2.41.5"
