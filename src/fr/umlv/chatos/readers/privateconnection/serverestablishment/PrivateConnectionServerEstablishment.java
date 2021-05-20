package fr.umlv.chatos.readers.privateconnection.serverestablishment;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static fr.umlv.chatos.readers.opcode.OpCode.PRIVATE_CONNECTION_REQUEST;

public class PrivateConnectionServerEstablishment {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String login;
    private final String token;

    public PrivateConnectionServerEstablishment(String login, String token){
        this.login = login;
        this.token = token;
    }

    /**
     * Transforms a PrivateConnectionRequest instance into an Optional ByteBuffer if the given size is enough to store it
     * Otherwise returns an empty optional
     * @param maxBufferSize the maximum size of the message
     * @return An optional containing the message if it has enough space, otherwise an empty one
     */
    public Optional<ByteBuffer> toByteBuffer(int maxBufferSize) {
        ByteBuffer encodedLogin = UTF8.encode(login);
        ByteBuffer encodedToken = UTF8.encode(token);
        int loginSize = encodedLogin.remaining();
        int tokenSize = encodedToken.remaining();
        int totalSize = Byte.BYTES + Integer.BYTES * 2 + loginSize + tokenSize;
        if(totalSize <= maxBufferSize){
            return Optional.of(ByteBuffer.allocate(maxBufferSize)
                    .put(PRIVATE_CONNECTION_REQUEST.value())
                    .putInt(loginSize).put(encodedLogin)
                    .putInt(tokenSize).put(encodedToken)
                    .flip()
            );
        } else return Optional.empty();
    }

    @Override
    public String toString() {
        return "Private connection server establishment, token : " + token + " (" + login + ")";
    }

    public String getLogin(){ return login; }

    public String getToken(){ return token; }
}