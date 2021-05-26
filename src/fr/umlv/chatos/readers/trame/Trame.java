package fr.umlv.chatos.readers.trame;

import fr.umlv.chatos.server.ServerFrameVisitor;
import fr.umlv.chatos.visitor.Visitor;

import java.nio.ByteBuffer;
import java.util.Optional;

public interface Trame {
    /**
     * Transforms a Trame instance into an Optional ByteBuffer if the given size is enough to store it
     * Otherwise returns an empty optional
     * @param maxBufferSize the maximum size of the message
     * @return An optional containing the message if it has enough space, otherwise an empty one
     */
    Optional<ByteBuffer> asByteBuffer(int maxBufferSize);
    void accept(Visitor serverVisitor);
}
