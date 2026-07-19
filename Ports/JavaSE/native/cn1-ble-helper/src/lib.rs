// cn1-ble-helper — cross-platform BLE bridge for the Codename One JavaSE
// port's native Bluetooth backend, built as an in-process shared library
// (cdylib `libcn1ble`). See PROTOCOL.md (next to Cargo.toml) for the exact
// event JSON shapes shared with the Java side
// (com.codename1.impl.javase.bluetooth.*).
//
// Architecture
// ------------
// Instead of the old stdin/stdout subprocess, this crate keeps a *single*
// global engine (one process = one adapter):
//   * one multi-threaded tokio runtime that owns all BLE work;
//   * a btleplug adapter + a shared peripheral/notification-pump state;
//   * an internal std MPSC channel of already-serialized outbound event
//     JSON strings. Background tasks (central-event listener, per-peripheral
//     notification pumps, per-command operations) push event JSON onto that
//     channel; the host drains it by calling `poll_event`.
//
// Two ABIs sit over the same engine and internal operations:
//   1. a C ABI (`cn1ble_*`) for the ParparVM Windows/Linux ports;
//   2. a JNI ABI (`Java_com_codename1_impl_javase_bluetooth_JniBleBridge_*`)
//      for the JavaSE port, which just System.load()s this library.
//
// Every operation carries a numeric request id; the engine answers with
// exactly one terminal event echoing it as "requestId" (either the success
// event or an "error" event with a typed "code"). The event JSON is
// byte-for-byte identical to the old stdout protocol.

use base64::engine::general_purpose::STANDARD as B64;
use base64::Engine;
use btleplug::api::{
    Central, CentralEvent, CentralState, CharPropFlags, Manager as _, Peripheral as _,
    ScanFilter, WriteType,
};
use btleplug::platform::{Adapter, Manager, Peripheral, PeripheralId};
use futures::stream::StreamExt;
use serde_json::{json, Map as JsonMap, Value};
use std::collections::HashMap;
use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_int, c_long, c_uchar};
use std::panic::{catch_unwind, AssertUnwindSafe};
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::mpsc::{Receiver, RecvTimeoutError, Sender};
use std::sync::{Arc, Mutex as StdMutex, OnceLock};
use std::time::Duration;
use tokio::sync::Mutex;
use tokio::task::JoinHandle;
use uuid::Uuid;

use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jboolean, jlong, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;

const PROTOCOL_VERSION: u64 = 1;

// ---------------- wire helpers ----------------

/// Single sink for everything the engine emits: already-serialized event
/// JSON strings destined for `poll_event`. Cloneable and handed to every
/// background task.
type EventSink = Sender<String>;

fn emit(sink: &EventSink, event: Value) {
    // Serialize here so the receiver side (poll_event) never touches serde.
    // If the channel is closed the engine has been torn down; dropping a
    // stray event is harmless.
    if let Ok(line) = serde_json::to_string(&event) {
        let _ = sink.send(line);
    }
}

fn emit_event(sink: &EventSink, name: &str, payload: Value) {
    let mut obj = JsonMap::new();
    obj.insert("event".into(), Value::String(name.into()));
    if let Value::Object(extra) = payload {
        for (k, v) in extra {
            obj.insert(k, v);
        }
    }
    emit(sink, Value::Object(obj));
}

/// Success terminal event for the command carrying `request_id`.
fn emit_terminal(sink: &EventSink, name: &str, request_id: u64, payload: Value) {
    let mut obj = JsonMap::new();
    obj.insert("event".into(), Value::String(name.into()));
    obj.insert("requestId".into(), Value::from(request_id));
    if let Value::Object(extra) = payload {
        for (k, v) in extra {
            obj.insert(k, v);
        }
    }
    emit(sink, Value::Object(obj));
}

/// Failure terminal event: `code` is one of the typed error codes from
/// PROTOCOL.md that the Java side maps onto BluetoothError values.
fn emit_error(
    sink: &EventSink,
    request_id: Option<u64>,
    command: &str,
    address: Option<&str>,
    code: &str,
    message: &str,
) {
    let mut obj = JsonMap::new();
    obj.insert("event".into(), Value::String("error".into()));
    if let Some(id) = request_id {
        obj.insert("requestId".into(), Value::from(id));
    }
    obj.insert("command".into(), Value::from(command));
    if let Some(a) = address {
        obj.insert("address".into(), Value::from(a));
    }
    obj.insert("code".into(), Value::from(code));
    obj.insert("message".into(), Value::from(message));
    emit(sink, Value::Object(obj));
}

/// Maps a btleplug failure to a wire error code.
fn error_code(e: &btleplug::Error) -> &'static str {
    use btleplug::Error as E;
    match e {
        E::PermissionDenied => "unauthorized",
        E::DeviceNotFound => "unknownPeripheral",
        E::NotConnected => "notConnected",
        E::NotSupported(_) => "notSupported",
        E::TimedOut(_) => "timeout",
        _ => "ioError",
    }
}

/// btleplug exposes a [`Uuid`]; the wire format is always lowercase dashed
/// 128-bit, which is exactly what `Uuid::to_string` produces.
fn fmt_uuid(u: &Uuid) -> String {
    u.to_string()
}

/// Normalize whatever string the Java side sends — the 128-bit dashed form
/// ("0000180a-…") or the bare 16-/32-bit assigned-number form ("180a") —
/// into a [`Uuid`] btleplug accepts.
fn parse_uuid(raw: &str) -> Option<Uuid> {
    let s = raw.trim();
    if let Ok(u) = Uuid::parse_str(s) {
        return Some(u);
    }
    // 16-/32-bit assigned number: expand using the Bluetooth Base UUID.
    if s.len() <= 8 && s.chars().all(|c| c.is_ascii_hexdigit()) {
        let padded = format!("{:0>8}-0000-1000-8000-00805f9b34fb", s.to_lowercase());
        return Uuid::parse_str(&padded).ok();
    }
    None
}

/// On macOS btleplug's `PeripheralId` is a UUID; on Linux it's a BDAddr; on
/// Windows a `BluetoothAddress`. The `Display` impls all yield canonical
/// strings, which is what we use as the wire-protocol address.
fn id_to_address(id: &PeripheralId) -> String {
    format!("{}", id).to_lowercase()
}

fn host_platform() -> &'static str {
    if cfg!(target_os = "macos") {
        "macos"
    } else if cfg!(target_os = "linux") {
        "linux"
    } else if cfg!(target_os = "windows") {
        "windows"
    } else {
        "unknown"
    }
}

/// The very first event the engine emits: what this build can do, so the
/// Java side gates features without trial-and-error round trips.
fn emit_capabilities(sink: &EventSink) {
    emit_event(
        sink,
        "capabilities",
        json!({
            "version": PROTOCOL_VERSION,
            "helperVersion": env!("CARGO_PKG_VERSION"),
            "platform": host_platform(),
            "descriptors": true,
            // btleplug has no live RSSI read; readRssi answers with the
            // last value seen in an advertisement.
            "rssi": "lastSeen",
            // btleplug is central-only and has no bonding API.
            "bonding": false,
        }),
    );
}

// ---------------- state ----------------

struct State {
    adapter: Adapter,
    peripherals: HashMap<String, Peripheral>,
    /// One notification pump per connected peripheral address.
    notif_pumps: HashMap<String, JoinHandle<()>>,
    scanning: bool,
}

type SharedState = Arc<Mutex<State>>;

async fn lookup_peripheral(state: &SharedState, address: &str) -> Option<Peripheral> {
    state.lock().await.peripherals.get(address).cloned()
}

// ---------------- command dispatch ----------------

async fn handle_command(state: SharedState, sink: EventSink, cmd: Value) {
    let name = cmd
        .get("cmd")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_string();
    let id = cmd.get("id").and_then(|v| v.as_u64());

    match name.as_str() {
        "scanStart" => cmd_scan_start(&state, &sink, id, &cmd).await,
        "scanStop" => cmd_scan_stop(&state, &sink, id).await,
        "connect" => cmd_connect(&state, &sink, id, &cmd).await,
        "disconnect" => cmd_disconnect(&state, &sink, id, &cmd).await,
        "discover" => cmd_discover(&state, &sink, id, &cmd).await,
        "read" => cmd_read(&state, &sink, id, &cmd).await,
        "write" => cmd_write(&state, &sink, id, &cmd).await,
        "subscribe" => cmd_subscribe(&state, &sink, id, &cmd).await,
        "unsubscribe" => cmd_unsubscribe(&state, &sink, id, &cmd).await,
        "readDescriptor" => cmd_descriptor(&state, &sink, id, &cmd, false).await,
        "writeDescriptor" => cmd_descriptor(&state, &sink, id, &cmd, true).await,
        "readRssi" => cmd_read_rssi(&state, &sink, id, &cmd).await,
        // In-process teardown happens via cn1ble_close / JniBleBridge.close,
        // which drop the runtime; a "shutdown" command is a no-op here.
        "shutdown" => {}
        other => {
            eprintln!("[Cn1BleHelper] unknown command '{}'", other);
            emit_error(
                &sink,
                id,
                other,
                None,
                "badRequest",
                &format!("unknown command '{}'", other),
            );
        }
    }
}

/// Extracts the "address" argument or answers with a badRequest error.
fn require_address<'a>(
    sink: &EventSink,
    id: Option<u64>,
    command: &str,
    cmd: &'a Value,
) -> Option<&'a str> {
    match cmd.get("address").and_then(|v| v.as_str()) {
        Some(a) => Some(a),
        None => {
            emit_error(sink, id, command, None, "badRequest", "missing address");
            None
        }
    }
}

/// Extracts the peripheral for the command's "address" or answers with a
/// typed error.
async fn require_peripheral(
    state: &SharedState,
    sink: &EventSink,
    id: Option<u64>,
    command: &str,
    cmd: &Value,
) -> Option<(Peripheral, String)> {
    let address = require_address(sink, id, command, cmd)?.to_string();
    match lookup_peripheral(state, &address).await {
        Some(p) => Some((p, address)),
        None => {
            emit_error(
                sink,
                id,
                command,
                Some(&address),
                "unknownPeripheral",
                "peripheral was never sighted by a scan",
            );
            None
        }
    }
}

async fn cmd_scan_start(state: &SharedState, sink: &EventSink, id: Option<u64>, cmd: &Value) {
    let mut services = Vec::new();
    if let Some(arr) = cmd.get("services").and_then(|v| v.as_array()) {
        for v in arr {
            if let Some(u) = v.as_str().and_then(parse_uuid) {
                services.push(u);
            }
        }
    }
    let mut s = state.lock().await;
    match s.adapter.start_scan(ScanFilter { services }).await {
        Ok(()) => {
            s.scanning = true;
            drop(s);
            if let Some(id) = id {
                emit_terminal(sink, "scanStarted", id, json!({}));
            }
        }
        Err(e) => {
            drop(s);
            emit_error(sink, id, "scanStart", None, "scanFailed", &e.to_string());
        }
    }
}

async fn cmd_scan_stop(state: &SharedState, sink: &EventSink, id: Option<u64>) {
    let mut s = state.lock().await;
    let res = s.adapter.stop_scan().await;
    s.scanning = false;
    drop(s);
    match res {
        Ok(()) => {
            if let Some(id) = id {
                emit_terminal(sink, "scanStopped", id, json!({}));
            }
        }
        Err(e) => emit_error(sink, id, "scanStop", None, error_code(&e), &e.to_string()),
    }
}

async fn cmd_connect(state: &SharedState, sink: &EventSink, id: Option<u64>, cmd: &Value) {
    let Some((p, address)) = require_peripheral(state, sink, id, "connect", cmd).await else {
        return;
    };
    match p.connect().await {
        Ok(()) => {
            ensure_notification_pump(state, sink, &address, &p).await;
            let name = peripheral_name(&p).await;
            if let Some(id) = id {
                emit_terminal(
                    sink,
                    "connected",
                    id,
                    json!({"address": address, "name": name}),
                );
            }
        }
        Err(e) => {
            let code = match e {
                btleplug::Error::TimedOut(_) => "timeout",
                btleplug::Error::PermissionDenied => "unauthorized",
                _ => "connectFailed",
            };
            emit_error(sink, id, "connect", Some(&address), code, &e.to_string());
        }
    }
}

async fn cmd_disconnect(state: &SharedState, sink: &EventSink, id: Option<u64>, cmd: &Value) {
    let Some((p, address)) = require_peripheral(state, sink, id, "disconnect", cmd).await else {
        return;
    };
    match p.disconnect().await {
        Ok(()) => {
            if let Some(id) = id {
                emit_terminal(sink, "disconnected", id, json!({"address": address}));
            }
        }
        Err(e) => emit_error(
            sink,
            id,
            "disconnect",
            Some(&address),
            error_code(&e),
            &e.to_string(),
        ),
    }
}

async fn cmd_discover(state: &SharedState, sink: &EventSink, id: Option<u64>, cmd: &Value) {
    let Some((p, address)) = require_peripheral(state, sink, id, "discover", cmd).await else {
        return;
    };
    if let Err(e) = p.discover_services().await {
        emit_error(
            sink,
            id,
            "discover",
            Some(&address),
            error_code(&e),
            &e.to_string(),
        );
        return;
    }
    let payload = build_discovered_payload(&p, &address).await;
    if let Some(id) = id {
        emit_terminal(sink, "discovered", id, payload);
    }
}

async fn build_discovered_payload(p: &Peripheral, address: &str) -> Value {
    let name = peripheral_name(p).await;
    let mut svc_arr: Vec<Value> = Vec::new();
    for svc in p.services() {
        let mut ch_arr: Vec<Value> = Vec::new();
        for ch in svc.characteristics {
            let mut props = Vec::new();
            let flags = ch.properties;
            if flags.contains(CharPropFlags::BROADCAST) {
                props.push("broadcast");
            }
            if flags.contains(CharPropFlags::READ) {
                props.push("read");
            }
            if flags.contains(CharPropFlags::WRITE_WITHOUT_RESPONSE) {
                props.push("writeWithoutResponse");
            }
            if flags.contains(CharPropFlags::WRITE) {
                props.push("write");
            }
            if flags.contains(CharPropFlags::NOTIFY) {
                props.push("notify");
            }
            if flags.contains(CharPropFlags::INDICATE) {
                props.push("indicate");
            }
            if flags.contains(CharPropFlags::AUTHENTICATED_SIGNED_WRITES) {
                props.push("signedWrite");
            }
            if flags.contains(CharPropFlags::EXTENDED_PROPERTIES) {
                props.push("extendedProps");
            }
            let descriptors: Vec<Value> = ch
                .descriptors
                .iter()
                .map(|d| json!({"uuid": fmt_uuid(&d.uuid)}))
                .collect();
            ch_arr.push(json!({
                "uuid": fmt_uuid(&ch.uuid),
                "properties": props,
                "descriptors": descriptors,
            }));
        }
        svc_arr.push(json!({
            "uuid": fmt_uuid(&svc.uuid),
            "primary": svc.primary,
            "characteristics": ch_arr,
        }));
    }
    json!({
        "address": address,
        "name": name,
        "services": svc_arr,
    })
}

/// Locates the canonical btleplug characteristic for a command's
/// address/service/characteristic triple, answering a typed error when any
/// piece is missing.
async fn resolve_characteristic(
    state: &SharedState,
    sink: &EventSink,
    id: Option<u64>,
    command: &str,
    cmd: &Value,
) -> Option<(Peripheral, btleplug::api::Characteristic, String, String, String)> {
    let (p, address) = require_peripheral(state, sink, id, command, cmd).await?;
    let svc_raw = cmd.get("service").and_then(|v| v.as_str()).unwrap_or("");
    let ch_raw = cmd
        .get("characteristic")
        .and_then(|v| v.as_str())
        .unwrap_or("");
    let (Some(svc_uuid), Some(ch_uuid)) = (parse_uuid(svc_raw), parse_uuid(ch_raw)) else {
        emit_error(
            sink,
            id,
            command,
            Some(&address),
            "badRequest",
            "missing or malformed service/characteristic uuid",
        );
        return None;
    };
    let found = p
        .characteristics()
        .into_iter()
        .find(|c| c.service_uuid == svc_uuid && c.uuid == ch_uuid);
    match found {
        Some(c) => Some((p, c, address, fmt_uuid(&svc_uuid), fmt_uuid(&ch_uuid))),
        None => {
            emit_error(
                sink,
                id,
                command,
                Some(&address),
                "unknownCharacteristic",
                "characteristic not in the discovered database",
            );
            None
        }
    }
}

async fn cmd_read(state: &SharedState, sink: &EventSink, id: Option<u64>, cmd: &Value) {
    let Some((p, ch, address, svc, chr)) =
        resolve_characteristic(state, sink, id, "read", cmd).await
    else {
        return;
    };
    match p.read(&ch).await {
        Ok(value) => {
            if let Some(id) = id {
                emit_terminal(
                    sink,
                    "readResult",
                    id,
                    json!({
                        "address": address,
                        "service": svc,
                        "characteristic": chr,
                        "value": B64.encode(&value),
                    }),
                );
            }
        }
        Err(e) => emit_error(
            sink,
            id,
            "read",
            Some(&address),
            error_code(&e),
            &e.to_string(),
        ),
    }
}

async fn cmd_write(state: &SharedState, sink: &EventSink, id: Option<u64>, cmd: &Value) {
    let Some((p, ch, address, svc, chr)) =
        resolve_characteristic(state, sink, id, "write", cmd).await
    else {
        return;
    };
    let value_b64 = cmd.get("value").and_then(|v| v.as_str()).unwrap_or("");
    let Ok(value) = B64.decode(value_b64) else {
        emit_error(
            sink,
            id,
            "write",
            Some(&address),
            "badRequest",
            "value is not valid base64",
        );
        return;
    };
    let no_response = cmd
        .get("noResponse")
        .and_then(|v| v.as_bool())
        .unwrap_or(false);
    let write_type = if no_response {
        WriteType::WithoutResponse
    } else {
        WriteType::WithResponse
    };
    match p.write(&ch, &value, write_type).await {
        Ok(()) => {
            if let Some(id) = id {
                emit_terminal(
                    sink,
                    "writeResult",
                    id,
                    json!({
                        "address": address,
                        "service": svc,
                        "characteristic": chr,
                    }),
                );
            }
        }
        Err(e) => emit_error(
            sink,
            id,
            "write",
            Some(&address),
            error_code(&e),
            &e.to_string(),
        ),
    }
}

async fn cmd_subscribe(state: &SharedState, sink: &EventSink, id: Option<u64>, cmd: &Value) {
    let Some((p, ch, address, svc, chr)) =
        resolve_characteristic(state, sink, id, "subscribe", cmd).await
    else {
        return;
    };
    match p.subscribe(&ch).await {
        Ok(()) => {
            ensure_notification_pump(state, sink, &address, &p).await;
            if let Some(id) = id {
                emit_terminal(
                    sink,
                    "subscribed",
                    id,
                    json!({
                        "address": address,
                        "service": svc,
                        "characteristic": chr,
                    }),
                );
            }
        }
        Err(e) => emit_error(
            sink,
            id,
            "subscribe",
            Some(&address),
            error_code(&e),
            &e.to_string(),
        ),
    }
}

async fn cmd_unsubscribe(state: &SharedState, sink: &EventSink, id: Option<u64>, cmd: &Value) {
    let Some((p, ch, address, svc, chr)) =
        resolve_characteristic(state, sink, id, "unsubscribe", cmd).await
    else {
        return;
    };
    match p.unsubscribe(&ch).await {
        Ok(()) => {
            if let Some(id) = id {
                emit_terminal(
                    sink,
                    "unsubscribed",
                    id,
                    json!({
                        "address": address,
                        "service": svc,
                        "characteristic": chr,
                    }),
                );
            }
        }
        Err(e) => emit_error(
            sink,
            id,
            "unsubscribe",
            Some(&address),
            error_code(&e),
            &e.to_string(),
        ),
    }
}

async fn cmd_descriptor(
    state: &SharedState,
    sink: &EventSink,
    id: Option<u64>,
    cmd: &Value,
    write: bool,
) {
    let command = if write {
        "writeDescriptor"
    } else {
        "readDescriptor"
    };
    let Some((p, ch, address, svc, chr)) =
        resolve_characteristic(state, sink, id, command, cmd).await
    else {
        return;
    };
    let desc_raw = cmd.get("descriptor").and_then(|v| v.as_str()).unwrap_or("");
    let Some(desc_uuid) = parse_uuid(desc_raw) else {
        emit_error(
            sink,
            id,
            command,
            Some(&address),
            "badRequest",
            "missing or malformed descriptor uuid",
        );
        return;
    };
    let Some(descriptor) = ch.descriptors.iter().find(|d| d.uuid == desc_uuid).cloned() else {
        emit_error(
            sink,
            id,
            command,
            Some(&address),
            "unknownDescriptor",
            "descriptor not in the discovered database",
        );
        return;
    };
    let base = json!({
        "address": address,
        "service": svc,
        "characteristic": chr,
        "descriptor": fmt_uuid(&desc_uuid),
    });
    if write {
        let value_b64 = cmd.get("value").and_then(|v| v.as_str()).unwrap_or("");
        let Ok(value) = B64.decode(value_b64) else {
            emit_error(
                sink,
                id,
                command,
                Some(&address),
                "badRequest",
                "value is not valid base64",
            );
            return;
        };
        match p.write_descriptor(&descriptor, &value).await {
            Ok(()) => {
                if let Some(id) = id {
                    emit_terminal(sink, "descriptorWriteResult", id, base);
                }
            }
            Err(e) => emit_error(
                sink,
                id,
                command,
                Some(&address),
                error_code(&e),
                &e.to_string(),
            ),
        }
    } else {
        match p.read_descriptor(&descriptor).await {
            Ok(value) => {
                if let Some(id) = id {
                    let mut payload = base;
                    payload["value"] = Value::from(B64.encode(&value));
                    emit_terminal(sink, "descriptorReadResult", id, payload);
                }
            }
            Err(e) => emit_error(
                sink,
                id,
                command,
                Some(&address),
                error_code(&e),
                &e.to_string(),
            ),
        }
    }
}

async fn cmd_read_rssi(state: &SharedState, sink: &EventSink, id: Option<u64>, cmd: &Value) {
    let Some((p, address)) = require_peripheral(state, sink, id, "readRssi", cmd).await else {
        return;
    };
    // btleplug exposes no live RSSI read; answer with the last value seen in
    // an advertisement (declared as rssi="lastSeen" in the capabilities).
    let rssi = p.properties().await.ok().flatten().and_then(|p| p.rssi);
    match rssi {
        Some(rssi) => {
            if let Some(id) = id {
                emit_terminal(
                    sink,
                    "rssiResult",
                    id,
                    json!({"address": address, "rssi": rssi, "source": "lastSeen"}),
                );
            }
        }
        None => emit_error(
            sink,
            id,
            "readRssi",
            Some(&address),
            "notSupported",
            "no RSSI observed for this peripheral",
        ),
    }
}

async fn peripheral_name(p: &Peripheral) -> String {
    p.properties()
        .await
        .ok()
        .flatten()
        .and_then(|p| p.local_name)
        .unwrap_or_default()
}

// ---------------- notification pump ----------------

/// Starts (once per peripheral) the task that forwards ValueNotifications
/// as "notification" events. btleplug delivers all subscribed
/// characteristics of a peripheral through a single stream.
async fn ensure_notification_pump(
    state: &SharedState,
    sink: &EventSink,
    address: &str,
    peripheral: &Peripheral,
) {
    let mut s = state.lock().await;
    if let Some(existing) = s.notif_pumps.get(address) {
        if !existing.is_finished() {
            return;
        }
    }
    let p = peripheral.clone();
    let addr = address.to_string();
    let sink = sink.clone();
    let task = tokio::spawn(async move {
        let mut stream = match p.notifications().await {
            Ok(stream) => stream,
            Err(e) => {
                eprintln!(
                    "[Cn1BleHelper] notification stream failed for {}: {}",
                    addr, e
                );
                return;
            }
        };
        while let Some(n) = stream.next().await {
            let service = p
                .characteristics()
                .into_iter()
                .find(|c| c.uuid == n.uuid)
                .map(|c| fmt_uuid(&c.service_uuid))
                .unwrap_or_default();
            emit_event(
                &sink,
                "notification",
                json!({
                    "address": addr,
                    "service": service,
                    "characteristic": fmt_uuid(&n.uuid),
                    "value": B64.encode(&n.value),
                }),
            );
        }
    });
    s.notif_pumps.insert(address.to_string(), task);
}

// ---------------- central-event listener ----------------

fn central_state_name(state: CentralState) -> &'static str {
    match state {
        CentralState::PoweredOn => "poweredOn",
        CentralState::PoweredOff => "poweredOff",
        _ => "unknown",
    }
}

async fn emit_scan_result(sink: &EventSink, address: &str, p: &Peripheral) {
    let props = p.properties().await.ok().flatten();
    let mut obj = JsonMap::new();
    obj.insert("address".into(), Value::from(address));
    match props {
        Some(prop) => {
            obj.insert(
                "name".into(),
                Value::from(prop.local_name.unwrap_or_default()),
            );
            obj.insert("rssi".into(), Value::from(prop.rssi.unwrap_or(-127)));
            if let Some(tx) = prop.tx_power_level {
                obj.insert("txPower".into(), Value::from(tx));
            }
            let services: Vec<Value> = prop
                .services
                .iter()
                .map(|u| Value::from(fmt_uuid(u)))
                .collect();
            obj.insert("serviceUuids".into(), Value::from(services));
            let mut manuf = JsonMap::new();
            for (company, data) in &prop.manufacturer_data {
                manuf.insert(company.to_string(), Value::from(B64.encode(data)));
            }
            obj.insert("manufacturerData".into(), Value::Object(manuf));
            let mut svc_data = JsonMap::new();
            for (uuid, data) in &prop.service_data {
                svc_data.insert(fmt_uuid(uuid), Value::from(B64.encode(data)));
            }
            obj.insert("serviceData".into(), Value::Object(svc_data));
        }
        None => {
            obj.insert("name".into(), Value::from(""));
            obj.insert("rssi".into(), Value::from(-127));
        }
    }
    emit_event(sink, "scanResult", Value::Object(obj));
}

async fn central_event_loop(state: SharedState, sink: EventSink, adapter: Adapter) {
    let mut events = match adapter.events().await {
        Ok(e) => e,
        Err(err) => {
            emit_error(
                &sink,
                None,
                "adapterEvents",
                None,
                "ioError",
                &err.to_string(),
            );
            return;
        }
    };
    while let Some(event) = events.next().await {
        match event {
            CentralEvent::StateUpdate(new_state) => {
                emit_event(
                    &sink,
                    "stateChanged",
                    json!({"state": central_state_name(new_state)}),
                );
            }
            CentralEvent::DeviceDiscovered(id) | CentralEvent::DeviceUpdated(id) => {
                let address = id_to_address(&id);
                let Ok(p) = adapter.peripheral(&id).await else {
                    continue;
                };
                let scanning = {
                    let mut s = state.lock().await;
                    s.peripherals.insert(address.clone(), p.clone());
                    s.scanning
                };
                // The peripheral cache is refreshed even between scans (so
                // connect-by-address keeps working) but sightings only flow
                // to the Java side while a scan is active.
                if scanning {
                    emit_scan_result(&sink, &address, &p).await;
                }
            }
            CentralEvent::DeviceConnected(id) => {
                let address = id_to_address(&id);
                let name = match adapter.peripheral(&id).await {
                    Ok(p) => peripheral_name(&p).await,
                    Err(_) => String::new(),
                };
                emit_event(
                    &sink,
                    "connected",
                    json!({"address": address, "name": name}),
                );
            }
            CentralEvent::DeviceDisconnected(id) => {
                let address = id_to_address(&id);
                // Tear down the notification pump tied to this address.
                let mut s = state.lock().await;
                if let Some(task) = s.notif_pumps.remove(&address) {
                    task.abort();
                }
                drop(s);
                emit_event(
                    &sink,
                    "disconnected",
                    json!({"address": address, "reason": ""}),
                );
            }
            _ => {}
        }
    }
}

// ---------------- global engine ----------------

/// Everything the C/JNI command entry points need to dispatch an operation.
/// Cloneable pieces (handle, state, sink) are pulled out under a short lock so
/// spawning a command never blocks concurrent poll_event calls.
struct BleEngine {
    /// Kept alive so its worker threads and spawned tasks keep running.
    _runtime: tokio::runtime::Runtime,
    handle: tokio::runtime::Handle,
    /// `None` when no BLE adapter is available (engine still delivers the
    /// capabilities + stateChanged=unsupported events, then idles).
    state: Option<SharedState>,
    sink: EventSink,
}

fn engine_slot() -> &'static StdMutex<Option<BleEngine>> {
    static SLOT: OnceLock<StdMutex<Option<BleEngine>>> = OnceLock::new();
    SLOT.get_or_init(|| StdMutex::new(None))
}

/// The receive end of the event channel lives in its own lock so that a
/// blocking `poll_event` never holds the engine lock the command entry
/// points contend on.
fn rx_slot() -> &'static StdMutex<Option<Receiver<String>>> {
    static SLOT: OnceLock<StdMutex<Option<Receiver<String>>>> = OnceLock::new();
    SLOT.get_or_init(|| StdMutex::new(None))
}

static ALIVE: AtomicBool = AtomicBool::new(false);

enum Poll {
    Event(String),
    /// Channel closed / engine torn down — host should treat as end of stream.
    Empty,
    /// No event within the timeout.
    Timeout,
}

/// Initialize the global engine. Returns true iff a BLE adapter is available.
/// Idempotent: a second call just reports the current adapter-available flag.
fn engine_start() -> bool {
    let mut guard = match engine_slot().lock() {
        Ok(g) => g,
        Err(_) => return false,
    };
    if guard.is_some() {
        // Already started: report whether an adapter was found.
        return guard.as_ref().map(|e| e.state.is_some()).unwrap_or(false);
    }

    let runtime = match tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .build()
    {
        Ok(r) => r,
        Err(e) => {
            eprintln!("[Cn1BleHelper] failed to build tokio runtime: {}", e);
            return false;
        }
    };

    let (tx, rx) = std::sync::mpsc::channel::<String>();
    let sink: EventSink = tx;
    if let Ok(mut r) = rx_slot().lock() {
        *r = Some(rx);
    }

    // capabilities is always the first event on the channel.
    emit_capabilities(&sink);

    // Acquire the first adapter, mirroring the old subprocess startup. Any
    // "can't get a working adapter" branch reports stateChanged=unsupported
    // and idles instead of failing hard (CI runners have no BT hardware).
    let adapter_result: Result<Adapter, String> = runtime.block_on(async {
        let manager = Manager::new().await.map_err(|e| e.to_string())?;
        let adapters = manager.adapters().await.map_err(|e| e.to_string())?;
        adapters
            .into_iter()
            .next()
            .ok_or_else(|| "no adapters returned by btleplug".to_string())
    });

    let has_adapter = match adapter_result {
        Ok(adapter) => {
            let state = Arc::new(Mutex::new(State {
                adapter: adapter.clone(),
                peripherals: HashMap::new(),
                notif_pumps: HashMap::new(),
                scanning: false,
            }));
            runtime.spawn(central_event_loop(state.clone(), sink.clone(), adapter));
            // Having an adapter at all is our "ready" signal; platforms that
            // stream true transitions keep updating via StateUpdate.
            emit_event(&sink, "stateChanged", json!({"state": "poweredOn"}));
            let handle = runtime.handle().clone();
            *guard = Some(BleEngine {
                _runtime: runtime,
                handle,
                state: Some(state),
                sink,
            });
            true
        }
        Err(e) => {
            eprintln!(
                "[Cn1BleHelper] no usable adapter ({}); reporting unsupported",
                e
            );
            emit_event(&sink, "stateChanged", json!({"state": "unsupported"}));
            let handle = runtime.handle().clone();
            // Keep the engine alive (adapter-less) so the two startup events
            // remain drainable via poll_event; commands become no-ops.
            *guard = Some(BleEngine {
                _runtime: runtime,
                handle,
                state: None,
                sink,
            });
            false
        }
    };

    ALIVE.store(true, Ordering::SeqCst);
    has_adapter
}

fn engine_is_alive() -> bool {
    ALIVE.load(Ordering::SeqCst)
}

/// Block up to `timeout_ms` for the next event JSON.
fn engine_poll(timeout_ms: i64) -> Poll {
    let guard = match rx_slot().lock() {
        Ok(g) => g,
        Err(_) => return Poll::Empty,
    };
    let Some(rx) = guard.as_ref() else {
        return Poll::Empty;
    };
    let dur = Duration::from_millis(timeout_ms.max(0) as u64);
    match rx.recv_timeout(dur) {
        Ok(line) => Poll::Event(line),
        Err(RecvTimeoutError::Timeout) => Poll::Timeout,
        Err(RecvTimeoutError::Disconnected) => Poll::Empty,
    }
}

/// Tear the engine down: drop the runtime (aborting all tasks) and the
/// receiver. Subsequent poll_event calls report Empty (closed).
fn engine_close() {
    ALIVE.store(false, Ordering::SeqCst);
    if let Ok(mut g) = engine_slot().lock() {
        *g = None;
    }
    if let Ok(mut r) = rx_slot().lock() {
        *r = None;
    }
}

/// Spawn one BLE command on the runtime. No-op when the engine is absent or
/// adapter-less (matching the old subprocess's idle behavior).
fn dispatch(cmd: Value) {
    let guard = match engine_slot().lock() {
        Ok(g) => g,
        Err(_) => return,
    };
    if let Some(engine) = guard.as_ref() {
        if let Some(state) = &engine.state {
            let state = state.clone();
            let sink = engine.sink.clone();
            engine.handle.spawn(handle_command(state, sink, cmd));
        }
    }
}

// ---------------- shared operation builders ----------------
//
// Both ABIs convert their native inputs to Rust types, then call one of
// these to build the exact command JSON the dispatcher understands.

fn op_scan_start(id: i64, service_csv: &str) {
    let services: Vec<String> = service_csv
        .split(',')
        .map(|s| s.trim())
        .filter(|s| !s.is_empty())
        .map(|s| s.to_string())
        .collect();
    dispatch(json!({"cmd": "scanStart", "id": id, "services": services}));
}

fn op_scan_stop(id: i64) {
    dispatch(json!({"cmd": "scanStop", "id": id}));
}

fn op_connect(id: i64, address: &str) {
    dispatch(json!({"cmd": "connect", "id": id, "address": address}));
}

fn op_disconnect(id: i64, address: &str) {
    dispatch(json!({"cmd": "disconnect", "id": id, "address": address}));
}

fn op_discover(id: i64, address: &str) {
    dispatch(json!({"cmd": "discover", "id": id, "address": address}));
}

fn op_read(id: i64, address: &str, service: &str, characteristic: &str) {
    dispatch(json!({
        "cmd": "read", "id": id, "address": address,
        "service": service, "characteristic": characteristic
    }));
}

fn op_write(
    id: i64,
    address: &str,
    service: &str,
    characteristic: &str,
    value: &[u8],
    no_response: bool,
) {
    dispatch(json!({
        "cmd": "write", "id": id, "address": address,
        "service": service, "characteristic": characteristic,
        "value": B64.encode(value), "noResponse": no_response
    }));
}

fn op_subscribe(id: i64, address: &str, service: &str, characteristic: &str, enable: bool) {
    let cmd = if enable { "subscribe" } else { "unsubscribe" };
    dispatch(json!({
        "cmd": cmd, "id": id, "address": address,
        "service": service, "characteristic": characteristic
    }));
}

fn op_read_descriptor(
    id: i64,
    address: &str,
    service: &str,
    characteristic: &str,
    descriptor: &str,
) {
    dispatch(json!({
        "cmd": "readDescriptor", "id": id, "address": address,
        "service": service, "characteristic": characteristic, "descriptor": descriptor
    }));
}

fn op_write_descriptor(
    id: i64,
    address: &str,
    service: &str,
    characteristic: &str,
    descriptor: &str,
    value: &[u8],
) {
    dispatch(json!({
        "cmd": "writeDescriptor", "id": id, "address": address,
        "service": service, "characteristic": characteristic,
        "descriptor": descriptor, "value": B64.encode(value)
    }));
}

fn op_read_rssi(id: i64, address: &str) {
    dispatch(json!({"cmd": "readRssi", "id": id, "address": address}));
}

// ======================================================================
//  C ABI — for the ParparVM Windows/Linux ports.
//  Every export is null-pointer-safe and never unwinds across the boundary.
//  Strings returned by cn1ble_poll_event are owned by the caller and MUST be
//  released with cn1ble_free (they are Rust CString allocations — do NOT call
//  libc free() on them).
// ======================================================================

/// SAFETY: reads a NUL-terminated C string; null yields an empty String.
unsafe fn cstr_to_string(p: *const c_char) -> String {
    if p.is_null() {
        String::new()
    } else {
        CStr::from_ptr(p).to_string_lossy().into_owned()
    }
}

/// SAFETY: reads `len` bytes at `p`; null/negative yields an empty slice.
unsafe fn cbytes_to_vec(p: *const c_uchar, len: c_int) -> Vec<u8> {
    if p.is_null() || len <= 0 {
        Vec::new()
    } else {
        std::slice::from_raw_parts(p, len as usize).to_vec()
    }
}

fn into_c_string(s: String) -> *mut c_char {
    match CString::new(s) {
        Ok(c) => c.into_raw(),
        // Interior NUL should never happen for our JSON; fall back to "".
        Err(_) => CString::new("").unwrap().into_raw(),
    }
}

#[no_mangle]
pub extern "C" fn cn1ble_start() -> c_int {
    catch_unwind(|| if engine_start() { 1 } else { 0 }).unwrap_or(0)
}

#[no_mangle]
pub extern "C" fn cn1ble_is_alive() -> c_int {
    catch_unwind(|| if engine_is_alive() { 1 } else { 0 }).unwrap_or(0)
}

#[no_mangle]
pub extern "C" fn cn1ble_poll_event(timeout_ms: c_long) -> *mut c_char {
    catch_unwind(|| match engine_poll(timeout_ms as i64) {
        Poll::Event(s) => into_c_string(s),
        Poll::Empty => into_c_string(String::new()),
        Poll::Timeout => std::ptr::null_mut(),
    })
    .unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "C" fn cn1ble_free(p: *mut c_char) {
    if p.is_null() {
        return;
    }
    let _ = catch_unwind(AssertUnwindSafe(|| unsafe {
        drop(CString::from_raw(p));
    }));
}

#[no_mangle]
pub extern "C" fn cn1ble_scan_start(id: c_long, service_csv: *const c_char) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let csv = unsafe { cstr_to_string(service_csv) };
        op_scan_start(id as i64, &csv);
    }));
}

#[no_mangle]
pub extern "C" fn cn1ble_scan_stop(id: c_long) {
    let _ = catch_unwind(AssertUnwindSafe(|| op_scan_stop(id as i64)));
}

#[no_mangle]
pub extern "C" fn cn1ble_connect(id: c_long, address: *const c_char) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = unsafe { cstr_to_string(address) };
        op_connect(id as i64, &address);
    }));
}

#[no_mangle]
pub extern "C" fn cn1ble_disconnect(id: c_long, address: *const c_char) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = unsafe { cstr_to_string(address) };
        op_disconnect(id as i64, &address);
    }));
}

#[no_mangle]
pub extern "C" fn cn1ble_discover(id: c_long, address: *const c_char) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = unsafe { cstr_to_string(address) };
        op_discover(id as i64, &address);
    }));
}

#[no_mangle]
pub extern "C" fn cn1ble_read(
    id: c_long,
    address: *const c_char,
    service: *const c_char,
    characteristic: *const c_char,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = unsafe { cstr_to_string(address) };
        let service = unsafe { cstr_to_string(service) };
        let characteristic = unsafe { cstr_to_string(characteristic) };
        op_read(id as i64, &address, &service, &characteristic);
    }));
}

#[no_mangle]
pub extern "C" fn cn1ble_write(
    id: c_long,
    address: *const c_char,
    service: *const c_char,
    characteristic: *const c_char,
    value: *const c_uchar,
    value_len: c_int,
    no_response: c_int,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = unsafe { cstr_to_string(address) };
        let service = unsafe { cstr_to_string(service) };
        let characteristic = unsafe { cstr_to_string(characteristic) };
        let bytes = unsafe { cbytes_to_vec(value, value_len) };
        op_write(
            id as i64,
            &address,
            &service,
            &characteristic,
            &bytes,
            no_response != 0,
        );
    }));
}

#[no_mangle]
pub extern "C" fn cn1ble_subscribe(
    id: c_long,
    address: *const c_char,
    service: *const c_char,
    characteristic: *const c_char,
    enable: c_int,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = unsafe { cstr_to_string(address) };
        let service = unsafe { cstr_to_string(service) };
        let characteristic = unsafe { cstr_to_string(characteristic) };
        op_subscribe(id as i64, &address, &service, &characteristic, enable != 0);
    }));
}

#[no_mangle]
pub extern "C" fn cn1ble_read_descriptor(
    id: c_long,
    address: *const c_char,
    service: *const c_char,
    characteristic: *const c_char,
    descriptor: *const c_char,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = unsafe { cstr_to_string(address) };
        let service = unsafe { cstr_to_string(service) };
        let characteristic = unsafe { cstr_to_string(characteristic) };
        let descriptor = unsafe { cstr_to_string(descriptor) };
        op_read_descriptor(id as i64, &address, &service, &characteristic, &descriptor);
    }));
}

#[no_mangle]
pub extern "C" fn cn1ble_write_descriptor(
    id: c_long,
    address: *const c_char,
    service: *const c_char,
    characteristic: *const c_char,
    descriptor: *const c_char,
    value: *const c_uchar,
    value_len: c_int,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = unsafe { cstr_to_string(address) };
        let service = unsafe { cstr_to_string(service) };
        let characteristic = unsafe { cstr_to_string(characteristic) };
        let descriptor = unsafe { cstr_to_string(descriptor) };
        let bytes = unsafe { cbytes_to_vec(value, value_len) };
        op_write_descriptor(
            id as i64,
            &address,
            &service,
            &characteristic,
            &descriptor,
            &bytes,
        );
    }));
}

#[no_mangle]
pub extern "C" fn cn1ble_read_rssi(id: c_long, address: *const c_char) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = unsafe { cstr_to_string(address) };
        op_read_rssi(id as i64, &address);
    }));
}

#[no_mangle]
pub extern "C" fn cn1ble_close() {
    let _ = catch_unwind(AssertUnwindSafe(engine_close));
}

// ======================================================================
//  JNI ABI — for the JavaSE port (class
//  com.codename1.impl.javase.bluetooth.JniBleBridge). Delegates to the same
//  internals as the C ABI. pollEvent returns Java null on timeout and "" once
//  closed.
// ======================================================================

fn jstr(env: &mut JNIEnv, s: &JString) -> String {
    if s.is_null() {
        return String::new();
    }
    match env.get_string(s) {
        Ok(js) => js.into(),
        Err(_) => String::new(),
    }
}

fn jbytes(env: &mut JNIEnv, a: &JByteArray) -> Vec<u8> {
    if a.is_null() {
        return Vec::new();
    }
    env.convert_byte_array(a).unwrap_or_default()
}

fn jbool(b: bool) -> jboolean {
    if b {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[no_mangle]
pub extern "system" fn Java_com_codename1_impl_javase_bluetooth_JniBleBridge_start(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    catch_unwind(|| jbool(engine_start())).unwrap_or(JNI_FALSE)
}

#[no_mangle]
pub extern "system" fn Java_com_codename1_impl_javase_bluetooth_JniBleBridge_isAlive(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    catch_unwind(|| jbool(engine_is_alive())).unwrap_or(JNI_FALSE)
}

#[no_mangle]
pub extern "system" fn Java_com_codename1_impl_javase_bluetooth_JniBleBridge_pollEvent(
    env: JNIEnv,
    _class: JClass,
    timeout_millis: jlong,
) -> jstring {
    catch_unwind(AssertUnwindSafe(|| {
        match engine_poll(timeout_millis as i64) {
            Poll::Event(s) => env
                .new_string(s)
                .map(|o| o.into_raw())
                .unwrap_or(std::ptr::null_mut()),
            // Closed: hand back the empty string (distinct from timeout's null).
            Poll::Empty => env
                .new_string("")
                .map(|o| o.into_raw())
                .unwrap_or(std::ptr::null_mut()),
            Poll::Timeout => std::ptr::null_mut(),
        }
    }))
    .unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_codename1_impl_javase_bluetooth_JniBleBridge_scanStart(
    mut env: JNIEnv,
    _class: JClass,
    id: jlong,
    service_csv: JString,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let csv = jstr(&mut env, &service_csv);
        op_scan_start(id as i64, &csv);
    }));
}

#[no_mangle]
pub extern "system" fn Java_com_codename1_impl_javase_bluetooth_JniBleBridge_scanStop(
    _env: JNIEnv,
    _class: JClass,
    id: jlong,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| op_scan_stop(id as i64)));
}

#[no_mangle]
pub extern "system" fn Java_com_codename1_impl_javase_bluetooth_JniBleBridge_connect(
    mut env: JNIEnv,
    _class: JClass,
    id: jlong,
    address: JString,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = jstr(&mut env, &address);
        op_connect(id as i64, &address);
    }));
}

#[no_mangle]
pub extern "system" fn Java_com_codename1_impl_javase_bluetooth_JniBleBridge_disconnect(
    mut env: JNIEnv,
    _class: JClass,
    id: jlong,
    address: JString,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = jstr(&mut env, &address);
        op_disconnect(id as i64, &address);
    }));
}

#[no_mangle]
pub extern "system" fn Java_com_codename1_impl_javase_bluetooth_JniBleBridge_discover(
    mut env: JNIEnv,
    _class: JClass,
    id: jlong,
    address: JString,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = jstr(&mut env, &address);
        op_discover(id as i64, &address);
    }));
}

#[no_mangle]
pub extern "system" fn Java_com_codename1_impl_javase_bluetooth_JniBleBridge_read(
    mut env: JNIEnv,
    _class: JClass,
    id: jlong,
    address: JString,
    service: JString,
    characteristic: JString,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = jstr(&mut env, &address);
        let service = jstr(&mut env, &service);
        let characteristic = jstr(&mut env, &characteristic);
        op_read(id as i64, &address, &service, &characteristic);
    }));
}

#[no_mangle]
pub extern "system" fn Java_com_codename1_impl_javase_bluetooth_JniBleBridge_write(
    mut env: JNIEnv,
    _class: JClass,
    id: jlong,
    address: JString,
    service: JString,
    characteristic: JString,
    value: JByteArray,
    no_response: jboolean,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = jstr(&mut env, &address);
        let service = jstr(&mut env, &service);
        let characteristic = jstr(&mut env, &characteristic);
        let bytes = jbytes(&mut env, &value);
        op_write(
            id as i64,
            &address,
            &service,
            &characteristic,
            &bytes,
            no_response != JNI_FALSE,
        );
    }));
}

#[no_mangle]
pub extern "system" fn Java_com_codename1_impl_javase_bluetooth_JniBleBridge_subscribe(
    mut env: JNIEnv,
    _class: JClass,
    id: jlong,
    address: JString,
    service: JString,
    characteristic: JString,
    enable: jboolean,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = jstr(&mut env, &address);
        let service = jstr(&mut env, &service);
        let characteristic = jstr(&mut env, &characteristic);
        op_subscribe(
            id as i64,
            &address,
            &service,
            &characteristic,
            enable != JNI_FALSE,
        );
    }));
}

#[no_mangle]
pub extern "system" fn Java_com_codename1_impl_javase_bluetooth_JniBleBridge_readDescriptor(
    mut env: JNIEnv,
    _class: JClass,
    id: jlong,
    address: JString,
    service: JString,
    characteristic: JString,
    descriptor: JString,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = jstr(&mut env, &address);
        let service = jstr(&mut env, &service);
        let characteristic = jstr(&mut env, &characteristic);
        let descriptor = jstr(&mut env, &descriptor);
        op_read_descriptor(id as i64, &address, &service, &characteristic, &descriptor);
    }));
}

#[no_mangle]
pub extern "system" fn Java_com_codename1_impl_javase_bluetooth_JniBleBridge_writeDescriptor(
    mut env: JNIEnv,
    _class: JClass,
    id: jlong,
    address: JString,
    service: JString,
    characteristic: JString,
    descriptor: JString,
    value: JByteArray,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = jstr(&mut env, &address);
        let service = jstr(&mut env, &service);
        let characteristic = jstr(&mut env, &characteristic);
        let descriptor = jstr(&mut env, &descriptor);
        let bytes = jbytes(&mut env, &value);
        op_write_descriptor(
            id as i64,
            &address,
            &service,
            &characteristic,
            &descriptor,
            &bytes,
        );
    }));
}

#[no_mangle]
pub extern "system" fn Java_com_codename1_impl_javase_bluetooth_JniBleBridge_readRssi(
    mut env: JNIEnv,
    _class: JClass,
    id: jlong,
    address: JString,
) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let address = jstr(&mut env, &address);
        op_read_rssi(id as i64, &address);
    }));
}

#[no_mangle]
pub extern "system" fn Java_com_codename1_impl_javase_bluetooth_JniBleBridge_close(
    _env: JNIEnv,
    _class: JClass,
) {
    let _ = catch_unwind(AssertUnwindSafe(engine_close));
}
