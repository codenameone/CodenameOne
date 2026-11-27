def guard(fn):
    try:
        return fn()
    except ValueError as e:
        return "err " + str(e)


def invoke(f, args, info):
    return f(*args) + info


def validate(cls: str, raw: int, hooks: list, plain, info: str):
    v = raw
    for d in hooks:
        v = guard(lambda d=d, v=v: invoke(d, (cls, v), info))
    if plain is not None:
        v = guard(lambda: invoke(plain, (cls, v), info))
    return v


def bad(c, v):
    raise ValueError(c)


print(validate("k", 1, [lambda c, v: str(v) + c, lambda c, v: v + "!"], None, "."))
print(validate("k", 2, [], lambda c, v: c * v, "?"))
print(validate("k", 3, [bad], lambda c, v: c + v, "?"))
