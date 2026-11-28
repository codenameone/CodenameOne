import argparse

NAMES = ["alpha", "beta"]


def help_only():
    # Only formats help: a computed option is never stored, so it builds.
    p = argparse.ArgumentParser(prog="h")
    g = p.add_argument_group("g")
    for n in NAMES:
        g.add_argument("--" + n)
    return p.format_help()


def parsed(argv):
    p = argparse.ArgumentParser(prog="x")
    p.add_argument("--fixed")
    for n in NAMES:
        p.add_argument("--" + n)
    opts = {"dest": "late"}
    p.add_argument("--late", **opts)
    return p.parse_args(argv)


def escapes():
    p = argparse.ArgumentParser(prog="e")
    p.add_argument(NAMES[0])
    return p


print(help_only())
print(parsed([]))
print(escapes())
