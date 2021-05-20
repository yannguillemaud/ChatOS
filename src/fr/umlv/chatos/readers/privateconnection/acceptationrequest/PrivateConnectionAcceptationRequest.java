package fr.umlv.chatos.readers.privateconnection.acceptationrequest;

import fr.umlv.chatos.readers.Sendable;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static fr.umlv.chatos.readers.opcode.OpCode.PRIVATE_CONNECTION_ACCEPTATION_REQUEST;

public class PrivateConnectionAcceptationRequest implements Sendable {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String login;

    public PrivateConnectionAcceptationRequest(String login){
        this.login = login;
    }

    /**
     * Transforms a PrivateConnectionAcceptationRequest instance into an Optional ByteBuffer if the given size is enough to store it
     * Otherwise returns an empty optional
     * @param maxBufferSize the maximum size of the message
     * @return An optional containing the message if it has enough space, otherwise an empty one
     */
    public Optional<ByteBuffer> toByteBuffer(int maxBufferSize) {
        ByteBuffer encodedLogin = UTF8.encode(login);
        int loginSize = encodedLogin.remaining();
        int totalSize = Byte.BYTES + Integer.BYTES + loginSize;
        if(totalSize <= maxBufferSize){
            return Optional.of(ByteBuffer.allocate(maxBufferSize)
                    .put(PRIVATE_CONNECTION_ACCEPTATION_REQUEST.value())
                    .putInt(loginSize).put(encodedLogin)
                    .flip()
            );
        } else return Optional.empty();
    }

    @Override
    public String toString() {
        return "Private connection acceptation request to : " + login;
    }

    public String getLogin(){ return login; }
}
