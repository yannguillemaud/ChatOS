package fr.umlv.chatos.readers.clientop;

import fr.umlv.chatos.readers.ByteReader;
import fr.umlv.chatos.readers.IntReader;
import fr.umlv.chatos.readers.Reader;

import java.nio.ByteBuffer;

public class ClientOpReader implements Reader<ClientMessageOpCode> {

    private final ByteReader byteReader = new ByteReader();
    private ProcessStatus state = ProcessStatus.REFILL;
    private ClientMessageOpCode opCode = null;


    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if(state == ProcessStatus.DONE || state == ProcessStatus.ERROR) {
            throw new IllegalStateException();
        }

        state = byteReader.process(bb);
        if (state == ProcessStatus.DONE) {
            var optionalOpCode = ClientMessageOpCode.clientMessageOpCode(byteReader.get());
            if (optionalOpCode.isEmpty()) {
                state = ProcessStatus.ERROR;
            } else {
                opCode = optionalOpCode.orElseThrow();
            }
        }

        return state;
    }

    @Override
    public ClientMessageOpCode get() {
        if (state!= ProcessStatus.DONE) {
            throw new IllegalStateException();
        }
        return opCode;

    }

    @Override
    public void reset() {
        state = ProcessStatus.REFILL;
        byteReader.reset();
    }
}
