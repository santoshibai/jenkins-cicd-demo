package com.example.jenkins;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class App {

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/health", exchange -> {
            String response = "Application is healthy";
            exchange.sendResponseHeaders(200, response.length());

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response.getBytes());
            }
        });

        server.start();

        System.out.println("Jenkins CI/CD Demo Application is running!");
        System.out.println("SCRUM-8: GitHub and Jira integration demo completed.");
    }
}