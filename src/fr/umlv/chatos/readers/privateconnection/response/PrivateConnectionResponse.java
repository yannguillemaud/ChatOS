package fr.umlv.chatos.readers.privateconnection.response;

import fr.umlv.chatos.readers.Sendable;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static fr.umlv.chatos.readers.opcode.OpCode.PRIVATE_CONNECTION_RESPONSE;

public class PrivateConnectionResponse implements Sendable {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String login;
    private final boolean acceptPrivateConnection;

    public PrivateConnectionResponse(String login, boolean acceptPrivateConnection){
        this.login = login;
        this.acceptPrivateConnection = acceptPrivateConnection;
    }

    /**
     * Transforms a PrivateConnectionResponse instance into an Optional ByteBuffer if the given size is enough to store it
     * Otherwise returns an empty optional
     * @param maxBufferSize the maximum size of the message
     * @return An optional containing the message if it has enough space, otherwise an empty one
     */
    public Optional<ByteBuffer> toByteBuffer(int maxBufferSize) {
        ByteBuffer encodedLogin = UTF8.encode(login);
        int loginSize = encodedLogin.remaining();
        int totalSize = Byte.BYTES * 2 + Integer.BYTES + loginSize;
        if(totalSize <= maxBufferSize){
            return Optional.of(ByteBuffer.allocate(maxBufferSize)
                    .put(PRIVATE_CONNECTION_RESPONSE.value())
                    .putInt(loginSize).put(encodedLogin)
                    .put((byte)(acceptPrivateConnection ? 1 : 0))
                    .flip()
            );
        } else return Optional.empty();
    }

    @Override
    public String toString() {
        return "Request private connection with : " + login + " (" + acceptPrivateConnection + ")";
    }

    public String getLogin(){ return login; }

    public boolean getAcceptPrivateConnection(){ return acceptPrivateConnection; }
}
