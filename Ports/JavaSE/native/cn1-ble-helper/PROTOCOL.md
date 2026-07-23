# cn1-ble-helper JSON protocol (version 1)

Communication between the JavaSE port's
`com.codename1.impl.javase.bluetooth.NativeBleBackend` and this Rust helper
executable (a [btleplug](https://github.com/deviceplug/btleplug) bridge:
CoreBluetooth on macOS, BlueZ over D-Bus on Linux, WinRT on Windows).

One JSON object per line, UTF-8: commands on the helper's **stdin**, events
on its **stdout**. Helper diagnostics go to stderr and are pumped into the
JVM's `System.err` with a `[Cn1BleHelper]` prefix.

## Identifiers

btleplug's `PeripheralId` is OS-specific (a UUID on macOS, a `BDAddr` on
Linux, a 64-bit address on Windows); the helper renders it as a lowercase
string and both sides treat that opaque string as the wire `address`.
Service/characteristic/descriptor UUIDs are normalized to the lowercase
dashed 128-bit form (`0000180a-0000-1000-8000-00805f9b34fb`) in every event;
commands may also send the bare 16-/32-bit assigned-number form. Binary
values travel as standard base64.

## Request/response model

Every command carries a numeric `id` (positive, monotonic). The helper
answers each command with **exactly one terminal event** echoing that value
as `requestId` — either the success event listed below or an `error` event.
Events without a `requestId` are unsolicited (spontaneous state changes,
scan sightings, notifications, remote disconnects).

## Commands (Java → helper)

```
{"cmd":"scanStart","id":1,"services":["<uuid>",...]}      → scanStarted
{"cmd":"scanStop","id":2}                                 → scanStopped
{"cmd":"connect","id":3,"address":"..."}                  → connected
{"cmd":"disconnect","id":4,"address":"..."}               → disconnected
{"cmd":"discover","id":5,"address":"..."}                 → discovered
{"cmd":"read","id":6,"address":"...","service":"...","characteristic":"..."}
                                                          → readResult
{"cmd":"write","id":7,"address":"...","service":"...","characteristic":"...",
 "value":"<base64>","noResponse":false}                   → writeResult
{"cmd":"subscribe","id":8,"address":"...","service":"...","characteristic":"..."}
                                                          → subscribed
{"cmd":"unsubscribe","id":9,"address":"...","service":"...","characteristic":"..."}
                                                          → unsubscribed
{"cmd":"readDescriptor","id":10,"address":"...","service":"...",
 "characteristic":"...","descriptor":"..."}               → descriptorReadResult
{"cmd":"writeDescriptor","id":11,"address":"...","service":"...",
 "characteristic":"...","descriptor":"...","value":"<base64>"}
                                                          → descriptorWriteResult
{"cmd":"readRssi","id":12,"address":"..."}                → rssiResult
{"cmd":"shutdown"}                                        → helper exits 0
```

`scanStart.services` is optional; when present only peripherals advertising
one of the listed service UUIDs are reported (btleplug `ScanFilter`).
`write.noResponse:true` selects write-without-response.

## Terminal success events (helper → Java)

```
{"event":"scanStarted","requestId":1}
{"event":"scanStopped","requestId":2}
{"event":"connected","requestId":3,"address":"...","name":"..."}
{"event":"disconnected","requestId":4,"address":"..."}
{"event":"discovered","requestId":5,"address":"...","name":"...",
 "services":[{"uuid":"...","primary":true,
   "characteristics":[{"uuid":"...",
     "properties":["broadcast"|"read"|"writeWithoutResponse"|"write"|
                   "notify"|"indicate"|"signedWrite"|"extendedProps",...],
     "descriptors":[{"uuid":"..."},...]}]}]}
{"event":"readResult","requestId":6,"address":"...","service":"...",
 "characteristic":"...","value":"<base64>"}
{"event":"writeResult","requestId":7,"address":"...","service":"...",
 "characteristic":"..."}
{"event":"subscribed","requestId":8,"address":"...","service":"...",
 "characteristic":"..."}
{"event":"unsubscribed","requestId":9,"address":"...","service":"...",
 "characteristic":"..."}
{"event":"descriptorReadResult","requestId":10,"address":"...","service":"...",
 "characteristic":"...","descriptor":"...","value":"<base64>"}
{"event":"descriptorWriteResult","requestId":11,"address":"...","service":"...",
 "characteristic":"...","descriptor":"..."}
{"event":"rssiResult","requestId":12,"address":"...","rssi":-58,"source":"lastSeen"}
```

`rssiResult.source` is `"lastSeen"` — btleplug has no live RSSI read, so the
value is the most recent advertisement sighting (also declared in the
`capabilities.rssi` field).

## Error event

```
{"event":"error","requestId":6,"command":"read","address":"...",
 "code":"notConnected","message":"human-readable detail"}
```

`code` is one of:

| code                    | meaning                                            | Java maps to `BluetoothError` |
|-------------------------|----------------------------------------------------|-------------------------------|
| `notSupported`          | feature absent on this platform/helper build       | `NOT_SUPPORTED`               |
| `unauthorized`          | OS denied Bluetooth permission                     | `UNAUTHORIZED`                |
| `poweredOff`            | adapter is off                                     | `POWERED_OFF`                 |
| `scanFailed`            | the OS refused/aborted the scan                    | `SCAN_FAILED`                 |
| `connectFailed`         | connect attempt failed                             | `CONNECTION_FAILED`           |
| `notConnected`          | GATT op on a disconnected peripheral               | `NOT_CONNECTED`               |
| `unknownPeripheral`     | address never sighted by a scan                    | `CONNECTION_FAILED`           |
| `unknownCharacteristic` | uuid not in the discovered database                | `GATT_ERROR`                  |
| `unknownDescriptor`     | uuid not in the discovered database                | `GATT_ERROR`                  |
| `timeout`               | the platform stack timed out                       | `TIMEOUT`                     |
| `badRequest`            | malformed command (missing args, bad base64)       | `UNKNOWN`                     |
| `ioError`               | any other transport/stack failure                  | `IO_ERROR`                    |

An `error` without a `requestId` is informational (e.g. the central event
stream failed) and is only logged by the Java side.

## Unsolicited events

```
{"event":"capabilities","version":1,"helperVersion":"1.0.0",
 "platform":"macos|linux|windows","descriptors":true,"rssi":"lastSeen",
 "bonding":false}
{"event":"stateChanged","state":"poweredOn|poweredOff|unsupported|unknown"}
{"event":"scanResult","address":"...","name":"...","rssi":-45,"txPower":4,
 "serviceUuids":["..."],"manufacturerData":{"76":"<base64>"},
 "serviceData":{"<uuid>":"<base64>"}}
{"event":"notification","address":"...","service":"...",
 "characteristic":"...","value":"<base64>"}
{"event":"connected","address":"...","name":"..."}
{"event":"disconnected","address":"...","reason":"..."}
```

- `capabilities` is the **first line** the helper ever writes; the Java side
  gates features (descriptor I/O, RSSI semantics, bonding) on it and can
  reject an incompatible `version`.
- `stateChanged` fires at startup with either `poweredOn` (btleplug handed
  us an adapter) or `unsupported` (no adapter / no BlueZ / permission
  denied); afterwards it mirrors btleplug's `CentralEvent::StateUpdate`
  stream on platforms that emit runtime transitions.
- `scanResult` sightings flow only while a scan is active, but the helper
  keeps its peripheral cache fresh in the background so connect-by-address
  works between scans. `txPower` is omitted when not advertised.
- Unsolicited `connected`/`disconnected` (no `requestId`) reflect
  OS-observed link transitions — including remote disconnects.

## Shutdown

The Java side sends `{"cmd":"shutdown"}` and closes stdin; the helper exits
with status 0. If the JVM dies without warning, the helper detects stdin EOF
and exits 0 on its own — it never outlives the JVM.
