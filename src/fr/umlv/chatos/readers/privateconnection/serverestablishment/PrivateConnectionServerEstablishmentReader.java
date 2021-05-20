package fr.umlv.chatos.readers.privateconnection.serverestablishment;

import fr.umlv.chatos.readers.Reader;
import fr.umlv.chatos.readers.StringReader;

import java.nio.ByteBuffer;

public class PrivateConnectionServerEstablishmentReader implements Reader<PrivateConnectionServerEstablishment> {

    private enum State {DONE, WAITING, ERROR}

    private final StringReader stringReader = new StringReader();

    private State state = State.WAITING;
    private PrivateConnectionServerEstablishment privateConnectionServerEstablishment;
    private String login;

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

        var messageReaderStatus = stringReader.process(bb);
        if(messageReaderStatus != ProcessStatus.DONE) {
            return messageReaderStatus;
        }
        String token = stringReader.get();
        stringReader.reset();

        privateConnectionServerEstablishment = new PrivateConnectionServerEstablishment(login, token);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public PrivateConnectionServerEstablishment get() {
        if(state != State.DONE) {
            throw new IllegalStateException();
        }
        return privateConnectionServerEstablishment;
    }

    @Override
    public void reset() {
        privateConnectionServerEstablishment = null;
        login = null;
        stringReader.reset();
        state = State.WAITING;
    }
}
