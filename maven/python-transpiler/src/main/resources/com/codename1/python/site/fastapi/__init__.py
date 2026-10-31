# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""FastAPI over a pure dispatch core: no sockets, no ASGI.

``app.dispatch(method, path, query, headers, body)`` maps one request to
``(status, headers, body)``; ``TestClient`` and the native server both go
through it. Written from FastAPI's public documentation and behaviour.
"""
from starlette.background import BackgroundTasks
from starlette.requests import Request
from starlette.responses import Response
from starlette.websockets import WebSocket, WebSocketDisconnect

from fastapi import status
from fastapi.applications import FastAPI
from fastapi.exceptions import HTTPException, WebSocketException
from fastapi.params import Body, Cookie, Depends, File, Form, Header, Path, Query, Security
from fastapi.routing import APIRouter

__all__ = ["FastAPI", "APIRouter", "Depends", "Query", "Path", "Body", "Header", "Cookie", "Form",
           "File", "Security", "HTTPException", "Request", "Response", "BackgroundTasks", "status",
           "WebSocket", "WebSocketDisconnect", "WebSocketException"]
