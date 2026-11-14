# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""llm's own test suite, run transpiled. The modules are every one that
tests the library; the ones that drive the click command line through
CliRunner are left out, as the command line is not part of an application.
run_heroes.py runs the same modules under CPython's pytest and compares."""
import pytest

import tests.conftest  # noqa: F401 - the fixtures
import tests.test_async
import tests.test_async_parity
import tests.test_chain_callbacks
import tests.test_embed
import tests.test_encode_decode
import tests.test_migrate
import tests.test_openai_messages
import tests.test_options_parameter
import tests.test_parts
import tests.test_pause_resume
import tests.test_serialization
import tests.test_utils

MODULES = [tests.test_parts, tests.test_serialization, tests.test_utils, tests.test_embed, tests.test_migrate,
           tests.test_pause_resume, tests.test_options_parameter, tests.test_async_parity, tests.test_async,
           tests.test_chain_callbacks, tests.test_encode_decode, tests.test_openai_messages]

for m in MODULES:
    print("## " + m.__name__)
    pytest.run_module(m)
