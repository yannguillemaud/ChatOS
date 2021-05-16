package fr.umlv.chatos.readers.initialization;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class InitializationMessage {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String login;

    public InitializationMessage(String login){
        this.login = login;
    }

    /**
     * Transforms a Message instance into an Optional ByteBuffer if the given size is enough to store it
     * Otherwise returns an empty optional
     * @param maxBufferSize the maximum size of the message
     * @return An optional containing the message ByteBuffer in write mode if it has enough space, otherwise an empty one
     */
    public Optional<ByteBuffer> toByteBuffer(int maxBufferSize) {
        ByteBuffer encodedLogin = UTF8.encode(login);
        int loginSize = encodedLogin.remaining();
        int totalSize = Integer.BYTES + loginSize;
        if(totalSize <= maxBufferSize){
            return Optional.of(ByteBuffer.allocate(maxBufferSize)
                    .putInt(loginSize).put(encodedLogin)
                    .flip()
            );
        } else return Optional.empty();
    };

    @Override
    public String toString() {
        return login;
    }

    public String getLogin(){ return login; }
}