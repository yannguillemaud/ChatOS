package fr.umlv.chatos.readers.global;

import fr.umlv.chatos.Sendable;
import fr.umlv.chatos.readers.clientop.ClientMessageOpCode;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class GlobalMessage implements Sendable {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String from;
    private final String value;

    public GlobalMessage(String from, String value){
        this.from = requireNonNull(from);
        this.value = requireNonNull(value);
    }

    /**
     * Transforms a Message instance into an Optional ByteBuffer if the given size is enough to store it
     * Otherwise returns an empty optional
     * @param maxBufferSize the maximum size of the message
     * @return An optional containing the message if it has enough space, otherwise an empty one
     */
    public Optional<ByteBuffer> toByteBuffer(int maxBufferSize) {
        ByteBuffer encodedLogin = UTF8.encode(from);
        ByteBuffer encodedValue = UTF8.encode(value);
        int loginSize = encodedLogin.remaining();
        int valueSize = encodedValue.remaining();
        int totalSize = Byte.BYTES + Integer.BYTES * 2  + loginSize + valueSize;
        if(totalSize <= maxBufferSize){
            return Optional.of(ByteBuffer.allocate(maxBufferSize)
                    .put(ClientMessageOpCode.GLOBAL_MESSAGE.value())
                    .putInt(loginSize).put(encodedLogin)
                    .putInt(valueSize).put(encodedValue)
                    .flip()
            );
        } else return Optional.empty();
    };

    @Override
    public String toString() {
        return "Global from " + from + ": " + value;
    }

    public String getFrom(){ return from; }
    public String getValue(){ return value; }
}