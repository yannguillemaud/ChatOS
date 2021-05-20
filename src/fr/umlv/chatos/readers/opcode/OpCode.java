package fr.umlv.chatos.readers.opcode;

import java.util.Optional;

public enum OpCode {
    INITIALIZATION((byte) 1),
    SUCCESS((byte) 2),
    FAIL((byte) 3),
    CLIENT_GLOBAL_MESSAGE((byte) 4),
    SERVER_GLOBAL_MESSAGE((byte) 5),
    PERSONAL_MESSAGE((byte) 6),
    PRIVATE_CONNECTION_REQUEST((byte) 7),
    PRIVATE_CONNECTION_ACCEPTATION_REQUEST((byte) 8),
    PRIVATE_CONNECTION_RESPONSE((byte) 9),
    PRIVATE_CONNECTION_SERVER_ESTABLISHMENT((byte) 10),
    PRIVATE_CONNECTION_CLIENT_ESTABLISHMENT((byte) 11);

    private final byte value;
    OpCode(final byte value) {
        this.value = value;
    }
    public byte value() {
        return this.value;
    }


    public static Optional<OpCode> opCode(byte code) {
        for (OpCode opCode : OpCode.values()) {
            if (opCode.value() == code) {
                return Optional.of(opCode);
            }
        }
        return Optional.empty();
    }
}
