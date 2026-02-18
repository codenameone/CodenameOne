export async function onRequest(context) {
  const url = new URL(context.request.url);
  const path = url.pathname;

  // Keep /developer-guide.html as canonical public URL.
  if (path === "/manual" || path === "/manual/") {
    return Response.redirect(`${url.origin}/developer-guide.html`, 301);
  }
  if (path === "/developer-guide.html") {
    const guideUrl = new URL(url);
    guideUrl.pathname = "/developer-guide-content/";
    return context.next(new Request(guideUrl.toString(), context.request));
  }

  // Let static assets and _redirects run first.
  const response = await context.next();
  if (response.status !== 404) {
    return response;
  }

  // 404 fallback for /files: prefer local files first, then redirect to download host.
  if (path === "/files" || path === "/files/") {
    return Response.redirect("https://download.codenameone.com/files", 302);
  }
  if (path.startsWith("/files/")) {
    const suffix = path.slice("/files/".length);
    if (suffix) {
      // download.codenameone.com stores these assets at root.
      return Response.redirect(
        `https://download.codenameone.com/${suffix}${url.search}`,
        302,
      );
    }
  }

  // 404 fallback for /demos: keep local /demos page/assets first, then fallback.
  if (path === "/demos" || path === "/demos/") {
    return Response.redirect("https://download.codenameone.com/demos", 302);
  }
  if (path.startsWith("/demos/")) {
    const suffix = path.slice("/demos/".length);
    if (suffix) {
      return Response.redirect(
        `https://download.codenameone.com/demos/${suffix}${url.search}`,
        302,
      );
    }
  }

  return response;
}
