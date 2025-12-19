/*
 * Lightweight URL implementation suitable for JavaAPI usage.
 */
package java.net;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.MalformedURLException;

public class URL {

    private final URI uri;

    public URL(String spec) throws MalformedURLException {
        this.uri = createUri(spec);
    }

    public URL(String protocol, String host, int port, String file) throws MalformedURLException {
        this(build(protocol, host, port, file));
    }

    public URL(String protocol, String host, String file) throws MalformedURLException {
        this(protocol, host, -1, file);
    }

    public URL(URL context, String spec) throws MalformedURLException {
        if (context == null) {
            this.uri = createUri(spec);
        } else {
            try {
                this.uri = context.uri.resolve(new URI(spec));
            } catch (URISyntaxException ex) {
                throw new MalformedURLException(ex.getMessage());
            }
        }
    }

    public URL(URL context, String spec, URLStreamHandler handler) throws MalformedURLException {
        this(context, spec);
    }

    private static URI createUri(String spec) throws MalformedURLException {
        try {
            return new URI(spec);
        } catch (URISyntaxException ex) {
            throw new MalformedURLException(ex.getMessage());
        }
    }

    private static String build(String protocol, String host, int port, String file) {
        StringBuilder sb = new StringBuilder();
        if (protocol != null) {
            sb.append(protocol).append(URI.SCHEME_SEPARATOR);
        }
        if (host != null) {
            sb.append(host);
        }
        if (port >= 0) {
            sb.append(":").append(port);
        }
        if (file != null) {
            if (!file.startsWith("/")) {
                sb.append("/");
            }
            sb.append(file);
        }
        return sb.toString();
    }

    public final InputStream openStream() throws IOException {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return new FileInputStream(uri.getPath());
        }
        throw new IOException("openStream not supported for scheme " + uri.getScheme());
    }

    public String toString() {
        return uri.toString();
    }
}
