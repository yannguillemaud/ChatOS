package fr.umlv.chatos.readers.clientglobal;

import fr.umlv.chatos.readers.Reader;
import fr.umlv.chatos.readers.StringReader;

import java.nio.ByteBuffer;

public class ClientGlobalMessageReader implements Reader<ClientGlobalMessage> {

    private enum State {DONE,WAITING,ERROR}

    private final StringReader stringReader = new StringReader();

    private State state = State.WAITING;
    private ClientGlobalMessage clientGlobalMessage;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if(state == State.DONE || state == State.ERROR) throw new IllegalStateException();

        var messageReaderStatus = stringReader.process(bb);
        if(messageReaderStatus != ProcessStatus.DONE) {
            return messageReaderStatus;
        }

        String value = stringReader.get();
        stringReader.reset();

        clientGlobalMessage = new ClientGlobalMessage(value);
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public ClientGlobalMessage get() {
        if(state != State.DONE) {
            throw new IllegalStateException();
        }
        return clientGlobalMessage;
    }

    @Override
    public void reset() {
        clientGlobalMessage = null;
        stringReader.reset();
        state = State.WAITING;
    }
}
