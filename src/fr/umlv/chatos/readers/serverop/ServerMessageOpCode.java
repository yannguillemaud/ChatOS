package fr.umlv.chatos.readers.serverop;

import java.util.Optional;

public enum ServerMessageOpCode {
    SUCCESS((byte) 2),
    FAIL((byte) 3),
    GLOBAL_MESSAGE((byte) 4),
    PERSONAL_MESSAGE((byte) 5),
    PRIVATE_CONNECTION_REQUEST((byte) 6),
    PRIVATE_CONNECTION_ESTABLISHMENT((byte) 7);

    private final byte value;
    ServerMessageOpCode(final byte value) {
        this.value = value;
    }
    public byte value() {
        return this.value;
    }


    public static Optional<ServerMessageOpCode> serverMessageOpCode(byte code) {
        for (ServerMessageOpCode opCode : ServerMessageOpCode.values()) {
            if (opCode.value() == code) {
                return Optional.of(opCode);
            }
        }
        return Optional.empty();
    }
}
