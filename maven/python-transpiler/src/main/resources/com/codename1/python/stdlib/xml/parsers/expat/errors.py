# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""xml.parsers.expat.errors: expat's error codes and messages."""

__all__ = ["codes", "messages"]

_TABLE = (
    "out of memory", "syntax error", "no element found", "not well-formed (invalid token)",
    "unclosed token", "partial character", "mismatched tag", "duplicate attribute",
    "junk after document element", "illegal parameter entity reference", "undefined entity",
    "recursive entity reference", "asynchronous entity", "reference to invalid character number",
    "reference to binary entity", "reference to external entity in attribute",
    "XML or text declaration not at start of entity", "unknown encoding",
    "encoding specified in XML declaration is incorrect", "unclosed CDATA section",
    "error in processing external entity reference", "document is not standalone",
    "unexpected parser state - please send a bug report", "entity declared in parameter entity",
    "requested feature requires XML_DTD support in Expat", "cannot change setting once parsing has begun",
    "unbound prefix", "must not undeclare prefix", "incomplete markup in parameter entity",
    "XML declaration not well-formed", "text declaration not well-formed", "illegal character(s) in public id",
    "parser suspended", "parser not suspended", "parsing aborted", "parsing finished",
    "cannot suspend in external parameter entity",
    "reserved prefix (xml) must not be undeclared or bound to another namespace name",
    "reserved prefix (xmlns) must not be declared or undeclared",
    "prefix must not be bound to one of the reserved namespace names", "invalid argument",
    "a successful prior call to function XML_GetBuffer is required",
    "limit on input amplification factor (from DTD and entities) breached", "parser not started",
)

XML_ERROR_NO_MEMORY = _TABLE[0]
XML_ERROR_SYNTAX = _TABLE[1]
XML_ERROR_NO_ELEMENTS = _TABLE[2]
XML_ERROR_INVALID_TOKEN = _TABLE[3]
XML_ERROR_UNCLOSED_TOKEN = _TABLE[4]
XML_ERROR_PARTIAL_CHAR = _TABLE[5]
XML_ERROR_TAG_MISMATCH = _TABLE[6]
XML_ERROR_DUPLICATE_ATTRIBUTE = _TABLE[7]
XML_ERROR_JUNK_AFTER_DOC_ELEMENT = _TABLE[8]
XML_ERROR_PARAM_ENTITY_REF = _TABLE[9]
XML_ERROR_UNDEFINED_ENTITY = _TABLE[10]
XML_ERROR_RECURSIVE_ENTITY_REF = _TABLE[11]
XML_ERROR_ASYNC_ENTITY = _TABLE[12]
XML_ERROR_BAD_CHAR_REF = _TABLE[13]
XML_ERROR_BINARY_ENTITY_REF = _TABLE[14]
XML_ERROR_ATTRIBUTE_EXTERNAL_ENTITY_REF = _TABLE[15]
XML_ERROR_MISPLACED_XML_PI = _TABLE[16]
XML_ERROR_UNKNOWN_ENCODING = _TABLE[17]
XML_ERROR_INCORRECT_ENCODING = _TABLE[18]
XML_ERROR_UNCLOSED_CDATA_SECTION = _TABLE[19]
XML_ERROR_EXTERNAL_ENTITY_HANDLING = _TABLE[20]
XML_ERROR_NOT_STANDALONE = _TABLE[21]
XML_ERROR_UNEXPECTED_STATE = _TABLE[22]
XML_ERROR_ENTITY_DECLARED_IN_PE = _TABLE[23]
XML_ERROR_FEATURE_REQUIRES_XML_DTD = _TABLE[24]
XML_ERROR_CANT_CHANGE_FEATURE_ONCE_PARSING = _TABLE[25]
XML_ERROR_UNBOUND_PREFIX = _TABLE[26]
XML_ERROR_UNDECLARING_PREFIX = _TABLE[27]
XML_ERROR_INCOMPLETE_PE = _TABLE[28]
XML_ERROR_XML_DECL = _TABLE[29]
XML_ERROR_TEXT_DECL = _TABLE[30]
XML_ERROR_PUBLICID = _TABLE[31]
XML_ERROR_SUSPENDED = _TABLE[32]
XML_ERROR_NOT_SUSPENDED = _TABLE[33]
XML_ERROR_ABORTED = _TABLE[34]
XML_ERROR_FINISHED = _TABLE[35]
XML_ERROR_SUSPEND_PE = _TABLE[36]
XML_ERROR_RESERVED_PREFIX_XML = _TABLE[37]
XML_ERROR_RESERVED_PREFIX_XMLNS = _TABLE[38]
XML_ERROR_RESERVED_NAMESPACE_URI = _TABLE[39]
XML_ERROR_INVALID_ARGUMENT = _TABLE[40]
XML_ERROR_NO_BUFFER = _TABLE[41]
XML_ERROR_AMPLIFICATION_LIMIT_BREACH = _TABLE[42]
XML_ERROR_NOT_STARTED = _TABLE[43]

codes = {}
messages = {}
for _i, _m in enumerate(_TABLE):
    codes[_m] = _i + 1
    messages[_i + 1] = _m
