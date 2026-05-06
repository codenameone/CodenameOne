// Medium adapter. Runs on https://medium.com/new-story.
//
// Medium has a single contenteditable div for both title and body. Type the
// title, press Enter, then paste body HTML via the document selection API
// (Medium's editor accepts HTML pastes and converts to its internal format).
// Set canonical via the Story Settings panel after the body is in place.

(async () => {
  const { waitFor, setReactValue, report, getTaskFor } = window.cn1Syndicator;
  const task = await getTaskFor("medium");
  if (!task) return;
  console.log("[medium-adapter] picked up task", task.slug);

  try {
    const editor = await waitFor(() => document.querySelector("div.postArticle-content[contenteditable='true']"));
    editor.focus();
    document.execCommand("selectAll", false);
    document.execCommand("delete", false);

    // Type the title (Medium converts the first line to <h3.graf--title>)
    document.execCommand("insertText", false, task.title);
    document.execCommand("insertParagraph", false);

    // Paste body as HTML so headings/images/code render.
    if (task.body_html) {
      // execCommand insertHTML works in Medium's contenteditable.
      document.execCommand("insertHTML", false, task.body_html);
    }

    // Wait for Medium's auto-save to assign a draft URL (/p/<id>/edit).
    await new Promise((r) => setTimeout(r, 4000));
    let draftUrl = location.href;

    // Story settings panel: click the gear/settings icon in the top bar
    // (varies by layout — try a couple of selectors), find the
    // "Customize canonical link" / canonical URL input, fill it.
    try {
      const gear = document.querySelector("button[aria-label*='Story settings' i], button[data-action='show-story-meta']");
      if (gear) {
        gear.click();
        const canonical = await waitFor(
          () => document.querySelector("input[placeholder*='canonical' i], input[placeholder*='URL of original' i]"),
          { timeout: 8000 }
        );
        setReactValue(canonical, task.canonical);
        // Close the panel so auto-save fires
        document.body.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape", bubbles: true }));
      }
    } catch (err) {
      console.warn("[medium-adapter] could not set canonical via panel", err);
    }

    // One last wait so auto-save settles after the canonical change.
    await new Promise((r) => setTimeout(r, 4000));
    draftUrl = location.href;
    report(task.id, { success: true, url: draftUrl });
  } catch (err) {
    console.error("[medium-adapter] failed", err);
    report(task.id, { success: false, error: String(err) });
  }
})();
