package fr.umlv.chatos.readers.privateconnection.acceptationrequest;

import fr.umlv.chatos.readers.Reader;
import fr.umlv.chatos.readers.StringReader;

import java.nio.ByteBuffer;

public class PrivateConnectionAcceptationRequestReader implements Reader<PrivateConnectionAcceptationRequest> {

    private enum State {DONE, WAITING, ERROR}

    private final StringReader stringReader = new StringReader();

    private State state = State.WAITING;
    private PrivateConnectionAcceptationRequest privateConnectionAcceptationRequest;

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

        privateConnectionAcceptationRequest = new PrivateConnectionAcceptationRequest(login);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public PrivateConnectionAcceptationRequest get() {
        if(state != State.DONE) {
            throw new IllegalStateException();
        }
        return privateConnectionAcceptationRequest;
    }

    @Override
    public void reset() {
        privateConnectionAcceptationRequest = null;
        stringReader.reset();
        state = State.WAITING;
    }
}
