package fr.umlv.chatos.readers.serverop;

import java.util.Optional;

public enum ServerErrorCode {
    UNDEFINED(0),
    EMPTY_PSEUDO(1),
    TOO_LONG_PSEUDO(2),
    ALREADY_USED(3),
    NOT_LINKED(4),
    TOO_LONG_MESSAGE(5),
    CONNEXION_DECLINED(6),
    CONNEXION_TIMEOUT(7);

    private final int value;
    ServerErrorCode(final int value) {
        this.value = value;
    }
    public int value() {
        return this.value;
    }

    public static Optional<ServerErrorCode> serverErrorCode(int value){
        for(var opcode : ServerErrorCode.values()){
            if(value == opcode.value) return Optional.of(opcode);
        }
        return Optional.empty();
    }
}
