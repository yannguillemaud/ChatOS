package fr.umlv.chatos.readers.clientop;

import java.util.Optional;

public enum ClientMessageOpCode {
    INITIALIZATION(1),
    GLOBAL_MESSAGE(4),
    PERSONAL_MESSAGE(6),
    PRIVATE_CONNECTION_REQUEST(8),
    PRIVATE_CONNECTION_RESPONSE(10),
    PRIVATE_CONNECTION_ESTABLISHMENT(12);

    private final int value;
    ClientMessageOpCode(final int value) {
        this.value = value;
    }
    public int value() {
        return this.value;
    }


    public static Optional<ClientMessageOpCode> clientMessageOpCode(int code) {
        for (ClientMessageOpCode opCode : ClientMessageOpCode.values()) {
            if (opCode.value() == code) {
                return Optional.of(opCode);
            }
        }
        return Optional.empty();
    }
}
