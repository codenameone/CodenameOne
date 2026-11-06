# Developer guide, command-line chapter. Transpiled and compiled with the
# demos so the guide's code cannot drift from what builds.

# tag::cli-python[]
import argparse
import sys


def main(argv=None):
    parser = argparse.ArgumentParser(description="Prints a greeting.")
    parser.add_argument("name", help="who to greet")
    parser.add_argument("-c", "--count", type=int, default=1, help="how many times")
    parser.add_argument("-l", "--loud", action="store_true", help="shout it")
    parser.add_argument("--version", action="version", version="%(prog)s 1.0")
    args = parser.parse_args(argv)
    greeting = "Hello, %s" % args.name
    for _ in range(args.count):
        print(greeting.upper() if args.loud else greeting)
    return 0


if __name__ == "__main__":
    sys.exit(main())
# end::cli-python[]
