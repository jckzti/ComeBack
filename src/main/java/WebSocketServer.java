import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WebSocketServer {
    private static final Set<WsContext> clients = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        var app = Javalin.create().start(7071);

        app.ws("/chat", ws -> {
            ws.onConnect(ctx -> {
                clients.add(ctx);
                broadcast("Novo usuário conectado. Total: " + clients.size());
            });

            ws.onMessage((ctx) -> {
                broadcast("Mensagem recebida: " + ctx.message());
            });

            ws.onClose(ctx -> {
                clients.remove(ctx);
                broadcast("Usuário desconectado. Restantes: " + clients.size());
            });
        });

        System.out.println("Servidor WebSocket rodando em ws://localhost:7070/chat");
    }

    private static void broadcast(String message) {
        clients.forEach(client -> client.send(message));
    }
}
