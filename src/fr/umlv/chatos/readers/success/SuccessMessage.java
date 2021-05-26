package fr.umlv.chatos.readers.success;

import fr.umlv.chatos.readers.trame.Trame;
import fr.umlv.chatos.visitor.Visitor;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static fr.umlv.chatos.readers.opcode.OpCode.INITIALIZATION;
import static fr.umlv.chatos.readers.opcode.OpCode.SUCCESS;

public class SuccessMessage implements Trame {

    /**
     * Transforms an InitializationMessage instance into an Optional ByteBuffer if the given size is enough to store it
     * Otherwise returns an empty optional
     * @param maxBufferSize the maximum size of the message
     * @return An optional containing the message if it has enough space, otherwise an empty one
     */
    public Optional<ByteBuffer> asByteBuffer(int maxBufferSize) {
        return Optional.of(
                ByteBuffer.allocate(Byte.BYTES)
                        .put(SUCCESS.value())
                        .flip()
        );
    }

    @Override
    public void accept(Visitor serverVisitor) {
        serverVisitor.visit(this);
    }

    @Override
    public String toString() {
        return "Success";
    }

}