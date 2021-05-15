package fr.umlv.chatos.opcode;

import java.util.Optional;

public enum ClientMessageOpCode {
    INITIALIZATION,
    GLOBAL_MESSAGE,
    PERSONAL_MESSAGE,
    PRIVATE_CONNECTION_REQUEST,
    PRIVATE_CONNECTION_RESPONSE,
    PRIVATE_CONNECTION_ESTABLISHMENT;

    public static Optional<ClientMessageOpCode> clientMessageOpCode(int code) {
        return switch (code) {
            case 1 -> Optional.of(INITIALIZATION);
            case 4 -> Optional.of(GLOBAL_MESSAGE);
            case 6 -> Optional.of(PERSONAL_MESSAGE);
            case 8 -> Optional.of(PRIVATE_CONNECTION_REQUEST);
            case 10 -> Optional.of(PRIVATE_CONNECTION_RESPONSE);
            case 12 -> Optional.of(PRIVATE_CONNECTION_ESTABLISHMENT);
            default -> Optional.empty();
        };
    }
}
