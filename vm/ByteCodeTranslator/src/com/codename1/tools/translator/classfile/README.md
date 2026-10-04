# `classfile`: the translator's class-file reader

ParparVM's translator used to read class files through [ASM](https://asm.ow2.io).
This package replaces that dependency. It is a rewrite of the parts of ASM the
translator uses, keeping ASM's API and design so the translator's code did not
have to change shape:

| Here | ASM |
| --- | --- |
| `ClassReader`, `ClassVisitor`, `MethodVisitor`, `FieldVisitor`, `AnnotationVisitor` | the same classes in `org.objectweb.asm` |
| `Opcodes`, `Type`, `Label`, `Handle`, `ConstantDynamic` | the same classes in `org.objectweb.asm` |
| `tree/*Node`, `tree/InsnList` | `org.objectweb.asm.tree` |
| `analysis/Analyzer`, `Frame`, `Interpreter`, `BasicInterpreter`, `SourceInterpreter` and their values | `org.objectweb.asm.tree.analysis` |
| `tree/JsrInliner` | the role of `org.objectweb.asm.commons.JSRInlinerAdapter` |

Every file keeps ASM's copyright and BSD 3-Clause license notice, and the
repository's `NOTICE` file reproduces it.

## Why it exists

The translator must be able to translate *itself*: it is compiled against
`vm/JavaAPI` (ParparVM's class library) and translated to C and to JavaScript,
which is how it runs natively and inside the Playground's browser page. ASM
cannot be translated that way, so the translator carries its own reader. See
`vm/selfhost/README.md` and the developer guide's "Java in the browser" chapter
for how the pieces fit together.

## What it covers, and what it doesn't

It reads, it never writes: there is no `ClassWriter`. The translator only
consumes class files, and the in-tree Java compiler (`vm/JavaCompiler`) writes its
own. Within reading, it supports what the translator visits -- the constant pool,
fields, methods, code, exception tables, line numbers, local variables,
annotations, the `Signature`, `InnerClasses`, `EnclosingMethod`, `Record` and
`BootstrapMethods` attributes, and stack-map frames (read only to place labels).
Any other attribute is skipped.

`tree/JsrInliner` expands `JSR`/`RET` subroutines, which older compilers emitted
for `finally`. The translator's output for such methods is built from the
inlined layout, so the inliner must place code exactly where ASM's did.

## How it is held to ASM

`ClassReaderConformanceTest` (in `vm/tests`) reads class files with both readers
and requires identical visitor event traces, and runs every Codename One core
method through both subroutine inliners. ASM survives in the build only as that
test's oracle and as benchmark corpus.
