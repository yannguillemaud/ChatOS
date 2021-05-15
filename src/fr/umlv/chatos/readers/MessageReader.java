package fr.umlv.chatos.readers;

import java.nio.ByteBuffer;

public class MessageReader implements Reader<Message> {

    enum State {DONE,WAITING,ERROR}

    private static final int BUFFER_SIZE = 1024;

    private final StringReader stringReader = new StringReader();

    private State state = State.WAITING;
    private Message message;
    private String login;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if(state == State.DONE || state == State.ERROR) throw new IllegalStateException();

        if(login == null) {
            var loginReaderStatus = stringReader.process(bb);
            if (loginReaderStatus != ProcessStatus.DONE) {
                return loginReaderStatus;
            }
            this.login = stringReader.get();
            stringReader.reset();
        }

        var messageReaderStatus = stringReader.process(bb);
        if(messageReaderStatus != ProcessStatus.DONE) {
            return messageReaderStatus;
        }
        String value = stringReader.get();
        stringReader.reset();

        message = new Message(login, value);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public Message get() {
        if(state != State.DONE) {
            throw new IllegalStateException();
        }
        return message;
    }

    @Override
    public void reset() {
        message = null;
        login = null;
        stringReader.reset();
        state = State.WAITING;
    }
}
