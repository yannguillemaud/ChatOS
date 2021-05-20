package fr.umlv.chatos.readers.personal;

import fr.umlv.chatos.readers.Sendable;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static fr.umlv.chatos.readers.opcode.OpCode.PERSONAL_MESSAGE;
import static java.util.Objects.requireNonNull;

public class PersonalMessage implements Sendable {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String login;
    private final String value;

    public PersonalMessage(String login, String value){
        this.login = login;
        this.value = value;
    }

    /**
     * Transforms a PersonalMessage instance into an Optional ByteBuffer if the given size is enough to store it
     * Otherwise returns an empty optional
     * @param maxBufferSize the maximum size of the message
     * @return An optional containing the message if it has enough space, otherwise an empty one
     */
    public Optional<ByteBuffer> toByteBuffer(int maxBufferSize) {
        ByteBuffer encodedLogin = UTF8.encode(login);
        ByteBuffer encodedValue = UTF8.encode(value);
        int loginSize = encodedLogin.remaining();
        int valueSize = encodedValue.remaining();
        int totalSize = Byte.BYTES + Integer.BYTES * 2 + loginSize + valueSize;
        if(totalSize <= maxBufferSize){
            return Optional.of(ByteBuffer.allocate(maxBufferSize)
                    .put(PERSONAL_MESSAGE.value())
                    .putInt(loginSize).put(encodedLogin)
                    .putInt(valueSize).put(encodedValue)
                    .flip()
            );
        } else return Optional.empty();
    };
    @Override
    public String toString() {
        return "Personal Message : " + value + " (" + login + ")";
    }

    public String getLogin(){ return login; }
    public String getValue(){ return value; }
}