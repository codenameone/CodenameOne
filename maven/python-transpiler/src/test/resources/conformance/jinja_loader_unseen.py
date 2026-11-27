from jinja2 import Environment, FileSystemLoader

env = Environment(loader=FileSystemLoader("no_such_templates"))
print(env.get_template("page.html").render(user={"name": "x"}))
