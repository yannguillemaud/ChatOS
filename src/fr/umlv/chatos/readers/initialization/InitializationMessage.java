package fr.umlv.chatos.readers.initialization;

import fr.umlv.chatos.readers.servererrorcode.ServerErrorCode;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static fr.umlv.chatos.readers.servererrorcode.ServerMessageOpCode.SUCCESS;

public class InitializationMessage {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String login;

    public InitializationMessage(String login){
        this.login = login;
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