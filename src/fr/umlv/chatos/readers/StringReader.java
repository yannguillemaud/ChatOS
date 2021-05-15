package fr.umlv.chatos.readers;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static fr.umlv.chatos.readers.Reader.State.*;

public class StringReader implements Reader<String> {
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    private final int BUFFER_SIZE = 1024;
    private final ByteBuffer stringBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private final IntReader intReader = new IntReader();

    private String value;
    private State state = WAITING;
    private boolean hasSize;

    public ProcessStatus process(ByteBuffer bb) {
        if(state == DONE || state == ERROR) {
            throw new IllegalStateException();
        }
        if(stringBuffer.position() == 0 && !hasSize){
            ProcessStatus status = intReader.process(bb);
            if (status == ProcessStatus.REFILL) {
                return status;
            }

            int size = intReader.get();
            intReader.reset();

            if(size < 0 || size > BUFFER_SIZE) {
                return ProcessStatus.ERROR;
            }
            stringBuffer.limit(size);
            hasSize = true;
        }

        try {
            bb.flip();
            int remains = stringBuffer.remaining();
            if(bb.remaining() <= remains){
                stringBuffer.put(bb);
            } else {
                int oldLimit = bb.limit();
                bb.limit(remains);
                stringBuffer.put(bb);
                bb.limit(oldLimit);
            }
        } finally {
            bb.compact();
        }

        if(stringBuffer.hasRemaining()){
            return ProcessStatus.REFILL;
        }

        state = DONE;
        stringBuffer.flip();
        value = UTF8.decode(stringBuffer).toString();
        return ProcessStatus.DONE;

    }

    @Override
    public String get() {
        if(state != DONE) throw new IllegalStateException();
        return value;
    }

    @Override
    public void reset() {
        stringBuffer.clear();
        value = null;
        hasSize = false;
        intReader.reset();
        state = WAITING;
    }
}
