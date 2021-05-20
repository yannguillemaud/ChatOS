package fr.umlv.chatos.readers.privateconnection.clientestablishment;

import fr.umlv.chatos.readers.Reader;
import fr.umlv.chatos.readers.StringReader;
import fr.umlv.chatos.readers.privateconnection.request.PrivateConnectionRequest;

import java.nio.ByteBuffer;

public class PrivateConnectionClientEstablishmentReader implements Reader<PrivateConnectionClientEstablishment> {

    private enum State {DONE, WAITING, ERROR}

    private final StringReader stringReader = new StringReader();

    private State state = State.WAITING;
    private PrivateConnectionClientEstablishment privateConnectionClientEstablishment;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if(state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        var messageReaderStatus = stringReader.process(bb);
        if(messageReaderStatus != ProcessStatus.DONE) {
            return messageReaderStatus;
        }

        String token = stringReader.get();
        stringReader.reset();

        privateConnectionClientEstablishment = new PrivateConnectionClientEstablishment(token);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public PrivateConnectionClientEstablishment get() {
        if(state != State.DONE) {
            throw new IllegalStateException();
        }
        return privateConnectionClientEstablishment;
    }

    @Override
    public void reset() {
        privateConnectionClientEstablishment = null;
        stringReader.reset();
        state = State.WAITING;
    }
}
