package fr.umlv.chatos.visitor;

import fr.umlv.chatos.readers.clientglobal.ClientGlobalMessage;
import fr.umlv.chatos.readers.errorcode.ErrorCode;
import fr.umlv.chatos.readers.initialization.InitializationMessage;
import fr.umlv.chatos.readers.personal.PersonalMessage;
import fr.umlv.chatos.readers.privateconnection.acceptationrequest.PrivateConnectionAcceptationRequest;
import fr.umlv.chatos.readers.privateconnection.clientestablishment.PrivateConnectionClientEstablishment;
import fr.umlv.chatos.readers.privateconnection.request.PrivateConnectionRequest;
import fr.umlv.chatos.readers.privateconnection.response.PrivateConnectionResponse;
import fr.umlv.chatos.readers.serverglobal.ServerGlobalMessage;
import fr.umlv.chatos.readers.success.SuccessMessage;
import fr.umlv.chatos.server.ChatOSServer;

public interface Visitor {
    void visit(SuccessMessage successMessage);
    void visit(ErrorCode errorCode);
    void visit(InitializationMessage initializationMessage);
    void visit(PersonalMessage personalMessage);
    void visit(ServerGlobalMessage serverGlobalMessage);
    void visit(ClientGlobalMessage clientGlobalMessage);
    void visit(PrivateConnectionRequest connectionRequest);
    void visit(PrivateConnectionResponse response);
}
