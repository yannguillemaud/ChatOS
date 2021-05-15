package fr.umlv.chatos.readers;

import fr.umlv.chatos.opcode.ClientMessageOpCode;
import fr.umlv.chatos.opcode.ServerMessageOpCode;

import java.nio.ByteBuffer;

public class ClientOpReader implements Reader<ClientMessageOpCode> {

    private final IntReader intReader = new IntReader();
    private ProcessStatus state = ProcessStatus.REFILL;
    private ClientMessageOpCode opCode = null;


    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if(state == ProcessStatus.DONE || state == ProcessStatus.ERROR) {
            throw new IllegalStateException();
        }

        state = intReader.process(bb);
        if (state == ProcessStatus.DONE) {
            var optionalOpCode = ClientMessageOpCode.clientMessageOpCode(intReader.get());
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
        intReader.reset();
    }
}
