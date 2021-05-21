package fr.umlv.chatos.readers.trame;

import fr.umlv.chatos.readers.Reader;
import fr.umlv.chatos.readers.clientglobal.ClientGlobalMessage;
import fr.umlv.chatos.readers.clientglobal.ClientGlobalMessageReader;
import fr.umlv.chatos.readers.errorcode.ErrorCode;
import fr.umlv.chatos.readers.errorcode.ErrorCodeReader;
import fr.umlv.chatos.readers.initialization.InitializationMessage;
import fr.umlv.chatos.readers.initialization.InitializationMessageReader;
import fr.umlv.chatos.readers.opcode.OpCode;
import fr.umlv.chatos.readers.opcode.OpCodeReader;
import fr.umlv.chatos.readers.personal.PersonalMessage;
import fr.umlv.chatos.readers.personal.PersonalMessageReader;
import fr.umlv.chatos.readers.privateconnection.acceptationrequest.PrivateConnectionAcceptationRequest;
import fr.umlv.chatos.readers.privateconnection.acceptationrequest.PrivateConnectionAcceptationRequestReader;
import fr.umlv.chatos.readers.privateconnection.clientestablishment.PrivateConnectionClientEstablishment;
import fr.umlv.chatos.readers.privateconnection.clientestablishment.PrivateConnectionClientEstablishmentReader;
import fr.umlv.chatos.readers.privateconnection.request.PrivateConnectionRequest;
import fr.umlv.chatos.readers.privateconnection.request.PrivateConnectionRequestReader;
import fr.umlv.chatos.readers.privateconnection.response.PrivateConnectionResponse;
import fr.umlv.chatos.readers.privateconnection.response.PrivateConnectionResponseReader;
import fr.umlv.chatos.readers.privateconnection.serverestablishment.PrivateConnectionServerEstablishment;
import fr.umlv.chatos.readers.privateconnection.serverestablishment.PrivateConnectionServerEstablishmentReader;
import fr.umlv.chatos.readers.serverglobal.ServerGlobalMessage;
import fr.umlv.chatos.readers.serverglobal.ServerGlobalMessageReader;
import fr.umlv.chatos.readers.success.SuccessMessage;

import java.nio.ByteBuffer;

import static fr.umlv.chatos.readers.errorcode.ErrorCode.ALREADY_USED;

public class TrameReader implements Reader<Trame> {

    private enum State {DONE,WAITING,ERROR}


    private final Reader<OpCode> opReader = new OpCodeReader();
    private final Reader<InitializationMessage> initializationMessageReader = new InitializationMessageReader();
    private final Reader<ErrorCode> errorCodeReader = new ErrorCodeReader();
    private final Reader<PersonalMessage> personalMessageReader = new PersonalMessageReader();
    private final Reader<ClientGlobalMessage> clientGlobalMessageReader = new ClientGlobalMessageReader();
    private final Reader<ServerGlobalMessage> serverGlobalMessageReader = new ServerGlobalMessageReader();
    private final Reader<PrivateConnectionRequest> privateConnectionRequestReader = new PrivateConnectionRequestReader();
    private final Reader<PrivateConnectionAcceptationRequest> privateConnectionAcceptationRequestReader = new PrivateConnectionAcceptationRequestReader();
    private final Reader<PrivateConnectionResponse> privateConnectionResponseReader = new PrivateConnectionResponseReader();
    private final Reader<PrivateConnectionServerEstablishment> privateConnectionServerEstablishmentReader = new PrivateConnectionServerEstablishmentReader();
    private final Reader<PrivateConnectionClientEstablishment> privateConnectionClientEstablishmentReader = new PrivateConnectionClientEstablishmentReader();

    private State state = State.WAITING;
    private OpCode opCode = null;
    private Trame trame = null;

    private ProcessStatus processTrameReader(Reader<? extends Trame> reader, ByteBuffer bb) {
        Reader.ProcessStatus status = reader.process(bb);
        if(status != ProcessStatus.DONE) {
            return status;
        }

        trame = reader.get();
        reader.reset();

        state = State.DONE;
        return ProcessStatus.DONE;
    }

    private ProcessStatus processSuccess() {
        trame = new SuccessMessage();

        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if(state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }

        if(opCode == null) {
            var opReaderStatus = opReader.process(bb);
            if (opReaderStatus != ProcessStatus.DONE) {
                return opReaderStatus;
            }
            opCode = opReader.get();
            System.out.println("Received: " + opCode);
            opReader.reset();
        }

        return switch (opCode) {
            case INITIALIZATION -> processTrameReader(initializationMessageReader, bb);
            case SUCCESS -> processSuccess();
            case FAIL -> processTrameReader(errorCodeReader, bb);
            case PERSONAL_MESSAGE -> processTrameReader(personalMessageReader, bb);
            case CLIENT_GLOBAL_MESSAGE -> processTrameReader(clientGlobalMessageReader, bb);
            case SERVER_GLOBAL_MESSAGE -> processTrameReader(serverGlobalMessageReader, bb);
            case PRIVATE_CONNECTION_REQUEST -> processTrameReader(privateConnectionRequestReader, bb);
            case PRIVATE_CONNECTION_ACCEPTATION_REQUEST -> processTrameReader(privateConnectionAcceptationRequestReader, bb);
            case PRIVATE_CONNECTION_RESPONSE -> processTrameReader(privateConnectionResponseReader, bb);
            case PRIVATE_CONNECTION_SERVER_ESTABLISHMENT -> processTrameReader(privateConnectionServerEstablishmentReader, bb);
            case PRIVATE_CONNECTION_CLIENT_ESTABLISHMENT -> processTrameReader(privateConnectionClientEstablishmentReader, bb);
        };
    }

    @Override
    public Trame get() {
        if(state != State.DONE) {
            throw new IllegalStateException();
        }
        return trame;
    }

    @Override
    public void reset() {
        switch (opCode) {
            case INITIALIZATION -> initializationMessageReader.reset();
            case FAIL -> errorCodeReader.reset();
            case PERSONAL_MESSAGE -> personalMessageReader.reset();
            case CLIENT_GLOBAL_MESSAGE -> clientGlobalMessageReader.reset();
            case SERVER_GLOBAL_MESSAGE -> serverGlobalMessageReader.reset();
            case PRIVATE_CONNECTION_REQUEST -> privateConnectionRequestReader.reset();
            case PRIVATE_CONNECTION_ACCEPTATION_REQUEST -> privateConnectionAcceptationRequestReader.reset();
            case PRIVATE_CONNECTION_RESPONSE -> privateConnectionResponseReader.reset();
            case PRIVATE_CONNECTION_SERVER_ESTABLISHMENT -> privateConnectionServerEstablishmentReader.reset();
            case PRIVATE_CONNECTION_CLIENT_ESTABLISHMENT -> privateConnectionClientEstablishmentReader.reset();
        }
        opCode = null;
        state = State.WAITING;
    }
}
