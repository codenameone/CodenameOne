# ParparVM JavaScript VM Protocol

Version: 1

This file documents the worker boundary emitted by the `javascript` backend.

Messages from host to worker:
- `start`: boot the translated application and invoke the translated `main`.
- `protocol-info`: request the protocol/version handshake without starting the app.
- `event`: inject a generic host event into the VM event queue.
- `ui-event`: inject a UI-oriented host event into the VM event queue.
- `timer-wake`: wake the scheduler after a host-driven timer tick.
- `host-callback`: deliver the completion or failure of a pending host call.

Messages from worker to host:
- `protocol`: reply to `protocol-info` with the explicit protocol version and message names.
- `host-call`: request a host-provided native operation or callback.
- `log`: emit a VM/runtime log line.
- `result`: report normal translated application completion.
- `error`: report fatal translated application or protocol failure.

Host hook native categories:
- Runtime-implemented natives: handled entirely inside `parparvm_runtime.js`.
- Host-hook natives: compiled to `host-call` messages and completed via `host-callback`.
- Unsupported natives: fail deterministically with a backend-specific unsupported message.
- Uncategorized natives: treated as backend bugs and should fail tests.
