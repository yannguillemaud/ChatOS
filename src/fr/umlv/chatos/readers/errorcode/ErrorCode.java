package fr.umlv.chatos.readers.errorcode;

import fr.umlv.chatos.readers.trame.Trame;

import java.nio.ByteBuffer;
import java.util.Optional;

import static fr.umlv.chatos.readers.opcode.OpCode.FAIL;

public enum ErrorCode implements Trame {
    UNDEFINED((byte) 0),
    EMPTY_PSEUDO((byte) 1),
    ALREADY_USED((byte) 2),
    NOT_LINKED((byte) 3),
    TOO_LONG_MESSAGE((byte) 4),
    CONNEXION_DECLINED((byte) 5),
    CONNEXION_TIMEOUT((byte) 6),
    CONNECTION_NOT_INITIALIZED((byte) 7),
    PRIVATE_CONNECTION_DEMAND_NOT_INITIATED((byte) 8),
    PRIVATE_CONNECTION_ALREADY_ESTABLISHED((byte) 9),
    PRIVATE_CONNECTION_ALREADY_INITIATED((byte) 10),
    CONNECTION_ALREADY_INITIALIZED((byte) 7);

    private final byte value;
    ErrorCode(final byte value) {
        this.value = value;
    }
    public byte value() {
        return this.value;
    }

    public static Optional<ErrorCode> serverErrorCode(byte value){
        for(var opcode : ErrorCode.values()){
            if(value == opcode.value) return Optional.of(opcode);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ByteBuffer> toByteBuffer(int maxBufferSize) {
        return  Optional.of(ByteBuffer.allocate(Byte.BYTES * 2)
                .put(FAIL.value())
                .put(this.value())
                .flip()
        );
    }
}
