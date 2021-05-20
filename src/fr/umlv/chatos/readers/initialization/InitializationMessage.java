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

import static fr.umlv.chatos.readers.serverop.ServerMessageOpCode.SUCCESS;

public class InitializationMessage implements Sendable {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String login;

    public InitializationMessage(String login){
        this.login = login;
    }

    @Override
    public Optional<ByteBuffer> toByteBuffer(int maxBufferSize) {
        ByteBuffer encodedLogin = UTF8.encode(login);
        int loginSize = encodedLogin.remaining();
        int totalSize = Byte.BYTES + Integer.BYTES + loginSize;

        if(totalSize < maxBufferSize) {
            ByteBuffer initBuffer = ByteBuffer.allocate(maxBufferSize)
                    .put(ClientMessageOpCode.INITIALIZATION.value())
                    .putInt(loginSize).put(encodedLogin)
                    .flip();
            return Optional.of(initBuffer);
        } else return Optional.empty();
    }

    public static ByteBuffer acceptByteBuffer() {
        return ByteBuffer.allocate(Byte.BYTES)
                .put(SUCCESS.value())
                .flip();
    }

    public static ByteBuffer failureByteBuffer(ServerErrorCode errorCode){
        return ByteBuffer.allocate(Byte.BYTES * 2)
                .put(ServerMessageOpCode.FAIL.value())
                .put(errorCode.value())
                .flip();
    }

    @Override
    public String toString() {
        return "Initialisation of " + login;
    }

    public String getLogin(){ return login; }
}