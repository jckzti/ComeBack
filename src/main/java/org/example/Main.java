package org.example;

import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    private static Map<String, Object> createJsonResponse(String key, Object value) {
        return Map.of(key, value);
    }

    private static Map<String, Object> createJsonResponse(String value) {
        return createJsonResponse("response", value);
    }

    public static void main(String[] args) {
        var app = Javalin.create()
                .get("/", ctx -> ctx.result("Bem-vindo à API Javalin!"))
                .start(7070);

        Map<Integer, String> users = new HashMap<>();
        AtomicInteger userIdCounter = new AtomicInteger(1);

        app.get("/users", ctx -> ctx.json(users));

        app.post("/users", ctx -> {
            String name = ctx.body();
            int id = userIdCounter.getAndIncrement();
            users.put(id, name);
            ctx.json(createJsonResponse("Usuário criado"));

        });

        app.get("/users/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            String user = users.get(id);
            if (user == null) {
                ctx.json(createJsonResponse("Usuário não encontrado"));
                return;

            }
            ctx.json(createJsonResponse("user", Map.of("id", id, "Nome", user)));
        });

        app.delete("/users/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            if (users.remove(id) == null) {
                ctx.json(createJsonResponse("Usuário não encontrado"));
                return;
            }
            ctx.result("Usuário removido com sucesso");
        });

        app.get("/time", ctx -> {
            ctx.json(createJsonResponse("Hora do servidor: " + LocalTime.now()));
        });
    }
}
