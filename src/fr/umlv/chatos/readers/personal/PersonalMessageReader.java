package fr.umlv.chatos.readers.personal;

import fr.umlv.chatos.readers.Reader;
import fr.umlv.chatos.readers.StringReader;

import java.nio.ByteBuffer;

public class PersonalMessageReader implements Reader<PersonalMessage> {

    private enum State {DONE,WAITING,ERROR}

    private static final int BUFFER_SIZE = 1024;

    private final StringReader stringReader = new StringReader();

    private State state = State.WAITING;
    private PersonalMessage personalMessage;
    private String from;
    private String to;

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

        if(to == null) {
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

        personalMessage = new PersonalMessage(from, to, value);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public PersonalMessage get() {
        if(state != State.DONE) {
            throw new IllegalStateException();
        }
        return personalMessage;
    }

    @Override
    public void reset() {
        personalMessage = null;
        from = null;
        to = null;
        stringReader.reset();
        state = State.WAITING;
    }
}
