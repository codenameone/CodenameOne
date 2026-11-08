# Developer guide, command-line chapter: cn1console. Transpiled and compiled
# with the demos so the guide's code cannot drift from what builds.

# tag::cli-python-console[]
import cn1console as console


def main():
    console.print_table([["api", 3], ["worker", 2]],
                        headers=["Service", "Replicas"], align={1: "right"})
    for name in console.progress(["a.txt", "b.txt", "c.txt"], label="upload"):
        pass  # upload the file
    with console.spinner("waiting for health checks"):
        pass  # poll until healthy
    env = console.select("Environment", ["staging", "production"])
    regions = console.checkbox("Regions", ["us-east", "eu-west", "ap-south"])
    if console.confirm("Deploy to %s?" % env):
        console.echo("deploying to", ", ".join(regions), style="bold")
# end::cli-python-console[]


if __name__ == "__main__":
    main()
