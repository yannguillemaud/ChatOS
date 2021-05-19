package fr.umlv.chatos.readers.clientop;

import java.util.Optional;

public enum ClientMessageOpCode {
    INITIALIZATION((byte) 1),
    GLOBAL_MESSAGE((byte) 4),
    PERSONAL_MESSAGE((byte) 6),
    PRIVATE_CONNECTION_REQUEST((byte) 8),
    PRIVATE_CONNECTION_RESPONSE((byte) 10),
    PRIVATE_CONNECTION_ESTABLISHMENT((byte) 12);

    private final byte value;
    ClientMessageOpCode(final byte value) {
        this.value = value;
    }
    public int value() {
        return this.value;
    }


    public static Optional<ClientMessageOpCode> clientMessageOpCode(byte code) {
        for (ClientMessageOpCode opCode : ClientMessageOpCode.values()) {
            if (opCode.value() == code) {
                return Optional.of(opCode);
            }
        }
        return Optional.empty();
    }
}
