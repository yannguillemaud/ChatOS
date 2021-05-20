package fr.umlv.chatos.server.token;

import java.util.UUID;

public class TokenGenerator {
    static String token() {
        StringBuilder token = new StringBuilder();
        long currentTimeMillis = System.currentTimeMillis();
        return token
                .append(currentTimeMillis)
                .append("-")
                .append(UUID.randomUUID().toString())
                .toString();
    }
}
