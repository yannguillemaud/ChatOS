package fr.umlv.chatos.readers.initialization;

import fr.umlv.chatos.Sendable;
import fr.umlv.chatos.readers.clientop.ClientMessageOpCode;
import fr.umlv.chatos.readers.personal.PersonalMessage;
import fr.umlv.chatos.readers.serverop.ServerErrorCode;
import fr.umlv.chatos.readers.serverop.ServerMessageOpCode;
import fr.umlv.chatos.server.ChatOSServer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static fr.umlv.chatos.readers.opcode.OpCode.INITIALIZATION;

public class InitializationMessage implements Sendable {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String login;

    public InitializationMessage(String login){
        this.login = login;
    }

    /**
     * Transforms an InitializationMessage instance into an Optional ByteBuffer if the given size is enough to store it
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
                    .put(INITIALIZATION.value())
                    .putInt(loginSize).put(encodedLogin)
                    .flip()
            );
        } else return Optional.empty();
    }

    @Override
    public String toString() {
        return "Initialisation of " + login;
    }

    public String getLogin(){ return login; }
}