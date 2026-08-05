#!/usr/bin/env node

import net from "node:net";
import readline from "node:readline";

const host = process.env.GUIBUILDER_MCP_HOST || "127.0.0.1";
const port = Number(process.env.GUIBUILDER_MCP_PORT || process.argv[2] || 8765);
let nextId = 1;
const pending = new Map();
let buffer = "";

const socket = net.createConnection({ host, port });
socket.setEncoding("utf8");
socket.on("data", chunk => {
  buffer += chunk;
  while (buffer.includes("\n")) {
    const index = buffer.indexOf("\n");
    const line = buffer.slice(0, index);
    buffer = buffer.slice(index + 1);
    if (!line.trim()) continue;
    const message = JSON.parse(line);
    const handler = pending.get(message.id);
    if (handler) {
      pending.delete(message.id);
      handler(message);
    } else {
      process.stdout.write(`${line}\n`);
    }
  }
});

function request(method, params = {}) {
  const id = nextId++;
  return new Promise((resolve, reject) => {
    pending.set(id, resolve);
    socket.write(`${JSON.stringify({ jsonrpc: "2.0", id, method, params })}\n`, error => {
      if (error) {
        pending.delete(id);
        reject(error);
      }
    });
  });
}

function toolText(response) {
  const content = response?.result?.content;
  if (!Array.isArray(content) || !content.length) return response;
  const text = content[0]?.text;
  if (typeof text !== "string") return response;
  try { return JSON.parse(text); } catch { return text; }
}

socket.on("connect", async () => {
  const initialized = await request("initialize", {
    protocolVersion: "2025-03-26",
    capabilities: {},
    clientInfo: { name: "guibuilder-local-client", version: "1.0" }
  });
  process.stdout.write(`MCP_CONNECTED ${host}:${port} ${JSON.stringify(initialized.result?.serverInfo || {})}\n`);
  socket.write(`${JSON.stringify({ jsonrpc: "2.0", method: "notifications/initialized", params: {} })}\n`);

  const input = readline.createInterface({ input: process.stdin, terminal: false });
  let queue = Promise.resolve();
  input.on("line", line => {
    queue = queue.then(async () => {
      const trimmed = line.trim();
      if (!trimmed) return;
      if (trimmed === "tools") {
        process.stdout.write(`${JSON.stringify((await request("tools/list")).result)}\n`);
        return;
      }
      if (trimmed === "quit" || trimmed === "exit") {
        socket.end();
        return;
      }
      let command;
      try {
        command = JSON.parse(trimmed);
      } catch {
        process.stdout.write(`${JSON.stringify({ error: "Expected JSON: {tool, arguments}" })}\n`);
        return;
      }
      const response = await request("tools/call", {
        name: command.tool,
        arguments: command.arguments || {}
      });
      process.stdout.write(`${JSON.stringify(toolText(response))}\n`);
    }).catch(error => process.stdout.write(`${JSON.stringify({ error: String(error) })}\n`));
  });
});

socket.on("error", error => {
  process.stderr.write(`MCP_CONNECT_ERROR ${error.message}\n`);
  process.exitCode = 1;
});
