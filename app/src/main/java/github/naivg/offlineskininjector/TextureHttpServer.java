package github.naivg.offlineskininjector;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * A small loopback-only HTTP server that serves the replacement textures.
 *
 * <p>Each key in the resource map is an absolute request path, for example
 * {@code /skin/<sha1>.png}.
 */
public final class TextureHttpServer {

    private final ServerSocket socket;
    private final Map<String, byte[]> resources;

    private TextureHttpServer(ServerSocket socket, Map<String, byte[]> resources) {
        this.socket = socket;
        this.resources = resources;
    }

    /**
     * Starts the server.
     *
     * @param resources the served resources, keyed by their absolute request path
     */
    public static TextureHttpServer start(Map<String, byte[]> resources) throws IOException {
        ServerSocket socket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        Map<String, byte[]> copy = new HashMap<String, byte[]>();
        if (resources != null) {
            copy.putAll(resources);
        }
        final TextureHttpServer server = new TextureHttpServer(socket, copy);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                server.serve();
            }
        }, "OfflineSkinInjector-HTTP");
        thread.setDaemon(true);
        thread.start();
        return server;
    }

    public int port() {
        return socket.getLocalPort();
    }

    /** The base URL of the server, without any resource path. */
    public String baseUrl() {
        return "http://127.0.0.1:" + port();
    }

    /** The absolute URL of one of the served resources. */
    public String url(String path) {
        return baseUrl() + path;
    }

    private void serve() {
        while (!socket.isClosed()) {
            Socket client = null;
            try {
                client = socket.accept();
                handle(client);
            } catch (Throwable t) {
                // A broken connection must not kill the server.
            } finally {
                if (client != null) {
                    try {
                        client.close();
                    } catch (IOException ignored) {
                        // Ignore close failures.
                    }
                }
            }
        }
    }

    private void handle(Socket client) throws IOException {
        client.setSoTimeout(5000);
        InputStream in = client.getInputStream();
        String requestLine = readLine(in);
        if (requestLine == null) {
            return;
        }
        // Drain the remaining headers so the client does not see a broken pipe.
        String line;
        while ((line = readLine(in)) != null && !line.isEmpty()) {
            // Discard headers.
        }

        String[] parts = requestLine.split(" ");
        String requested = parts.length > 1 ? parts[1] : "";
        int query = requested.indexOf('?');
        if (query >= 0) {
            requested = requested.substring(0, query);
        }

        OutputStream raw = client.getOutputStream();
        BufferedOutputStream out = new BufferedOutputStream(raw);
        byte[] png = resources.get(requested);
        if (png != null) {
            out.write(("HTTP/1.1 200 OK\r\n"
                    + "Content-Type: image/png\r\n"
                    + "Content-Length: " + png.length + "\r\n"
                    + "Cache-Control: no-store\r\n"
                    + "Connection: close\r\n\r\n").getBytes("ISO-8859-1"));
            out.write(png);
        } else {
            out.write(("HTTP/1.1 404 Not Found\r\n"
                    + "Content-Length: 0\r\n"
                    + "Connection: close\r\n\r\n").getBytes("ISO-8859-1"));
        }
        out.flush();
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') {
                int len = sb.length();
                if (len > 0 && sb.charAt(len - 1) == '\r') {
                    sb.setLength(len - 1);
                }
                return sb.toString();
            }
            sb.append((char) c);
            if (sb.length() > 8192) {
                return null;
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
