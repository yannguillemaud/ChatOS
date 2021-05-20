package fr.umlv.chatos.readers.servererrorcode;

import java.util.Optional;

public enum ServerErrorCode {
    UNDEFINED((byte) 0),
    EMPTY_PSEUDO((byte) 1),
    TOO_LONG_PSEUDO((byte) 2),
    ALREADY_USED((byte) 3),
    NOT_LINKED((byte) 4),
    TOO_LONG_MESSAGE((byte) 5),
    CONNEXION_DECLINED((byte) 6),
    CONNEXION_TIMEOUT((byte) 7);

    private final byte value;
    ServerErrorCode(final byte value) {
        this.value = value;
    }
    public byte value() {
        return this.value;
    }

    public static Optional<ServerErrorCode> serverErrorCode(byte value){
        for(var opcode : ServerErrorCode.values()){
            if(value == opcode.value) return Optional.of(opcode);
        }
        return Optional.empty();
    }
}
