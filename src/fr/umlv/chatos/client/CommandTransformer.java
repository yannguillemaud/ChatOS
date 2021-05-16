package fr.umlv.chatos.client;

import fr.umlv.chatos.readers.clientop.ClientMessageOpCode;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Helps getting transformed string into protocol's trame
 * Due to our RFC, each client might not use the same Charset (as it could be if we forced UTF8 Charset usage)
 * Therefore each client should create its own CommandTransformer according to it's defined charset
 */
public class CommandTransformer {
    private static final int BUFFER_SIZE = 1024;
    private static final Charset charset = StandardCharsets.UTF_8;
    private static final Logger logger = Logger.getLogger(CommandTransformer.class.getName());

    private CommandTransformer(){}

    private static void initMessageUsage(){
        System.out.println("Server connexion request: INIT login");
    }

    private static void globalMessageUsage(){
        System.out.println("Global message: GLOBAL message");
    }

    private static void privateMessageUsage(){
        System.out.println("Private message: PRIVATEMSG to_pseudo message");
    }

    private static void connexionRequestUsage(){
        System.out.println("Private connexion request: PRIVATE to_pseudo");
    }

    private static void usages(){
        System.out.println("Commands list: ");
        initMessageUsage();
        globalMessageUsage();
        privateMessageUsage();
        connexionRequestUsage();
    }

    /**
     * Transform a command line sent to a bytebuffer
     * If it cannot be wrapped in a 1024 bytebuffer, returns an empty one
     * @param commandString
     * @return Optional of bytebuffer containing the message according to Chatos protocol's format,
     * empty if it's size > BUFFER_SIZE [ 1.0: 1024 ]
     */
    public static Optional<ByteBuffer> asByteBuffer(String commandString){
        requireNonNull(commandString);
        String[] tokens = commandString.split(" ");
        if(tokens.length < 2) return Optional.empty();
        return transformCommand(tokens);
    }

    /**
     *
     * @param tokens
     * @return
     */
    private static Optional<ByteBuffer> transformCommand(String[] tokens){
        String command = tokens[0];
        switch(command){
            case "INIT" : return initToBB(tokens);
            case "GLOBAL" : return globalToBB(tokens);
            case "PRIVATEMSG" : return privateMsgToBB(tokens);
            case "PRIVATE" : return connexionRqtToBB(tokens);
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
    private static Optional<ByteBuffer> initToBB(String[] tokens){
        if(tokens.length != 2) {
            initMessageUsage();
            return Optional.empty();
        }

        byte opCode = (byte) ClientMessageOpCode.INITIALIZATION.value();
        String login = tokens[1];
        ByteBuffer encodedLogin = charset.encode(login);
        int loginSize = encodedLogin.remaining();

        //Calcul de la place nécessaire
        int packetSize = Byte.BYTES + Integer.BYTES + loginSize;
        if(packetSize > BUFFER_SIZE){
            System.out.println("Login is too long");
            return Optional.empty();
        }
        ByteBuffer initBuffer = ByteBuffer.allocate(BUFFER_SIZE)
                .put(opCode)
                .putInt(loginSize).put(encodedLogin)
                .flip();

        return Optional.of(initBuffer);
    }

    /**
     * Processes user command line into a global message bytebuffer
     * @param tokens user command line, containing the /command in tokens[0]
     * @return Optional of bytebuffer, containing ChatOs global message bytebuffer
     * returns an Optional.empty if the buffer size is > 1024
     */
    private static Optional<ByteBuffer> globalToBB(String[] tokens){
        if(tokens.length < 2) {
            globalMessageUsage();
            return Optional.empty();
        }

        byte opCode = (byte) ClientMessageOpCode.GLOBAL_MESSAGE.value();
        String message = Arrays.stream(tokens, 1, tokens.length)
                .collect(Collectors.joining(" "));
        ByteBuffer encodedMessage = charset.encode(message);
        int messageSize = encodedMessage.remaining();

        int packetSize = Byte.BYTES + Integer.BYTES + messageSize;
        if(packetSize > BUFFER_SIZE){
            System.out.println("Message is too long");
            return Optional.empty();
        }

        var messageBuffer = ByteBuffer.allocate(BUFFER_SIZE)
                .put(opCode)
                .putInt(messageSize).put(encodedMessage)
                .flip();
        return Optional.of(messageBuffer);
    }

    /**
     * Processes user command line into a private message bytebuffer
     * @param tokens user command line
     * @return Optional of bytebuffer, containing ChatOs private message bytebuffer
     * returns an Optional.empty if the buffer size is > 1024
     */
    private static Optional<ByteBuffer> privateMsgToBB(String[] tokens){
        if(tokens.length < 3) {
            globalMessageUsage();
            return Optional.empty();
        }

        byte opCode = (byte) ClientMessageOpCode.PERSONAL_MESSAGE.value();
        String adresseLogin = tokens[1];
        ByteBuffer encodedLogin = charset.encode(adresseLogin);
        int loginSize = encodedLogin.remaining();

        String message = Arrays.stream(tokens, 2, tokens.length)
                .collect(Collectors.joining(" "));
        ByteBuffer encodedMessage = charset.encode(message);
        int messageSize = encodedMessage.remaining();

        int totalSize = (Byte.SIZE + Integer.BYTES * 2 + loginSize + messageSize);
        if(totalSize > BUFFER_SIZE){
            System.out.println("Message is too long");
            return Optional.empty();
        }

        ByteBuffer messageBuffer = ByteBuffer.allocate(BUFFER_SIZE)
                .put(opCode)
                .putInt(loginSize).put(encodedLogin)
                .putInt(messageSize).put(encodedMessage)
                .flip();
        return Optional.of(messageBuffer);
    }


    /**
     * Processes user command line into a connexion request bytebuffer
     * @param tokens user command line, containing the /command in tokens[0]
     * @return Optional of bytebuffer, containing ChatOs private connexion request bytebuffer
     * returns an Optional.empty if the buffer size is > 1024
     */
    private static Optional<ByteBuffer> connexionRqtToBB(String[] tokens){
        if(tokens.length != 2){
            connexionRequestUsage();
            return Optional.empty();
        }

        byte opCode = (byte) ClientMessageOpCode.PRIVATE_CONNECTION_REQUEST.value();
        String adresseeLogin = tokens[1];
        ByteBuffer encodedLogin = charset.encode(adresseeLogin);
        int loginSize = encodedLogin.remaining();

        int packetSize = (Byte.BYTES + Integer.BYTES + loginSize);
        if(packetSize > BUFFER_SIZE){
            System.out.println("Login is too long");
            return Optional.empty();
        }

        ByteBuffer requestBuffer = ByteBuffer.allocate(BUFFER_SIZE)
                .put(opCode)
                .putInt(loginSize).put(encodedLogin)
                .flip();
        return Optional.of(requestBuffer);
    }
}