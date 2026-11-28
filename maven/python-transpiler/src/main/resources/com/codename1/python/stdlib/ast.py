# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""The node classes of Python's syntax tree, without a parser.

A translated program carries no Python parser: the source was parsed when the
application was built. parse() therefore raises SyntaxError, which is what a
caller that sniffs whether some text is Python (puremagic's scanner) already
treats as "it is not"; the node classes exist so that isinstance checks and
annotations that name them still work.
"""


class AST:
    _fields = ()
    _attributes = ("lineno", "col_offset", "end_lineno", "end_col_offset")

    def __init__(self, *args, **kwargs):
        for name, value in zip(self._fields, args):
            setattr(self, name, value)
        for name, value in kwargs.items():
            setattr(self, name, value)


def _node(name, fields=(), base=AST):
    return type(name, (base,), {"_fields": fields})


mod = _node("mod")
stmt = _node("stmt")
expr = _node("expr")
expr_context = _node("expr_context")
Module = _node("Module", ("body", "type_ignores"), mod)
Expression = _node("Expression", ("body",), mod)
Interactive = _node("Interactive", ("body",), mod)
FunctionDef = _node("FunctionDef", ("name", "args", "body", "decorator_list", "returns"), stmt)
AsyncFunctionDef = _node("AsyncFunctionDef", ("name", "args", "body", "decorator_list", "returns"), stmt)
ClassDef = _node("ClassDef", ("name", "bases", "keywords", "body", "decorator_list"), stmt)
Return = _node("Return", ("value",), stmt)
Delete = _node("Delete", ("targets",), stmt)
Assign = _node("Assign", ("targets", "value"), stmt)
AugAssign = _node("AugAssign", ("target", "op", "value"), stmt)
AnnAssign = _node("AnnAssign", ("target", "annotation", "value", "simple"), stmt)
For = _node("For", ("target", "iter", "body", "orelse"), stmt)
AsyncFor = _node("AsyncFor", ("target", "iter", "body", "orelse"), stmt)
While = _node("While", ("test", "body", "orelse"), stmt)
If = _node("If", ("test", "body", "orelse"), stmt)
With = _node("With", ("items", "body"), stmt)
AsyncWith = _node("AsyncWith", ("items", "body"), stmt)
Match = _node("Match", ("subject", "cases"), stmt)
Raise = _node("Raise", ("exc", "cause"), stmt)
Try = _node("Try", ("body", "handlers", "orelse", "finalbody"), stmt)
TryStar = _node("TryStar", ("body", "handlers", "orelse", "finalbody"), stmt)
Assert = _node("Assert", ("test", "msg"), stmt)
Import = _node("Import", ("names",), stmt)
ImportFrom = _node("ImportFrom", ("module", "names", "level"), stmt)
Global = _node("Global", ("names",), stmt)
Nonlocal = _node("Nonlocal", ("names",), stmt)
Expr = _node("Expr", ("value",), stmt)
Pass = _node("Pass", (), stmt)
Break = _node("Break", (), stmt)
Continue = _node("Continue", (), stmt)
BoolOp = _node("BoolOp", ("op", "values"), expr)
NamedExpr = _node("NamedExpr", ("target", "value"), expr)
BinOp = _node("BinOp", ("left", "op", "right"), expr)
UnaryOp = _node("UnaryOp", ("op", "operand"), expr)
Lambda = _node("Lambda", ("args", "body"), expr)
IfExp = _node("IfExp", ("test", "body", "orelse"), expr)
Dict = _node("Dict", ("keys", "values"), expr)
Set = _node("Set", ("elts",), expr)
ListComp = _node("ListComp", ("elt", "generators"), expr)
SetComp = _node("SetComp", ("elt", "generators"), expr)
DictComp = _node("DictComp", ("key", "value", "generators"), expr)
GeneratorExp = _node("GeneratorExp", ("elt", "generators"), expr)
Await = _node("Await", ("value",), expr)
Yield = _node("Yield", ("value",), expr)
YieldFrom = _node("YieldFrom", ("value",), expr)
Compare = _node("Compare", ("left", "ops", "comparators"), expr)
Call = _node("Call", ("func", "args", "keywords"), expr)
FormattedValue = _node("FormattedValue", ("value", "conversion", "format_spec"), expr)
JoinedStr = _node("JoinedStr", ("values",), expr)
Constant = _node("Constant", ("value", "kind"), expr)
Attribute = _node("Attribute", ("value", "attr", "ctx"), expr)
Subscript = _node("Subscript", ("value", "slice", "ctx"), expr)
Starred = _node("Starred", ("value", "ctx"), expr)
Name = _node("Name", ("id", "ctx"), expr)
List = _node("List", ("elts", "ctx"), expr)
Tuple = _node("Tuple", ("elts", "ctx"), expr)
Slice = _node("Slice", ("lower", "upper", "step"), expr)
Load = _node("Load", (), expr_context)
Store = _node("Store", (), expr_context)
Del = _node("Del", (), expr_context)
arguments = _node("arguments", ("posonlyargs", "args", "vararg", "kwonlyargs", "kw_defaults", "kwarg",
                                "defaults"))
arg = _node("arg", ("arg", "annotation"))
keyword = _node("keyword", ("arg", "value"))
alias = _node("alias", ("name", "asname"))


def parse(source, filename="<unknown>", mode="exec", **kwargs):
    raise SyntaxError("ast.parse is not available in a translated program: the source it would parse was not"
                      " compiled with the application")


def iter_fields(node):
    for field in node._fields:
        try:
            yield field, getattr(node, field)
        except AttributeError:
            pass


def iter_child_nodes(node):
    for name, field in iter_fields(node):
        if isinstance(field, AST):
            yield field
        elif isinstance(field, list):
            for item in field:
                if isinstance(item, AST):
                    yield item


def walk(node):
    from collections import deque
    todo = deque([node])
    while todo:
        node = todo.popleft()
        todo.extend(iter_child_nodes(node))
        yield node


def literal_eval(node_or_string):
    raise ValueError("ast.literal_eval is not available in a translated program")
