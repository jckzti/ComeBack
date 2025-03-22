import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketServer {
    private static final Set<WsContext> clients = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> users = new ConcurrentHashMap<>(); // username -> password
    private static final Map<String, String> sessionUserMap = new ConcurrentHashMap<>(); // sessionId -> username
    private static final Map<String, List<String>> userHistory = new ConcurrentHashMap<>(); // username -> history

    public static void main(String[] args) {
        var app = Javalin.create().start(7071);

        app.ws("/chat", ws -> {
            ws.onConnect(ctx -> ctx.send("Bem-vindo! Envie seu usuário e senha no formato: usuario:senha"));

            //ws.onMessage(ctx -> handleMessage(ctx));
            ws.onMessage(WebSocketServer::handleMessage);

            ws.onClose(ctx -> {
                String username = sessionUserMap.remove(ctx.sessionId());
                if (username != null) {
                    clients.remove(ctx);
                    broadcast("Usuário " + username + " desconectado. Restantes: " + clients.size());
                }
            });
        });

        System.out.println("Servidor WebSocket rodando em ws://localhost:7071/chat");
    }

    private static void handleMessage(WsMessageContext ctx) {
        String message = ctx.message();

        if (!sessionUserMap.containsKey(ctx.sessionId())) {
            authenticateUser(ctx, message);
            return;
        }

        String username = sessionUserMap.get(ctx.sessionId());
        if (message.startsWith("/")) {
            handleCommand(ctx, username, message);
        } else {
            processCalculation(ctx, username, message);
        }
    }

    private static void authenticateUser(WsContext ctx, String message) {
        String[] parts = message.split(":");
        if (parts.length != 2) {
            ctx.send("Formato inválido. Use usuario:senha");
            return;
        }

        String username = parts[0].trim();
        String password = parts[1].trim();

        if (users.containsKey(username)) {
            if (!users.get(username).equals(password)) {
                ctx.send("Usuário já existe, mas a senha está incorreta. Troque a senha ou escolha outro usuário.");
                return;
            }
        } else {
            users.put(username, password);
            userHistory.put(username, new ArrayList<>());
        }

        sessionUserMap.put(ctx.sessionId(), username);
        clients.add(ctx);
        ctx.send("Login bem-sucedido! Você pode começar a enviar cálculos.");
    }

    private static void processCalculation(WsContext ctx, String username, String message) {
        String[] parts = message.split(" ");
        if (parts.length != 3) {
            ctx.send("Formato inválido. Use: número 1 operador numero 2 (ex: 5 + 3)");
            return;
        }

        try {
            double num1 = Double.parseDouble(parts[0]);
            String operator = parts[1];
            double num2 = Double.parseDouble(parts[2]);
            double result = switch (operator) {
                case "+" -> num1 + num2;
                case "-" -> num1 - num2;
                case "*" -> num1 * num2;
                case "/" -> num2 != 0 ? num1 / num2 : Double.NaN;
                default -> throw new IllegalArgumentException("Operador inválido");
            };

            String response = "Resultado: " + result;
            ctx.send(response);
            userHistory.get(username).add(message + " = " + result);
        } catch (Exception e) {
            ctx.send("Erro ao processar cálculo. Verifique a entrada.");
        }
    }

    private static void handleCommand(WsContext ctx, String username, String command) {
        if (command.equals("/historico") || command.equals("/history") || command.equals("/hist") || command.equals("/h")) {
            List<String> history = userHistory.get(username);
            ctx.send("Seu histórico:\n" + String.join("\n", history));
        } else {
            ctx.send("Comando desconhecido.");
        }
    }

    private static void broadcast(String message) {
        clients.forEach(client -> client.send(message));
    }
}
