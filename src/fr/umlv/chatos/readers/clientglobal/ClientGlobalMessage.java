package fr.umlv.chatos.readers.clientglobal;

import fr.umlv.chatos.readers.trame.Trame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static fr.umlv.chatos.readers.opcode.OpCode.CLIENT_GLOBAL_MESSAGE;

public class ClientGlobalMessage implements Trame {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String value;

    public ClientGlobalMessage(String value) {
        this.value = value;
    }

    @Override
    public Optional<ByteBuffer> toByteBuffer(int maxBufferSize) {
        ByteBuffer encodedValue = UTF8.encode(value);
        int valueSize = encodedValue.remaining();
        int totalSize = Byte.BYTES + Integer.BYTES + valueSize;
        if(totalSize <= maxBufferSize){
            return Optional.of(ByteBuffer.allocate(maxBufferSize)
                    .put(CLIENT_GLOBAL_MESSAGE.value())
                    .putInt(valueSize).put(encodedValue)
                    .flip()
            );
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "Global Message : " + value;
    }

    public String getValue(){ return value; }
}