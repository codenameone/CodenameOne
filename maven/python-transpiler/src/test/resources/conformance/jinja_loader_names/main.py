from jinja2 import Environment, FileSystemLoader

env = Environment(loader=FileSystemLoader("templates"))
print(env.get_template("page.html").render(user={"name": "x"}))
print(env.get_template("missing.html").render())
