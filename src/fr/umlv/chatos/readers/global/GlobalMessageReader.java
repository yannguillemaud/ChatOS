package fr.umlv.chatos.readers.global;

import fr.umlv.chatos.readers.Reader;
import fr.umlv.chatos.readers.StringReader;
import fr.umlv.chatos.readers.global.GlobalMessage;
import fr.umlv.chatos.readers.personal.PersonalMessage;
import fr.umlv.chatos.readers.personal.PersonalMessageReader;

import java.nio.ByteBuffer;

public class GlobalMessageReader implements Reader<GlobalMessage> {

    private enum State {DONE,WAITING,ERROR}

    private static final int BUFFER_SIZE = 1024;

    private final StringReader stringReader = new StringReader();

    private State state = State.WAITING;
    private String from;
    private GlobalMessage globalMessage;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if(state == State.DONE || state == State.ERROR) throw new IllegalStateException();

        if(from == null) {
            var loginReaderStatus = stringReader.process(bb);
            if (loginReaderStatus != ProcessStatus.DONE) {
                return loginReaderStatus;
            }
            this.from = stringReader.get();
            stringReader.reset();
        }

        var messageReaderStatus = stringReader.process(bb);
        if(messageReaderStatus != ProcessStatus.DONE) {
            return messageReaderStatus;
        }

        String value = stringReader.get();
        stringReader.reset();

        globalMessage = new GlobalMessage(from, value);
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
        from = null;
        globalMessage = null;
        stringReader.reset();
        state = State.WAITING;
    }
}
