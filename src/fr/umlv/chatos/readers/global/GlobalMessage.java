package fr.umlv.chatos.readers.global;

import fr.umlv.chatos.readers.clientop.ClientMessageOpCode;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static fr.umlv.chatos.readers.serverop.ServerMessageOpCode.GLOBAL_MESSAGE;

public class GlobalMessage {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String value;

    public GlobalMessage(String value){
        this.value = value;
    }

    /**
     * Transforms a Message instance into an Optional ByteBuffer if the given size is enough to store it
     * Otherwise returns an empty optional
     * @param maxBufferSize the maximum size of the message
     * @return An optional containing the message if it has enough space, otherwise an empty one
     */
    public Optional<ByteBuffer> toByteBuffer(int maxBufferSize) {
        ByteBuffer encodedValue = UTF8.encode(value);
        int valueSize = encodedValue.remaining();
        int totalSize = Integer.BYTES + valueSize;
        if(totalSize <= maxBufferSize){
            return Optional.of(ByteBuffer.allocate(maxBufferSize)
                    .put(GLOBAL_MESSAGE.value())
                    .putInt(valueSize).put(encodedValue)
                    .flip()
            );
        } else return Optional.empty();
    };

    @Override
    public String toString() {
        return "Global Message: " + value;
    }

    public String getValue(){ return value; }
}