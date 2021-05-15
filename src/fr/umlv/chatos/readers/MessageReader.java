package fr.umlv.chatos.readers;

import static fr.umlv.chatos.readers.Reader.State.*;

import java.nio.ByteBuffer;

public class MessageReader implements Reader<Message> {

    private static final int BUFFER_SIZE = 1024;

    private final StringReader stringReader = new StringReader();

    private State state = State.WAITING;
    private Message message;
    private String login;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if(state == DONE || state == ERROR) throw new IllegalStateException();

        if(login == null) {
            var loginReaderStatus = stringReader.process(bb);
            if (loginReaderStatus != ProcessStatus.DONE) return loginReaderStatus;
            this.login = stringReader.get();
            stringReader.reset();
        }

        var messageReaderStatus = stringReader.process(bb);
        if(messageReaderStatus != ProcessStatus.DONE) return messageReaderStatus;
        String value = stringReader.get();
        stringReader.reset();

        message = new Message(login, value);
        state = DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public Message get() {
        if(state != DONE) throw new IllegalStateException();
        return message;
    }

    @Override
    public void reset() {
        message = null;
        login = null;
        stringReader.reset();
        state = WAITING;
    }
}
