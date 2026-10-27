# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Clean-room subset of uvicorn: run an app through the platform server."""


def run(app, host="127.0.0.1", port=8000, **kw):
    import _cn1server
    _cn1server.serve(app.dispatch, host, port)
