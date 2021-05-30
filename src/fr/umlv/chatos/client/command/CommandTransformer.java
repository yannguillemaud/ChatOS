package fr.umlv.chatos.client.command;

import fr.umlv.chatos.readers.clientglobal.ClientGlobalMessage;
import fr.umlv.chatos.readers.initialization.InitializationMessage;
import fr.umlv.chatos.readers.personal.PersonalMessage;
import fr.umlv.chatos.readers.privateconnection.acceptationrequest.PrivateConnectionAcceptationRequest;
import fr.umlv.chatos.readers.privateconnection.response.PrivateConnectionResponse;
import fr.umlv.chatos.readers.trame.Trame;

import fr.umlv.chatos.readers.privateconnection.request.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Helps getting transformed string into protocol's trame
 * Due to our RFC, each client uses the same Charset (forced UTF8 Charset usage)
 * However to improves this protocols each client might use its own charset
 * Therefore each client whill have to create its own CommandTransformer according to it's defined charset
 */
public class CommandTransformer {
    private static void initMessageUsage(){
        System.out.println("Server connexion request: /login login");
    }

    private static void globalMessageUsage(){
        System.out.println("Global message: /global message");
    }

    private static void privateMessageUsage(){
        System.out.println("Private message: /private to_pseudo message");
    }

    private static void connexionRequestUsage(){ System.out.println("Private connexion request: /connexion to_pseudo"); }

    private static void acceptUsage() { System.out.println("Accept private connexion: /accept to_pseudo"); }

    private static void declineUsage() { System.out.println("Decline private connexion: /decline to_pseudo"); }

    private static void usages(){
        System.out.println("Commands list: ");
        initMessageUsage();
        globalMessageUsage();
        privateMessageUsage();
        connexionRequestUsage();
        acceptUsage();
        declineUsage();
    }

    /**
     * Transform a command line sent to a bytebuffer
     * If it cannot be wrapped in a 1024 bytebuffer, returns an empty one
     * @param commandString
     * @return Optional of bytebuffer containing the message according to Chatos protocol's format,
     * empty if it's size > BUFFER_SIZE [ 1.0: 1024 ]
     */
    public Optional<Trame> asByteBuffer(String commandString){
        requireNonNull(commandString);
        String[] tokens = commandString.split(" ");
        return transformCommand(tokens);
    }

    /**
     *
     * @param tokens
     * @return
     */
    private Optional<Trame> transformCommand(String[] tokens){
        String command = tokens[0];
        switch(command){
            case "/login" : return initTrame(tokens);
            case "/global" : return globalTrame(tokens);
            case "/private" : return privateMessageTrame(tokens);
            case "/connexion" : return connexionRqtTrame(tokens);
            case "/accept": return connexionAcceptTrame(tokens);
            case "/decline": return connexionDeclineTrame(tokens);
            case "/help": {
                usages();
                return Optional.empty();
            }
            default : {
                System.out.println("Unknown command");
                return Optional.empty();
            }
        }
    }

    /**
     * Processes user command line into an initialization bytebuffer
     * @param tokens user command line, containing the /command in tokens[0]
     * @return Optional of bytebuffer, containing ChatOs initialization bytebuffer
     * returns an Optional.empty if the buffer size is > 1024
     */
    private Optional<Trame> initTrame(String[] tokens){
        if(tokens.length != 2) {
            initMessageUsage();
            return Optional.empty();
        }

        String login = tokens[1];
        return Optional.of(new InitializationMessage(login));
    }

    /**
     * Processes user command line into a global message bytebuffer
     * @param tokens user command line, containing the /command in tokens[0]
     * @return Optional of bytebuffer, containing ChatOs global message bytebuffer
     * returns an Optional.empty if the buffer size is > 1024
     */
    private Optional<Trame> globalTrame(String[] tokens){
        if(tokens.length < 2) {
            globalMessageUsage();
            return Optional.empty();
        }

        String message = Arrays.stream(tokens, 1, tokens.length)
                .collect(Collectors.joining(" "));

        return Optional.of(new ClientGlobalMessage(message));
    }

    /**
     * Processes user command line into a private message bytebuffer
     * Command: PRIVATEMSG to message
     * @param tokens user command line
     * @return Optional of bytebuffer, containing ChatOs private message bytebuffer
     * returns an Optional.empty if the buffer size is > 1024
     */
    private Optional<Trame> privateMessageTrame(String[] tokens){
        if(tokens.length < 3) {
            privateMessageUsage();
            return Optional.empty();
        }

        String login = tokens[1];
        String message = Arrays.stream(tokens, 2, tokens.length)
                .collect(Collectors.joining(" "));
        return Optional.of(new PersonalMessage(login, message));
    }


    /**
     * Processes user command line into a connexion request bytebuffer
     * @param tokens user command line, containing the /command in tokens[0]
     * @return Optional of bytebuffer, containing ChatOs private connexion request bytebuffer
     * returns an Optional.empty if the buffer size is > 1024
     */
    private Optional<Trame> connexionRqtTrame(String[] tokens){
        if(tokens.length != 2){
            connexionRequestUsage();
            return Optional.empty();
        }

        String adresseeLogin = tokens[1];
        return Optional.of(new PrivateConnectionRequest(adresseeLogin));
    }

    private Optional<Trame> connexionAcceptTrame(String[] tokens){
        if(tokens.length != 2){
            acceptUsage();
            return Optional.empty();
        }

        String adresseeLogin = tokens[1];
        return Optional.of(new PrivateConnectionResponse(adresseeLogin, true));
    }

    private Optional<Trame> connexionDeclineTrame(String[] tokens){
        if(tokens.length != 2){
            acceptUsage();
            return Optional.empty();
        }

        String adresseeLogin = tokens[1];
        return Optional.of(new PrivateConnectionResponse(adresseeLogin, false));
    }
}