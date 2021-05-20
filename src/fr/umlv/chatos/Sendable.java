package fr.umlv.chatos;

import java.nio.ByteBuffer;
import java.util.Optional;

public interface Sendable {
    Optional<ByteBuffer> toByteBuffer(int maxBufferSize);
}
