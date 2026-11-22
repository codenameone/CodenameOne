---
title: "Flowcharts, Node Editors and Kanban Boards Are Now Standard Components"
slug: diagrams-flowcharts-and-kanban-boards
url: /blog/diagrams-flowcharts-and-kanban-boards/
date: '2026-11-25'
author: Shai Almog
description: "NodeGraph draws diagrams and port-based node editors with thousands of nodes, automatic layered, tree, radial and force layouts, flowchart shapes, swim lanes, a minimap, undo and JSON save. KanbanBoard adds draggable cards, swim lanes and WIP limits. Both ship in the core, on every platform."
feed_html: "<img src=\"https://www.codenameone.com/blog/diagrams-flowcharts-and-kanban-boards.jpg\" alt=\"Flowcharts and Kanban boards in Java\" /> NodeGraph for diagrams, flowcharts and node editors, and KanbanBoard with swim lanes and WIP limits, as standard components on iOS, Android, desktop and web."
series: ["release-2026-11-20"]
---

![Flowcharts and Kanban boards in Java](/blog/diagrams-flowcharts-and-kanban-boards.jpg)

Show a business user their process as a flowchart and they understand it in ten seconds. Show them the same process as a table of states and they ask for a meeting. That's why every serious business app ends up needing a diagram: an approval flow, an org chart, a network map, a pipeline of deals. And in almost every framework, that diagram is a third party extension, a commercial license, or a web view running a JavaScript library inside your native app.

That's backwards. The baseline for a business app moved, and a framework that stops at buttons and lists stops short of what your users expect. So this week Codename One ships two new core components: `NodeGraph` for diagrams and node editors, and `KanbanBoard`.

## A diagram in six lines

```java {class="cn1-no-try"}
DefaultGraphModel model = new DefaultGraphModel();
model.addNode(new GraphNode("web", "Web app"));
model.addNode(new GraphNode("api", "API gateway"));
model.addEdge("web", "api");
NodeGraph graph = new NodeGraph(model);
graph.arrange(new LayeredArrangement(), false);
```

You don't place anything. The graph lays itself out. `LayeredArrangement` is for flows, `TreeArrangement` for hierarchies, `RadialArrangement` for something at the center, and `ForceArrangement` for networks, and it can run live while the user drags. Pin a node and the layout flows around it.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,260px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img src="/developer-guide/img/generated/graph-services.png" alt="A service architecture laid out automatically" loading="lazy" style="width:100%;height:auto" /><figcaption>Layered layout.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/graph-tree.png" alt="A tree laid out automatically" loading="lazy" style="width:100%;height:auto" /><figcaption>Tree layout.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/graph-radial.png" alt="A radial layout" loading="lazy" style="width:100%;height:auto" /><figcaption>Radial layout.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/graph-force.png" alt="A force directed network layout" loading="lazy" style="width:100%;height:auto" /><figcaption>Force layout.</figcaption></figure>
</div>
{{< /guide-block >}}

Like our [DataGrid](/blog/a-million-rows-and-live-excel-formulas/) and Gantt chart, `NodeGraph` paints its nodes instead of making a component for each one, so thousands of nodes pan and zoom smoothly on a phone. Zoom far out and nodes become plain cards, the way a map drops street names.

## Flowcharts with swim lanes

Flowchart shapes, swim lanes, edge labels and line styles are built in:

```java {class="cn1-no-try"}
model.addGroup(GraphGroup.lane("shop", "Shop", true, 0, 0, 620, 150));
GraphNode paid = new GraphNode("paid", "Paid?", 220, 40, 110, 70)
        .setShape(GraphShapes.DECISION);
paid.setGroupId("shop");
model.addNode(paid);
model.addEdge("paid", "cancel").setLabel("no");
model.addEdge("paid", "pick").setLabel("yes");

NodeGraph graph = new NodeGraph(model);
graph.setEdgeStyle(NodeGraph.EDGE_ORTHOGONAL);
graph.setEditingEnabled(true);
```

![A flowchart with swim lanes for the shop and the warehouse](/developer-guide/img/generated/graph-flowchart.png)

Turn editing on and the user can draw their own: a palette of shapes to drag in, resize handles, a minimap, undo and redo, and copy and paste. `GraphJson` saves the diagram and loads it back, so the flowchart a manager drew on an iPad goes to your server and opens on a colleague's desktop.

## Node editors, like the ones in game engines

The second mode is the one you know from shader editors and automation tools. Nodes have typed ports, and the user wires outputs to inputs:

```java {class="cn1-no-try"}
model.addNode(new GraphNode("noise", "Noise", 0, 0, 160, 90)
        .addOutput("out", "Value", "float"));
model.addNode(new GraphNode("color", "Color ramp", 240, 0, 160, 90)
        .addInput("in", "Factor", "float")
        .addOutput("out", "Color", "color"));
NodeGraph graph = new NodeGraph(model);
graph.setEditingEnabled(true);
graph.setEdgeStyle(NodeGraph.EDGE_CURVED);
```

A `ConnectionValidator` refuses a wire from a color into a number, `setMaxConnections` caps a port, and `setAllowCycles(false)` keeps the graph acyclic. Nodes can hold real components, a slider or a text field inside a node that scales with the zoom. That's the building block for a workflow builder, an IoT rule engine, a visual query designer or a no-code tool inside your product.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,300px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img src="/developer-guide/img/generated/graph-editor.png" alt="A port based node editor with typed connections" loading="lazy" style="width:100%;height:auto" /><figcaption>A node editor with typed ports.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/graph-components.png" alt="Nodes containing live components" loading="lazy" style="width:100%;height:auto" /><figcaption>Real components inside nodes.</figcaption></figure>
</div>
{{< /guide-block >}}

## Kanban, with the rules teams actually use

```java {class="cn1-no-try"}
DefaultKanbanModel model = new DefaultKanbanModel();
model.addColumn(new KanbanColumn("todo", "To do"));
model.addColumn(new KanbanColumn("doing", "Doing").setWipLimit(2));
model.addColumn(new KanbanColumn("done", "Done").setCollapsed(true));
model.setEnforceWipLimits(true);
model.addLane(new KanbanLane("app", "App"));
model.addCard("todo", new KanbanCard("c1", "Dark mode").setLaneId("app").addTag("ui"));
KanbanBoard board = new KanbanBoard(model);
```

Cards carry tags, badges, and descriptions. Swim lanes split the board by team or customer. A WIP limit is either a warning or a hard rule. Cards and whole columns drag, a long press lifts a card on a touch screen, and on a phone `setColumnSnap(true)` makes the board page one column at a time instead of shrinking into something unreadable. Undo and redo come built in.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,300px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img src="/developer-guide/img/generated/kanban-board.png" alt="A Kanban board with tagged cards" loading="lazy" style="width:100%;height:auto" /><figcaption>The board.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/generated/kanban-lanes.png" alt="A Kanban board with swim lanes and a WIP limit" loading="lazy" style="width:100%;height:auto" /><figcaption>Swim lanes and WIP limits.</figcaption></figure>
</div>
{{< /guide-block >}}

A Kanban board isn't only for software teams. It's a sales pipeline, a hiring funnel, a repair shop's queue of devices, a content calendar. Any process where things move through stages is a board, and now it's one constructor away.

## Remix them

Put a Kanban board of service tickets next to [Fieldline's](/blog/field-service-scheduling-template/) dispatch map. Let an operations manager draw the approval flow as a diagram, save it with `GraphJson`, and have your [backend](/blog/multi-tenant-saas-backend-in-java/) execute it. Attach the [spreadsheet](/blog/a-million-rows-and-live-excel-formulas/) each stage produces and the [PDF](/blog/generate-and-view-pdfs-in-java/) it ends in. Every one of these exists somewhere. Getting all of them in one framework, tested together on every platform, is the hard part.

Tomorrow: {{< post-link path="/blog/build-a-game-platform-with-luau-scripting" text="Brickyard, a game platform with its own studio" >}}, plus portable shaders and 3D audio.
