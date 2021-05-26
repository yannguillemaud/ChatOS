package fr.umlv.chatos.server;

import fr.umlv.chatos.readers.clientglobal.ClientGlobalMessage;
import fr.umlv.chatos.readers.errorcode.ErrorCode;
import fr.umlv.chatos.readers.initialization.InitializationMessage;
import fr.umlv.chatos.readers.personal.PersonalMessage;
import fr.umlv.chatos.readers.privateconnection.acceptationrequest.PrivateConnectionAcceptationRequest;
import fr.umlv.chatos.readers.privateconnection.request.PrivateConnectionRequest;
import fr.umlv.chatos.readers.privateconnection.response.PrivateConnectionResponse;
import fr.umlv.chatos.readers.privateconnection.serverestablishment.PrivateConnectionServerEstablishment;
import fr.umlv.chatos.readers.serverglobal.ServerGlobalMessage;
import fr.umlv.chatos.readers.success.SuccessMessage;
import fr.umlv.chatos.readers.trame.Trame;
import fr.umlv.chatos.visitor.Visitor;

import java.util.logging.Logger;

import static fr.umlv.chatos.readers.errorcode.ErrorCode.*;
import static fr.umlv.chatos.server.ChatOSServer.BUFFER_SIZE;
import static fr.umlv.chatos.server.ChatOSServer.Context.StateContext.LOGGED;
import static fr.umlv.chatos.server.ChatOSServer.Context.StateContext.PENDING;

public class ServerFrameVisitor implements Visitor {
    static private final Logger logger = Logger.getLogger(ChatOSServer.class.getName());
    private final ChatOSServer server;
    private final ChatOSServer.Context context;

    public ServerFrameVisitor(ChatOSServer server, ChatOSServer.Context context){
        this.server = server;
        this.context = context;
    }

    @Override
    public void visit(SuccessMessage successMessage) {

    }

    @Override
    public void visit(ErrorCode errorCode) {

    }

    @Override
    public void visit(InitializationMessage initializationMessage) {
        if(context.isInitialized()){
            context.queueMessage(CONNECTION_ALREADY_INITIALIZED);
            return;
        }

        String login = initializationMessage.getLogin();
        if(server.tryRegister(login, context)){
            context.login = login;
            context.isInitialized = true;
            logger.info("Set " + login + " to " + context);
            context.queueMessage(new SuccessMessage());
        } else context.queueMessage(ALREADY_USED);
    }

    @Override
    public void visit(PersonalMessage personalMessage) {
        if(!context.isInitialized()){
            context.queueMessage(CONNECTION_NOT_INITIALIZED);
            return;
        } else if (personalMessage.getLogin().length() == 0){
            context.queueMessage(EMPTY_PSEUDO);
            return;
        } else if (!server.isRegistered(personalMessage.getLogin())){
            context.queueMessage(NOT_LINKED);
            return;
        }

        var personalMessageToSend = new PersonalMessage(context.login, personalMessage.getValue());
        var personalMessageToSendByteBuffer = personalMessageToSend.asByteBuffer(BUFFER_SIZE);

        if (personalMessageToSendByteBuffer.isEmpty()) {
            context.queueMessage(TOO_LONG_MESSAGE);
        } else {
            server.trySendTrameTo(personalMessage.getLogin(), personalMessageToSend); //returns true
            context.queueMessage(new SuccessMessage());
        }
    }

    @Override
    public void visit(ServerGlobalMessage serverGlobalMessage) {
    }

    @Override
    public void visit(ClientGlobalMessage clientGlobalMessage) {
        System.out.println("is: " + context.isInitialized());
        if (!context.isInitialized()) {
            context.queueMessage(CONNECTION_NOT_INITIALIZED);
            return;
        }

        var globalMessageToSend = new ServerGlobalMessage(context.login, clientGlobalMessage.getValue());
        var globalMessageToSendByteBuffer = globalMessageToSend.asByteBuffer(BUFFER_SIZE);

        Trame serverResponse;
        if (globalMessageToSendByteBuffer.isEmpty()) {
            serverResponse = TOO_LONG_MESSAGE;
        } else {
            server.broadcast(globalMessageToSend);
            serverResponse = new SuccessMessage();
        }

        context.queueMessage(serverResponse);
    }

    @Override
    public void visit(PrivateConnectionRequest connectionRequest) {
        if (!context.isInitialized()) {
            context.queueMessage(CONNECTION_NOT_INITIALIZED);
            return;
        } else if (connectionRequest.getLogin().length() == 0) {
            context.queueMessage(EMPTY_PSEUDO);
            return;
        } else if (!server.isRegistered(connectionRequest.getLogin())){
            context.queueMessage(NOT_LINKED);
            return;
        } else if (server.isPrivateConnectionDemandInitiated(context.login, connectionRequest.getLogin())) {
            context.queueMessage(PRIVATE_CONNECTION_ALREADY_INITIATED);
            return;
        } else if (server.isPrivateConnectionEstablished(context.login, connectionRequest.getLogin())) {
            context.queueMessage(PRIVATE_CONNECTION_ALREADY_ESTABLISHED);
            return;
        }


        var privateConnectionAcceptationRequest = new PrivateConnectionAcceptationRequest(connectionRequest.getLogin());
        var privateConnectionAcceptationRequestByteBuffer = privateConnectionAcceptationRequest.asByteBuffer(BUFFER_SIZE);

        if (privateConnectionAcceptationRequestByteBuffer.isEmpty()) {
            context.queueMessage(TOO_LONG_MESSAGE); // Pas sur de ce code d'erreur
        } else if (server.trySendTrameTo(connectionRequest.getLogin(), privateConnectionAcceptationRequest)) {
            server.privateConnectionDemandInit(context.login, connectionRequest.getLogin());
            context.queueMessage(new SuccessMessage());
        } else {
            context.queueMessage(NOT_LINKED);
        }
    }

    @Override
    public void visit(PrivateConnectionResponse response) {
        if (!context.isInitialized()) {
            context.queueMessage(CONNECTION_NOT_INITIALIZED);
            return;
        } else if (response.getLogin().length() == 0) {
            context.queueMessage(EMPTY_PSEUDO);
            return;
        } else if (!server.isPrivateConnectionDemandInitiated(response.getLogin(), context.login)) {
            context.queueMessage(PRIVATE_CONNECTION_DEMAND_NOT_INITIATED);
            return;
        } else if (server.isPrivateConnectionEstablished(response.getLogin(), context.login)) {
            context.queueMessage(PRIVATE_CONNECTION_ALREADY_ESTABLISHED);
            return;
        } else if (!response.getAcceptPrivateConnection()) {
            if (!server.trySendTrameTo(response.getLogin(), CONNEXION_DECLINED)) {
                context.queueMessage(NOT_LINKED);
            }
            server.abortPrivateConnectionDemand(response.getLogin(), context.login);
            return;
        }

        var token = server.generateNewToken();
        var privateConnectionEstablishmentA = new PrivateConnectionServerEstablishment(context.login, token);
        var privateConnectionEstablishmentB = new PrivateConnectionServerEstablishment(response.getLogin(), token);
        var privateConnectionEstablishmentAByteBuffer = privateConnectionEstablishmentA.asByteBuffer(BUFFER_SIZE);
        var privateConnectionEstablishmentBByteBuffer = privateConnectionEstablishmentB.asByteBuffer(BUFFER_SIZE);

        if (
                privateConnectionEstablishmentAByteBuffer.isEmpty() ||
                        privateConnectionEstablishmentBByteBuffer.isEmpty()
        ) {
            context.queueMessage(UNDEFINED); // Je sais pas trop quoi mettre là
        } else if (
                server.trySendTrameTo(context.login, privateConnectionEstablishmentB) &&
                        server.trySendTrameTo(response.getLogin(), privateConnectionEstablishmentA)
        ) {
            server.initPrivateConnection(response.getLogin(), context.login, token);
        } else {
            // Ca veut qu'un des deux gars existe pas ca n'as aucun sens
        }
    }
}
