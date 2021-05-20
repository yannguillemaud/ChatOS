package fr.umlv.chatos.readers.personal;

import fr.umlv.chatos.Sendable;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import static fr.umlv.chatos.readers.serverop.ServerMessageOpCode.PERSONAL_MESSAGE;
import static java.util.Objects.requireNonNull;

public class PersonalMessage implements Sendable {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String from;
    private final String to;
    private final String value;

    public PersonalMessage(String from, String to, String value){
        this.from = requireNonNull(from);
        this.to = requireNonNull(to);
        this.value = requireNonNull(value);
    }

    /**
     * Transforms a Message instance into an Optional ByteBuffer if the given size is enough to store it
     * Otherwise returns an empty optional
     * @param maxBufferSize the maximum size of the message
     * @return An optional containing the message if it has enough space, otherwise an empty one
     */
    public Optional<ByteBuffer> toByteBuffer(int maxBufferSize) {
        ByteBuffer encodedFrom = UTF8.encode(from);
        ByteBuffer encodedTo = UTF8.encode(to);
        ByteBuffer encodedValue = UTF8.encode(value);

        int fromSize = encodedFrom.remaining();
        int toSize = encodedTo.remaining();
        int valueSize = encodedValue.remaining();
        int totalSize = Integer.BYTES + Integer.BYTES * 3 + fromSize + toSize + valueSize;

        if(totalSize <= maxBufferSize){
            return Optional.of(ByteBuffer.allocate(maxBufferSize)
                    .put(PERSONAL_MESSAGE.value())
                    .putInt(fromSize).put(encodedFrom)
                    .putInt(toSize).put(encodedTo)
                    .putInt(valueSize).put(encodedValue)
                    .flip()
            );
        } else return Optional.empty();
    };

    @Override
    public String toString() {
        return "Personal Message from " + from + " to " + to + ": " + value;
    }

    public String getFrom() { return from; }
    public String getTo(){ return to; }
    public String getValue(){ return value; }
}