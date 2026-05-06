// DZone adapter. Runs on https://dzone.com/content/article/post.html.
//
// DZone's editor is Froala. Title is a textarea (Angular-bound), body lives
// in window.FroalaEditor.INSTANCES[0]. The save mechanism is the "Save draft"
// button — Cloudflare doesn't challenge it because the request originates
// from the user's already-trusted browser session.

(async () => {
  const { waitFor, setReactValue, report, getTaskFor } = window.cn1Syndicator;
  const task = await getTaskFor("dzone");
  if (!task) return;
  console.log("[dzone-adapter] picked up task", task.slug);

  try {
    // Title (Angular ng-model)
    const title = await waitFor(() => document.querySelector("textarea[name='title']"));
    setReactValue(title, task.title);

    // Subtitle / TL;DR — use description if present
    if (task.description) {
      const sub = document.querySelector("textarea[name='subtitle']");
      if (sub) setReactValue(sub, task.description.slice(0, 300));
      const meta = document.getElementById("meta-description-textarea");
      if (meta) setReactValue(meta, task.description.slice(0, 155));
    }

    // Body — set via Froala's JS API
    if (task.body_html) {
      await waitFor(() => window.FroalaEditor && window.FroalaEditor.INSTANCES && window.FroalaEditor.INSTANCES.length);
      const inst = window.FroalaEditor.INSTANCES[0];
      inst.html.set(task.body_html);
      if (inst.events && inst.events.trigger) inst.events.trigger("contentChanged");
    }

    // Wait a moment for Angular to digest the title and subtitle changes
    // before clicking Save.
    await new Promise((r) => setTimeout(r, 1500));

    const save = Array.from(document.querySelectorAll("button"))
      .find((b) => /^save\s*draft$/i.test((b.textContent || "").trim()));
    if (!save) throw new Error("Save Draft button not found");
    save.click();

    // After save, DZone keeps you on post.html or redirects to drafts list.
    // Wait a few seconds then report.
    await new Promise((r) => setTimeout(r, 6000));
    report(task.id, { success: true, url: location.href });
  } catch (err) {
    console.error("[dzone-adapter] failed", err);
    report(task.id, { success: false, error: String(err) });
  }
})();
