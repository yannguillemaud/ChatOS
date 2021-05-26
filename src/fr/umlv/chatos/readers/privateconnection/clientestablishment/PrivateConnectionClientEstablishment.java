package fr.umlv.chatos.readers.privateconnection.clientestablishment;

import fr.umlv.chatos.readers.trame.Trame;
import fr.umlv.chatos.visitor.Visitor;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static fr.umlv.chatos.readers.opcode.OpCode.PRIVATE_CONNECTION_CLIENT_ESTABLISHMENT;

public class PrivateConnectionClientEstablishment implements Trame {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String token;

    public PrivateConnectionClientEstablishment(String token){
        this.token = token;
    }

    /**
     * Transforms a PrivateConnectionRequest instance into an Optional ByteBuffer if the given size is enough to store it
     * Otherwise returns an empty optional
     * @param maxBufferSize the maximum size of the message
     * @return An optional containing the message if it has enough space, otherwise an empty one
     */
    public Optional<ByteBuffer> asByteBuffer(int maxBufferSize) {
        ByteBuffer encodedToken = UTF8.encode(token);
        int tokenSize = encodedToken.remaining();
        int totalSize = Byte.BYTES + Integer.BYTES + tokenSize;
        if(totalSize <= maxBufferSize){
            return Optional.of(ByteBuffer.allocate(maxBufferSize)
                    .put(PRIVATE_CONNECTION_CLIENT_ESTABLISHMENT.value())
                    .putInt(tokenSize).put(encodedToken)
                    .flip()
            );
        } else return Optional.empty();
    }

    @Override
    public void accept(Visitor serverVisitor) {

    }

    @Override
    public String toString() {
        return "Private connection client establishment, token : " + token;
    }

    public String getToken(){ return token; }
}
