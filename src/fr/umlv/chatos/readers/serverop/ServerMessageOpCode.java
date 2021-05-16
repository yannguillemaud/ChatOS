package fr.umlv.chatos.readers.serverop;

import java.util.Optional;

public enum ServerMessageOpCode {
    SUCCESS(2),
    FAIL(3),
    GLOBAL_MESSAGE(5),
    PERSONAL_MESSAGE(7),
    PRIVATE_CONNECTION_REQUEST(9),
    PRIVATE_CONNECTION_ESTABLISHMENT(11);

    private final int value;
    ServerMessageOpCode(final int value) {
        this.value = value;
    }
    public int value() {
        return this.value;
    }


    public static Optional<ServerMessageOpCode> serverMessageOpCode(int code) {
        for (ServerMessageOpCode opCode : ServerMessageOpCode.values()) {
            if (opCode.value() == code) {
                return Optional.of(opCode);
            }
        }
        return Optional.empty();
    }
}
