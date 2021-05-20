package fr.umlv.chatos.readers;

import java.nio.ByteBuffer;
import java.util.Optional;

public interface Sendable {
    Optional<ByteBuffer> toByteBuffer(int maxBufferSize);
}
