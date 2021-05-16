package fr.umlv.chatos.readers.initialization;

import fr.umlv.chatos.readers.Reader;
import fr.umlv.chatos.readers.StringReader;

import java.nio.ByteBuffer;

public class InitializationMessageReader implements Reader<InitializationMessage> {

    private enum State {DONE,WAITING,ERROR}

    private static final int BUFFER_SIZE = 1024;

    private final StringReader stringReader = new StringReader();

    private State state = State.WAITING;
    private InitializationMessage initializationMessage;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if(state == State.DONE || state == State.ERROR) throw new IllegalStateException();

        var messageReaderStatus = stringReader.process(bb);
        if(messageReaderStatus != ProcessStatus.DONE) {
            return messageReaderStatus;
        }

        String login = stringReader.get();
        stringReader.reset();

        initializationMessage = new InitializationMessage(login);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public InitializationMessage get() {
        if(state != State.DONE) {
            throw new IllegalStateException();
        }
        return initializationMessage;
    }

    @Override
    public void reset() {
        initializationMessage = null;
        stringReader.reset();
        state = State.WAITING;
    }
}
