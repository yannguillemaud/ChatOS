package fr.umlv.chatos.readers.errorcode;

import fr.umlv.chatos.readers.ByteReader;
import fr.umlv.chatos.readers.Reader;

import java.nio.ByteBuffer;

public class ErrorCodeReader implements Reader<ErrorCode> {
    private final ByteReader intReader = new ByteReader();
    private ProcessStatus state = ProcessStatus.REFILL;
    private ErrorCode opCode = null;


    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if(state == ProcessStatus.DONE || state == ProcessStatus.ERROR) {
            throw new IllegalStateException();
        }

        state = intReader.process(bb);
        if (state == ProcessStatus.DONE) {
            var optional = ErrorCode.serverErrorCode(intReader.get());
            if (optional.isEmpty()) { state = ProcessStatus.ERROR; }
            else opCode = optional.get();
        }

        return state;
    }

    @Override
    public ErrorCode get() {
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
