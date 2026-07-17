// cn1-ble-helper — cross-platform BLE bridge for the Codename One JavaSE
// port's native Bluetooth backend. See PROTOCOL.md (next to Cargo.toml) for
// the exact JSON-line command/event format shared with the Java side
// (com.codename1.impl.javase.bluetooth.NativeBleBackend).
//
// Architecture
// ------------
// One tokio runtime, several concurrent tasks:
//   * stdin reader   -> parses JSON-lines into commands and spawns one task
//                       per command so slow BLE operations never block the
//                       command stream;
//   * central events -> translates btleplug's CentralEvent stream into
//                       stateChanged / scanResult / connected / disconnected
//                       wire events;
//   * per-peripheral notification pumps (one per connected peripheral) ->
//                       translate ValueNotification streams into
//                       "notification" events;
//   * writer         -> drains a single mpsc channel so every line is
//                       written atomically without locking stdout.
//
// Every command carries a numeric "id"; the helper answers with exactly one
// terminal event echoing it as "requestId" (either the success event for
// that command or an "error" event with a typed "code").

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
use std::sync::Arc;
use tokio::io::{AsyncBufReadExt, AsyncWriteExt, BufReader};
use tokio::sync::{mpsc, Mutex};
use tokio::task::JoinHandle;
use uuid::Uuid;

const PROTOCOL_VERSION: u64 = 1;

// ---------------- wire helpers ----------------

/// Single sink for everything the helper writes. Wire writes happen through
/// the corresponding receiver task so no two events ever interleave.
type EventSink = mpsc::UnboundedSender<Value>;

fn emit(sink: &EventSink, event: Value) {
    // If the channel is closed the writer task has exited and we're tearing
    // down; dropping a stray event is harmless.
    let _ = sink.send(event);
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

/// The very first line the helper writes: what this build can do, so the
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
        "shutdown" => std::process::exit(0),
        other => {
            eprintln!("cn1-ble-helper: unknown command '{}'", other);
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
    let command = if write { "writeDescriptor" } else { "readDescriptor" };
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
                    "cn1-ble-helper: notification stream failed for {}: {}",
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

// ---------------- stdin reader ----------------

async fn stdin_loop(state: SharedState, sink: EventSink) {
    let stdin = tokio::io::stdin();
    let mut reader = BufReader::new(stdin).lines();
    loop {
        match reader.next_line().await {
            Ok(Some(line)) => {
                if line.trim().is_empty() {
                    continue;
                }
                match serde_json::from_str::<Value>(&line) {
                    Ok(cmd) => {
                        let state = state.clone();
                        let sink = sink.clone();
                        tokio::spawn(async move {
                            handle_command(state, sink, cmd).await;
                        });
                    }
                    Err(e) => eprintln!("cn1-ble-helper: malformed JSON: {} ({})", line, e),
                }
            }
            Ok(None) => {
                // stdin EOF — the JVM is gone; exit cleanly.
                std::process::exit(0);
            }
            Err(e) => {
                eprintln!("cn1-ble-helper: stdin read error: {}", e);
                std::process::exit(0);
            }
        }
    }
}

// ---------------- stdout writer ----------------

async fn writer_loop(mut rx: mpsc::UnboundedReceiver<Value>) {
    let mut stdout = tokio::io::stdout();
    while let Some(value) = rx.recv().await {
        let mut line = match serde_json::to_string(&value) {
            Ok(s) => s,
            Err(e) => {
                eprintln!("cn1-ble-helper: failed to serialize event: {}", e);
                continue;
            }
        };
        line.push('\n');
        if let Err(e) = stdout.write_all(line.as_bytes()).await {
            eprintln!("cn1-ble-helper: stdout write failed: {}", e);
            return;
        }
        if let Err(e) = stdout.flush().await {
            eprintln!("cn1-ble-helper: stdout flush failed: {}", e);
            return;
        }
    }
}

// ---------------- entry point ----------------

#[tokio::main(flavor = "current_thread")]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let (tx, rx) = mpsc::unbounded_channel::<Value>();
    tokio::spawn(writer_loop(rx));
    emit_capabilities(&tx);

    // Every "we can't get a working adapter" branch — no BlueZ on the host,
    // permission denied, zero adapters — produces a stateChanged=unsupported
    // event and idles instead of crashing; CI runners without BT hardware
    // hit this all the time.
    let adapter_result: Result<Adapter, Box<dyn std::error::Error>> = async {
        let manager = Manager::new().await?;
        let adapters = manager.adapters().await?;
        adapters
            .into_iter()
            .next()
            .ok_or_else(|| "no adapters returned by btleplug".into())
    }
    .await;

    let adapter = match adapter_result {
        Ok(a) => a,
        Err(e) => {
            // Surface the underlying reason on stderr for diagnostics; the
            // wire-side stateChanged is the only thing the Java side reads.
            eprintln!(
                "cn1-ble-helper: no usable adapter ({}); reporting unsupported",
                e
            );
            emit_event(&tx, "stateChanged", json!({"state": "unsupported"}));
            // Drain stdin so a shutdown command or EOF still terminates
            // us cleanly.
            let stdin = tokio::io::stdin();
            let mut reader = BufReader::new(stdin).lines();
            loop {
                match reader.next_line().await {
                    Ok(Some(_)) => continue,
                    _ => return Ok(()),
                }
            }
        }
    };

    let state = Arc::new(Mutex::new(State {
        adapter: adapter.clone(),
        peripherals: HashMap::new(),
        notif_pumps: HashMap::new(),
        scanning: false,
    }));

    tokio::spawn(central_event_loop(state.clone(), tx.clone(), adapter));

    // Having an adapter at all is our "ready" signal; platforms that stream
    // true state transitions keep updating via CentralEvent::StateUpdate.
    emit_event(&tx, "stateChanged", json!({"state": "poweredOn"}));

    stdin_loop(state, tx).await;
    Ok(())
}
