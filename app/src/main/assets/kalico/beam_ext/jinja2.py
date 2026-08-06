class Undefined(Exception): pass
class Environment:
    def __init__(self, *args, **kwargs): pass
    def from_string(self, source): return Template(source)
    def get_template(self, name): return Template(name)
    def join_path(self, template, parent): return template
class Template:
    def __init__(self, source): self.source = source
    def render(self, *args, **kwargs): return ""
