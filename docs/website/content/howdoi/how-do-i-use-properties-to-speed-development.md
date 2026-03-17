---
title: USE PROPERTIES TO SPEED DEVELOPMENT
slug: how-do-i-use-properties-to-speed-development
url: /how-do-i/how-do-i-use-properties-to-speed-development/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-use-properties-to-speed-development.html
tags:
- basic
- io
- ui
description: Simplify UI, Database/Storage and Parsing code
youtube_id: 77N2t2n8rbQ
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-19.jpg
---
{{< youtube "77N2t2n8rbQ" >}}

Codename One properties are useful when you want one model class to do more than simply hold data. A plain old Java object can represent state just fine, but it does not automatically know how to bind to UI, serialize itself, parse structured input, or describe its own fields at runtime. The properties API exists to make those jobs easier.

The core idea is that a property-backed object carries metadata about its fields through the property index. That means the framework can introspect the object safely even after obfuscation. Once that metadata exists, a lot of repetitive plumbing becomes easier: JSON and XML mapping, serialization, SQL helpers, UI binding, and generated forms.

This is what makes properties more than just a different syntax for getters and setters. You still keep a clear data model, but you also gain a structured description of that model that the framework can reuse. That is why the video moves quickly from basic property access into parsing, storage, CRUD helpers, and binding. Those features all depend on the same underlying introspection capability.

Two uses are especially practical. The first is data mapping. If your app receives structured data from a service and you want a cleaner route from raw JSON or XML into an object model, properties can reduce a lot of manual parsing code. The second is UI binding. If a field in the model changes and a component should reflect that change, or vice versa, the properties API gives you a much cleaner starting point than manually wiring every update yourself.

The video also highlights Instant UI, which can generate forms from property objects. That is still conceptually useful, but it should be applied with judgment. Generated UI can be a strong accelerator for internal tools, simple data-entry screens, or prototypes. It is not automatically the best fit for polished product UI where custom layout and styling matter more. In modern projects, CSS and hand-authored layout code still remain the better choice for most high-touch product screens.

So the best way to think about properties is as a productivity tool for model-driven parts of an application. If the same object needs to be bound to UI, serialized, parsed, and perhaps stored, properties can eliminate a lot of repetitive glue code. If the object is simple and none of those benefits matter, a normal POJO may still be the simpler choice.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Properties Are Amazing](/blog/properties-are-amazing/)
- [How Do I Use Storage, File System And SQL](/how-do-i/how-do-i-use-storage-file-system-sql/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
