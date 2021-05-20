package fr.umlv.chatos.client;

import fr.umlv.chatos.readers.opcode.OpCode;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static fr.umlv.chatos.readers.clientop.ClientMessageOpCode.GLOBAL_MESSAGE;
import static java.util.Objects.requireNonNull;

/**
 * Helps getting transformed string into protocol's trame
 * Due to our RFC, each client uses the same Charset (forced UTF8 Charset usage)
 * However to improves this protocols each client might use its own charset
 * Therefore each client whill have to create its own CommandTransformer according to it's defined charset
 */
public class CommandTransformer {
    private static final int BUFFER_SIZE = 1024;
    private static final Charset charset = StandardCharsets.UTF_8;
    private static final Logger logger = Logger.getLogger(CommandTransformer.class.getName());

    private String linkedLogin;

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
    public Optional<ByteBuffer> asByteBuffer(String commandString){
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
    private Optional<ByteBuffer> transformCommand(String[] tokens){
        String command = tokens[0];
        if(command.equals("INIT")) return initToBB(tokens);

        if(linkedLogin == null){
            System.out.println("Not initialized yet");
            initMessageUsage();
            return Optional.empty();
        }

        switch(command){
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
    private Optional<ByteBuffer> initToBB(String[] tokens){
        if(tokens.length != 2) {
            initMessageUsage();
            return Optional.empty();
        }

        byte opCode = OpCode.INITIALIZATION.value();
        String login = tokens[1];
        Optional<ByteBuffer> initBuffer = new InitializationMessage(login).toByteBuffer(BUFFER_SIZE);
        if(initBuffer.isPresent()){
            logger.info("Correct init packet from " + login);
            this.linkedLogin = login;
        }
        return initBuffer;
    }

    /**
     * Processes user command line into a global message bytebuffer
     * @param tokens user command line, containing the /command in tokens[0]
     * @return Optional of bytebuffer, containing ChatOs global message bytebuffer
     * returns an Optional.empty if the buffer size is > 1024
     */
    private Optional<ByteBuffer> globalToBB(String[] tokens){
        if(tokens.length < 2) {
            globalMessageUsage();
            return Optional.empty();
        }

        byte opCode = OpCode.GLOBAL_MESSAGE.value();
        String message = Arrays.stream(tokens, 1, tokens.length)
                .collect(Collectors.joining(" "));

        GlobalMessage globalMessage = new GlobalMessage(linkedLogin, message);
        Optional<ByteBuffer> optional = globalMessage.toByteBuffer(BUFFER_SIZE);
        if(optional.isEmpty()){
            System.out.println("Message too long");
        }
        return optional;
    }

    /**
     * Processes user command line into a private message bytebuffer
     * Command: PRIVATEMSG to message
     * @param tokens user command line
     * @return Optional of bytebuffer, containing ChatOs private message bytebuffer
     * returns an Optional.empty if the buffer size is > 1024
     */
    private Optional<ByteBuffer> privateMsgToBB(String[] tokens){
        if(tokens.length < 3) {
            globalMessageUsage();
            return Optional.empty();
        }

        byte opCode = OpCode.PERSONAL_MESSAGE.value();
        String adresseLogin = tokens[1];
        ByteBuffer encodedLogin = charset.encode(adresseLogin);
        int loginSize = encodedLogin.remaining();
        String message = Arrays.stream(tokens, 2, tokens.length)
                .collect(Collectors.joining(" "));
        Optional<ByteBuffer> optional = new PersonalMessage(linkedLogin, to, message).toByteBuffer(BUFFER_SIZE);
        if(optional.isEmpty()){
            System.out.println("Message too long");
        }
        return optional;
    }


    /**
     * Processes user command line into a connexion request bytebuffer
     * @param tokens user command line, containing the /command in tokens[0]
     * @return Optional of bytebuffer, containing ChatOs private connexion request bytebuffer
     * returns an Optional.empty if the buffer size is > 1024
     */
    private Optional<ByteBuffer> connexionRqtToBB(String[] tokens){
        if(tokens.length != 2){
            connexionRequestUsage();
            return Optional.empty();
        }

        byte opCode = OpCode.PRIVATE_CONNECTION_REQUEST.value();
        String adresseeLogin = tokens[1];
        ByteBuffer encodedLogin = charset.encode(adresseeLogin);
        int loginSize = encodedLogin.remaining();

        int packetSize = (Byte.BYTES + Integer.BYTES + loginSize);
        if(packetSize > BUFFER_SIZE){
            System.out.println("Login is too long");
            return Optional.empty();
        }

        logger.info("Correct connexion request packet to: " + adresseeLogin);
        ByteBuffer requestBuffer = ByteBuffer.allocate(BUFFER_SIZE)
                .put(opCode)
                .putInt(loginSize).put(encodedLogin)
                .flip();
        return Optional.of(requestBuffer);
    }
}