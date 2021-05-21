package fr.umlv.chatos.readers.trame;

import java.nio.ByteBuffer;
import java.util.Optional;

public interface Trame {
    Optional<ByteBuffer> toByteBuffer(int maxBufferSize);
}
