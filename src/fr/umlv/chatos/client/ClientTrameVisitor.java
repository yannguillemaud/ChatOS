package fr.umlv.chatos.client;

import fr.umlv.chatos.readers.clientglobal.ClientGlobalMessage;
import fr.umlv.chatos.readers.errorcode.ErrorCode;
import fr.umlv.chatos.readers.initialization.InitializationMessage;
import fr.umlv.chatos.readers.personal.PersonalMessage;
import fr.umlv.chatos.readers.privateconnection.request.PrivateConnectionRequest;
import fr.umlv.chatos.readers.privateconnection.response.PrivateConnectionResponse;
import fr.umlv.chatos.readers.serverglobal.ServerGlobalMessage;
import fr.umlv.chatos.readers.success.SuccessMessage;
import fr.umlv.chatos.readers.trame.Trame;
import fr.umlv.chatos.server.ChatOSServer;
import fr.umlv.chatos.visitor.Visitor;

public class ClientTrameVisitor implements Visitor {
    @Override
    public void visit(SuccessMessage successMessage) {
        System.out.println("Connexion successful");
    }

    @Override
    public void visit(ErrorCode errorCode) {
        System.out.println(errorCode);
    }

    @Override
    public void visit(InitializationMessage initializationMessage) {

    }

    @Override
    public void visit(PersonalMessage personalMessage) {
        System.out.println(personalMessage);
    }

    @Override
    public void visit(ServerGlobalMessage serverGlobalMessage) {
        System.out.println(serverGlobalMessage);
    }

    @Override
    public void visit(ClientGlobalMessage clientGlobalMessage) {

    }

    @Override
    public void visit(PrivateConnectionRequest connectionRequest) {
        System.out.println(connectionRequest);
    }

    @Override
    public void visit(PrivateConnectionResponse response) {
        System.out.println(response);
    }
}
