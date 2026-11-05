# argparse: parsing, errors, help and usage text, subcommands, groups.
import argparse
import sys

sys.stderr = sys.stdout


def run(parser, args):
    try:
        ns = parser.parse_args(args)
        print("ok", ns)
    except SystemExit as e:
        print("exit", e.code)


p = argparse.ArgumentParser(prog="greet", description="Prints a greeting.", epilog="See the guide.")
p.add_argument("name", help="who to greet")
p.add_argument("-c", "--count", type=int, default=1, help="how many times (default: %(default)s)")
p.add_argument("-l", "--loud", action="store_true", help="shout it")
p.add_argument("--color", action=argparse.BooleanOptionalAction, default=True, help="colour output")
p.add_argument("--mode", choices=["fast", "slow"], default="fast")
p.add_argument("-v", "--verbose", action="count", default=0)
p.add_argument("--tag", action="append", dest="tags")
p.add_argument("--version", action="version", version="%(prog)s 1.0")
run(p, ["Ada"])
run(p, ["--count", "2", "-lvv", "Ada"])
run(p, ["-c2", "--count=3", "Ada", "--no-color"])
run(p, ["-lc", "4", "Ada", "--tag", "a", "--tag=b", "--mode", "slow"])
run(p, ["--cou", "5", "--", "-Ada"])
run(p, [])
run(p, ["-c", "x", "Ada"])
run(p, ["--mode", "medium", "Ada"])
run(p, ["Ada", "Bob"])
run(p, ["--unknown", "Ada"])
run(p, ["Ada", "--count"])
run(p, ["--c", "1", "Ada"])
run(p, ["--version"])
run(p, ["-h"])
print(p.parse_known_args(["Ada", "--extra", "x"]))

n = argparse.ArgumentParser(prog="nums")
n.add_argument("values", nargs="+", type=float)
n.add_argument("--scale", nargs="?", const=10, type=int, default=1)
n.add_argument("--pair", nargs=2, metavar=("A", "B"))
n.add_argument("rest", nargs="*")
run(n, ["1", "-2", "3.5"])
run(n, ["1", "--scale", "2"])
run(n, ["1", "--scale"])
run(n, ["--pair", "x", "y", "4"])
run(n, ["--pair", "x"])
run(n, [])
n.print_help()

g = argparse.ArgumentParser(prog="grp")
ex = g.add_mutually_exclusive_group(required=True)
ex.add_argument("-a", action="store_true")
ex.add_argument("-b", action="store_true")
opts = g.add_argument_group("output", "Where the result goes.")
opts.add_argument("-o", "--out", metavar="FILE", help="write here")
run(g, ["-a"])
run(g, ["-a", "-b"])
run(g, [])
g.print_help()

top = argparse.ArgumentParser(prog="notes")
top.add_argument("-f", "--file", default="notes.txt")
sub = top.add_subparsers(dest="command", required=True, help="what to do")
add = sub.add_parser("add", help="add a note")
add.add_argument("text", nargs="+")
add.add_argument("--tag", default="")
add.set_defaults(handler="adder")
show = sub.add_parser("show", aliases=["ls"], help="list the notes")
show.add_argument("--limit", type=int, default=10)
run(top, ["add", "buy", "milk", "--tag", "home"])
run(top, ["-f", "w.txt", "ls", "--limit", "3"])
run(top, ["rm"])
run(top, [])
run(top, ["add"])
run(top, ["add", "--help"])
top.print_help()

d = argparse.ArgumentParser(prog="defaults", formatter_class=argparse.ArgumentDefaultsHelpFormatter)
d.add_argument("--size", type=int, default=3, help="the size")
d.add_argument("--name", default="x", help="the name")
d.add_argument("--really-long-option-name-here", metavar="SOME_LONG_VALUE", help="a long option whose help needs to wrap onto more than one line of the help output")
d.add_argument("files", nargs="*", help="input files")
d.print_help()
print(d.parse_args(["--size", "4", "a", "b"]))


def positive(s):
    v = int(s)
    if v <= 0:
        raise argparse.ArgumentTypeError("%r is not positive" % s)
    return v


t = argparse.ArgumentParser(prog="typed", exit_on_error=False)
t.add_argument("--n", type=positive)
try:
    t.parse_args(["--n", "-3"])
except argparse.ArgumentError as e:
    print("error:", e)
print(t.parse_args(["--n", "7"]))

w = argparse.ArgumentParser(prog="wide")
for i in range(12):
    w.add_argument("--option-number-%d" % i, metavar="V")
w.add_argument("source")
w.add_argument("destination")
w.print_usage()
ns = argparse.Namespace(a=1, b="x")
print(ns, "a" in ns, ns == argparse.Namespace(a=1, b="x"))
