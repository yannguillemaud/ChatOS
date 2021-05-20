package fr.umlv.chatos.readers.privateconnection.request;

import fr.umlv.chatos.readers.Reader;
import fr.umlv.chatos.readers.StringReader;

import java.nio.ByteBuffer;

public class PrivateConnectionRequestReader  implements Reader<PrivateConnectionRequest> {

    private enum State {DONE, WAITING, ERROR}

    private final StringReader stringReader = new StringReader();

    private State state = State.WAITING;
    private PrivateConnectionRequest privateConnectionRequest;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if(state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        var messageReaderStatus = stringReader.process(bb);
        if(messageReaderStatus != ProcessStatus.DONE) {
            return messageReaderStatus;
        }

        String login = stringReader.get();
        stringReader.reset();

        privateConnectionRequest = new PrivateConnectionRequest(login);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public PrivateConnectionRequest get() {
        if(state != State.DONE) {
            throw new IllegalStateException();
        }
        return privateConnectionRequest;
    }

    @Override
    public void reset() {
        privateConnectionRequest = null;
        stringReader.reset();
        state = State.WAITING;
    }
}
