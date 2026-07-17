---
title: "Codename One: semantic UI tools over MCP"
slug: 2026-08-01-0300-codenameone-codename-one-mcp-server
platform: linkedin
account: codenameone
source_slug: codename-one-mcp-server
publish_at: '2026-08-01T03:00:00'
timezone: Asia/Jerusalem
review_by: '2026-07-24'
status: draft
image: /blog/codename-one-mcp-server.jpg
---

A running Codename One simulator can now expose its UI to MCP hosts such as Codex, Claude Code, Claude Desktop, and opencode.

Built-in tools return the semantic UI snapshot, find nodes, set text, and perform actions on the Codename One EDT. Applications can add typed domain tools through the existing `com.codename1.ai.Tool` contract.

Socket mode attaches to a visible running tool. A small stdio bridge lets local MCP hosts connect to that same process. Nothing is exposed until the API or MCP menu starts the server.

Full write-up: {{canonical}}
