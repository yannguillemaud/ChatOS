package fr.umlv.chatos.readers.clientglobal;

import fr.umlv.chatos.readers.Sendable;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static fr.umlv.chatos.readers.opcode.OpCode.GLOBAL_MESSAGE_CLIENT;

public class ClientGlobalMessage implements Sendable {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String value;

    public ClientGlobalMessage(String value) {
        this.value = value;
    }

    /**
     * Transforms a GlobalMessage instance into an Optional ByteBuffer if the given size is enough to store it
     * Otherwise returns an empty optional
     * @param maxBufferSize the maximum size of the message
     * @return An optional containing the message if it has enough space, otherwise an empty one
     */
    public Optional<ByteBuffer> toByteBuffer(int maxBufferSize) {
        ByteBuffer encodedValue = UTF8.encode(value);
        int valueSize = encodedValue.remaining();
        int totalSize = Byte.BYTES + Integer.BYTES + valueSize;
        if(totalSize <= maxBufferSize){
            return Optional.of(ByteBuffer.allocate(maxBufferSize)
                    .put(GLOBAL_MESSAGE_CLIENT.value())
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