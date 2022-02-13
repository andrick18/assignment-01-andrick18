import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

class Arguments {
    private final int port;
    private final String directory;
    private final int responses;

    Arguments(int port, String directory, int responses) {
        assert(port >= 0);
        assert(port <= 65535);
        assert(directory != null);
        assert(!directory.isEmpty());
        assert(responses >= 0);

        this.port = port;
        this.directory = directory;
        this.responses = responses;
    }

    int getPort() { return port; }
    String getDirectory() { return directory; }
    int getResponses() { return responses; }
}

class MyHttpHandler implements HttpHandler {
    private final Arguments arguments;
    private static int count;

    MyHttpHandler(Arguments arguments) {
        this.arguments = arguments;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        File file = new File(arguments.getDirectory(), httpExchange.getRequestURI().getPath());
        if(file.exists() && file.isFile()) {
            fileFound(httpExchange, file.length(), new FileInputStream(file));
        }
        else {
            fileNotFound(httpExchange);
        }
    }

    private void fileNotFound(HttpExchange httpExchange) throws IOException {
        String response = "404.\nThe requested URL " + httpExchange.getRequestURI()
                + " was not found on this server.";
        httpExchange.sendResponseHeaders(404, response.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void fileFound(HttpExchange httpExchange, long length, FileInputStream in) throws IOException {
        httpExchange.sendResponseHeaders(200, length);
        OutputStream os = httpExchange.getResponseBody();
        try {
            byte[] buf = new byte [16 * 1024];
            int len;
            while ((len=in.read(buf)) != -1) {
                os.write (buf, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        in.close();
        os.close();
    }
}

public class Main {
    static Arguments parseArgs(String[] args) {
        int port = -1;
        String directory = null;
        int responses = 0;
        for(int i = 0; i < args.length; i += 2) {
            if(args[i].equals("--port")) {
                port = Integer.parseInt(args[i + 1]);
            }
            else if(args[i].equals("--directory")) {
                directory = args[i + 1];
            }
            else if(args[i].equals("--responses")) {
                responses = Integer.parseInt(args[i + 1]);
            }
            else {
                throw new IllegalArgumentException();
            }
        }
        return new Arguments(port, directory, responses);
    }

    public static void main(String[] args) throws IOException {
        Arguments arguments = parseArgs(args);
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(arguments.getPort()), 0);
        httpServer.createContext("/", new MyHttpHandler(arguments));
        httpServer.setExecutor(Executors.newSingleThreadExecutor());
        httpServer.start();
    }
}