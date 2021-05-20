package fr.umlv.chatos.readers.global;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static fr.umlv.chatos.readers.opcode.OpCode.GLOBAL_MESSAGE;

public class GlobalMessage {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String value;
    private final String author;

    public GlobalMessage(String value, String author) {
        this.value = value;
        this.author = author;
    }

    public GlobalMessage(String value){
        this(value, null);
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
        if (author == null) {
            int totalSize = Byte.BYTES + Integer.BYTES + valueSize;
            if(totalSize <= maxBufferSize){
                return Optional.of(ByteBuffer.allocate(maxBufferSize)
                        .put(GLOBAL_MESSAGE.value())
                        .putInt(valueSize).put(encodedValue)
                        .flip()
                );
            }
        } else {
            ByteBuffer encodedAuthor = UTF8.encode(author);
            int authorSize = encodedAuthor.remaining();
            int totalSize = Byte.BYTES + Integer.BYTES * 2 + valueSize + authorSize;
            if(totalSize <= maxBufferSize){
                return Optional.of(ByteBuffer.allocate(maxBufferSize)
                        .put(GLOBAL_MESSAGE.value())
                        .putInt(valueSize).put(encodedValue)
                        .putInt(authorSize).put(encodedAuthor)
                        .flip()
                );
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "Global Message : " + value + (author == null ? " from " + author : "");
    }

    public String getValue(){ return value; }

    public boolean hasAuthor() {
        return author != null;
    }

    public String getAuthor() {
        if (!hasAuthor()) {
            throw new IllegalStateException();
        }
        return author;
    }
}