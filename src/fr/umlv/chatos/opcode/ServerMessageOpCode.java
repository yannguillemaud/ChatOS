package fr.umlv.chatos.opcode;

import java.util.Optional;

public enum ServerMessageOpCode {
    SUCCESS,
    FAIL,
    GLOBAL_MESSAGE,
    PERSONAL_MESSAGE,
    PRIVATE_CONNECTION_REQUEST,
    PRIVATE_CONNECTION_ESTABLISHMENT;

    public static Optional<ServerMessageOpCode> serverMessageOpCode(int code) {
        return switch (code) {
            case 2 -> Optional.of(SUCCESS);
            case 3 -> Optional.of(FAIL);
            case 5 -> Optional.of(GLOBAL_MESSAGE);
            case 7 -> Optional.of(PERSONAL_MESSAGE);
            case 9 -> Optional.of(PRIVATE_CONNECTION_REQUEST);
            case 11 -> Optional.of(PRIVATE_CONNECTION_ESTABLISHMENT);
            default -> Optional.empty();
        };
    }
}
