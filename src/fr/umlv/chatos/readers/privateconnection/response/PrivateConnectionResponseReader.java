package fr.umlv.chatos.readers.privateconnection.response;

import fr.umlv.chatos.readers.ByteReader;
import fr.umlv.chatos.readers.Reader;
import fr.umlv.chatos.readers.StringReader;
import fr.umlv.chatos.readers.personal.PersonalMessage;
import fr.umlv.chatos.readers.personal.PersonalMessageReader;
import fr.umlv.chatos.readers.privateconnection.request.PrivateConnectionRequest;

import java.nio.ByteBuffer;

public class PrivateConnectionResponseReader implements Reader<PrivateConnectionResponse> {

    private enum State {DONE, WAITING, ERROR}

    private final StringReader stringReader = new StringReader();
    private final ByteReader byteReader = new ByteReader();

    private State state = State.WAITING;
    private PrivateConnectionResponse privateConnectionResponse;
    private String login = null;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if(state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if(login == null) {
            var loginReaderStatus = stringReader.process(bb);
            if (loginReaderStatus != ProcessStatus.DONE) {
                return loginReaderStatus;
            }
            this.login = stringReader.get();
            stringReader.reset();
        }

        var byteReaderStatus = byteReader.process(bb);
        if(byteReaderStatus != ProcessStatus.DONE) {
            return byteReaderStatus;
        }
        boolean acceptPrivateConnection = byteReader.get() == 1;
        byteReader.reset();

        privateConnectionResponse = new PrivateConnectionResponse(login, acceptPrivateConnection);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public PrivateConnectionResponse get() {
        if(state != State.DONE) {
            throw new IllegalStateException();
        }
        return privateConnectionResponse;
    }

    @Override
    public void reset() {
        privateConnectionResponse = null;
        stringReader.reset();
        state = State.WAITING;
    }
}
