# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""runpy: running a module as a script means executing its code again,
under __main__; a translated module is compiled to run once, when it is
imported, so both functions raise NotImplementedError."""


def run_module(mod_name, init_globals=None, run_name=None, alter_sys=False):
    raise NotImplementedError("runpy.run_module(%r): a translated program cannot re-run a module as a script"
                              % (mod_name,))


def run_path(path_name, init_globals=None, run_name=None):
    raise NotImplementedError("runpy.run_path(%r): a translated program cannot run a file" % (path_name,))
