package fr.umlv.chatos.readers.global;

import fr.umlv.chatos.readers.Reader;
import fr.umlv.chatos.readers.StringReader;
import fr.umlv.chatos.readers.global.GlobalMessage;

import java.nio.ByteBuffer;

public class GlobalMessageReader implements Reader<GlobalMessage> {

    private enum State {DONE,WAITING,ERROR}

    private static final int BUFFER_SIZE = 1024;

    private final StringReader stringReader = new StringReader();

    private State state = State.WAITING;
    private GlobalMessage globalMessage;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if(state == State.DONE || state == State.ERROR) throw new IllegalStateException();


        var messageReaderStatus = stringReader.process(bb);
        if(messageReaderStatus != ProcessStatus.DONE) {
            return messageReaderStatus;
        }
        String value = stringReader.get();
        stringReader.reset();

        globalMessage = new GlobalMessage(value);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public GlobalMessage get() {
        if(state != State.DONE) {
            throw new IllegalStateException();
        }
        return globalMessage;
    }

    @Override
    public void reset() {
        globalMessage = null;
        stringReader.reset();
        state = State.WAITING;
    }
}
