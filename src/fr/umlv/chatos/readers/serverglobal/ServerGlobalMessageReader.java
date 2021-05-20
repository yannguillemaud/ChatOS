package fr.umlv.chatos.readers.serverglobal;

import fr.umlv.chatos.readers.Reader;
import fr.umlv.chatos.readers.StringReader;

import java.nio.ByteBuffer;

public class ServerGlobalMessageReader implements Reader<ServerGlobalMessage> {

    private enum State {DONE,WAITING,ERROR}

    private final StringReader stringReader = new StringReader();

    private State state = State.WAITING;
    private String login;
    private ServerGlobalMessage serverGlobalMessage;

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

        serverGlobalMessage = new ServerGlobalMessage(login, value);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public ServerGlobalMessage get() {
        if(state != State.DONE) {
            throw new IllegalStateException();
        }
        return serverGlobalMessage;
    }

    @Override
    public void reset() {
        login = null;
        serverGlobalMessage = null;
        stringReader.reset();
        state = State.WAITING;
    }
}
