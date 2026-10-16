#!/usr/bin/env python3
"""Local macOS test of production editor command ordering; no CI integration."""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
view = (ROOT / "Ports/MacPort/nativeSources/METALView.m").read_text()
view = view[view.index("@implementation METALView"):]
session = (ROOT / "Ports/MacPort/nativeSources/CN1AppKitTextInput.m").read_text()


def method(source, signature):
    start = source.index(signature)
    end = source.index("\n}", start) + 2
    return source[start:end] + "\n"


with tempfile.TemporaryDirectory(prefix="cn1-editor-command-") as directory:
    tmp = Path(directory)
    (tmp / "queue.h").write_text("".join(method(session, signature) for signature in [
        "- (void)resetDeferredEditsIfNeeded", "- (BOOL)deferNativeEdit:",
        "- (int)beginEditorCommand", "- (void)completeEditorCommand:"]))
    (tmp / "echo.h").write_text(method(session,
        "JAVA_VOID com_codename1_impl_ios_IOSNative_updateTextInputState___")
        + method(session, "void CN1MacTextInputNotifySelection(")
        + method(view, "static NSUInteger cn1ActiveEdge("))
    (tmp / "view.h").write_text("".join(method(view, signature) for signature in [
        "- (BOOL)cn1DeferNativeEdit:", "- (void)cn1SendEditorKeyCommand:(int)command modifiers:",
        "- (void)cn1SendEditorKeyCommand:(int)command {", "- (NSInteger)cn1ReplaceRange:",
        "- (NSRange)cn1EffectiveRange:", "- (void)insertText:", "- (void)cut:", "- (void)paste:",
        "- (NSUInteger)cn1LineStart:", "- (NSUInteger)cn1LineEnd:",
        "- (NSUInteger)cn1AfterLineBreak:", "- (NSUInteger)cn1BeforeLineBreak:",
        "- (NSUInteger)cn1VerticalTarget:", "- (NSUInteger)cn1SelectionAnchor:",
        "- (void)cn1RememberAnchor:", "- (NSUInteger)cn1IndexBefore:",
        "- (NSUInteger)cn1IndexAfter:", "- (NSRange)cn1Extend:",
        "- (void)cn1ForgetAnchor", "- (void)cn1SetSelection:",
        "- (void)doCommandBySelector:", "- (void)selectAll:"]))
    executable = tmp / "contract"
    subprocess.run(["xcrun", "clang", "-fno-objc-arc", "-Wall", "-Wextra", "-Werror",
        "-Wno-unused-parameter", "-framework", "AppKit", "-I", str(tmp),
        str(Path(__file__).with_name("contract.m")), "-o", str(executable)], check=True, timeout=60)
    subprocess.run([str(executable)], check=True, timeout=15)
