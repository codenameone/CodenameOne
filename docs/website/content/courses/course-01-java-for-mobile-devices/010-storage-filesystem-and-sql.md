---
title: "Storage Filesystem and SQL"
layout: "course-lesson"
course_id: "course-01-java-for-mobile-devices"
course_title: "Java for Mobile Devices - Free online course"
module_title: "Course Lessons"
module_key: "01-course-lessons"
module_order: 1
lesson_order: 10
weight: 10
is_course_lesson: true
description: "Choose the right persistence layer: preferences, storage, filesystem, or SQL."
---
> Module 1: Course Lessons

{{< youtube _EXEN52wQvs >}}

Persistent data in Codename One usually starts with a choice between three levels of storage: `Preferences` for very small settings, `Storage` for simple app-private persisted objects or blobs, and SQL for data that needs real querying, sorting, or filtering. The file system sits beside those as a lower-level tool rather than the default answer to every persistence question.

`Storage` is the place to start for many apps because it is portable and application-oriented. It is not a general shared file hierarchy. It is a higher-level persistence API tied to the app itself. That makes it a much better default than reaching immediately for raw files just because you are used to desktop development.

The file system becomes useful when you actually need file paths, larger assets, or interoperability with APIs that naturally work in terms of files. But mobile app isolation still matters. Devices do not expose the same shared-file assumptions developers are used to on the desktop. Even when some platforms allow more shared file access than others, that behavior is not the best foundation for portable application design.

SQL is the right tool when your data is large enough or dynamic enough that you need real query capability. If you need filtering, sorting, lookups, or structured updates over a meaningful amount of data, SQLite is usually a better fit than trying to serialize everything into a single stored object. If you just need to save app state or a few structured objects, SQL is often unnecessary complexity.

The lesson also makes an important operational point about cleanup. Database cursors and related resources should be closed explicitly. You should not rely on the garbage collector to clean up database resources whenever it happens to run. That becomes especially important when portability differences between platforms affect database behavior and thread safety.

Shipping an initial database is another valid pattern. If the app needs seed data, you can package a database resource and copy it into the correct writable location on first run. That is often easier than trying to generate the full initial data set programmatically every time.

The modern recommendation is mostly about choosing the simplest layer that fits the problem. Use `Preferences` for settings, `Storage` for straightforward app-private persistence, SQL when you genuinely need query power, and filesystem APIs when the problem is really file-oriented. Starting at the highest appropriate level tends to produce the most portable and maintainable code.

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Use Properties To Speed Development](/how-do-i/how-do-i-use-properties-to-speed-development/)
- [How Do I Improve Application Performance Or Track Down Performance Issues](/how-do-i/how-do-i-improve-application-performance-or-track-down-performance-issues/)
