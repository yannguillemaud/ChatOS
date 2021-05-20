package fr.umlv.chatos.server;

import fr.umlv.chatos.readers.Reader;
import fr.umlv.chatos.readers.opcode.OpCode;
import fr.umlv.chatos.readers.opcode.OpCodeReader;
import fr.umlv.chatos.readers.global.GlobalMessage;
import fr.umlv.chatos.readers.global.GlobalMessageReader;
import fr.umlv.chatos.readers.initialization.InitializationMessage;
import fr.umlv.chatos.readers.initialization.InitializationMessageReader;
import fr.umlv.chatos.readers.personal.PersonalMessage;
import fr.umlv.chatos.readers.personal.PersonalMessageReader;
import fr.umlv.chatos.readers.privateconnection.clientestablishment.PrivateConnectionClientEstablishment;
import fr.umlv.chatos.readers.privateconnection.clientestablishment.PrivateConnectionClientEstablishmentReader;
import fr.umlv.chatos.readers.privateconnection.request.PrivateConnectionRequest;
import fr.umlv.chatos.readers.privateconnection.request.PrivateConnectionRequestReader;
import fr.umlv.chatos.readers.privateconnection.response.PrivateConnectionResponse;
import fr.umlv.chatos.readers.privateconnection.response.PrivateConnectionResponseReader;
import fr.umlv.chatos.readers.servererrorcode.ServerErrorCode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static fr.umlv.chatos.readers.opcode.OpCode.FAIL;
import static fr.umlv.chatos.readers.opcode.OpCode.SUCCESS;
import static fr.umlv.chatos.readers.servererrorcode.ServerErrorCode.ALREADY_USED;

public class ChatOSServer {

    static private class Context {

        final private SelectionKey key;
        final private SocketChannel sc;
        private final int BUFFER_SIZE = 1024;
        final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
        final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
        final private Queue<ByteBuffer> queue = new LinkedList<>();
        final private ChatOSServer server;
        private boolean closed = false;

        private final Reader<OpCode> opReader = new OpCodeReader();
        private final Reader<InitializationMessage> initializationMessageReader = new InitializationMessageReader();
        private final Reader<PersonalMessage> personalMessageReader = new PersonalMessageReader();
        private final Reader<GlobalMessage> globalMessageReader = new GlobalMessageReader();
        private final Reader<PrivateConnectionRequest> privateConnectionRequestReader = new PrivateConnectionRequestReader();
        private final Reader<PrivateConnectionResponse> privateConnectionResponseReader = new PrivateConnectionResponseReader();
        private final Reader<PrivateConnectionClientEstablishment> privateConnectionClientEstablishmentReader = new PrivateConnectionClientEstablishmentReader();
        private OpCode opCode = null;
        private String login = null;


        private Context(ChatOSServer server, SelectionKey key){
            this.key = key;
            this.sc = (SocketChannel) key.channel();
            this.server = server;
        }


        public static ByteBuffer acceptByteBuffer() {
            return ByteBuffer.allocate(Byte.BYTES)
                    .put(SUCCESS.value())
                    .flip();
        }

        public static ByteBuffer failureByteBuffer(ServerErrorCode errorCode){
            return ByteBuffer.allocate(Byte.BYTES * 2)
                    .put(FAIL.value())
                    .put(errorCode.value())
                    .flip();
        }

        private void processIn() {
            switch (opCode) {
                case INITIALIZATION -> {
                    for(;;){
                        Reader.ProcessStatus status = initializationMessageReader.process(bbin);
                        switch (status) {
                            case DONE -> {
                                System.out.println("DONE");
                                opCode = null;
                                InitializationMessage message = initializationMessageReader.get();
                                String login = message.getLogin();
                                ByteBuffer serverResponse;
                                if (server.isLoginAvailable(login)) {
                                    serverResponse = acceptByteBuffer();
                                    logger.info("Set " + login + " to " + sc);
                                    this.login = login;
                                } else {
                                    serverResponse = failureByteBuffer(ALREADY_USED);
                                }
                                queueMessage(serverResponse);
                                initializationMessageReader.reset();
                            }
                            case REFILL -> {
                                System.out.println("REFILL");
                                return;
                            }
                            case ERROR -> {
                                System.out.println("CLOSE");
                                silentlyClose();
                                return;
                            }
                        }
                    }
                }
                case GLOBAL_MESSAGE -> {
                    for(;;){
                        Reader.ProcessStatus status = globalMessageReader.process(bbin);
                        switch (status) {
                            case DONE -> {
                                System.out.println("DONE global");
                                opCode = null;
                                var globalMessage = globalMessageReader.get();
                                var globalMessageToSend = new GlobalMessage(globalMessage.getValue(), login);
                                var globalMessageToSendByteBuffer = globalMessageToSend.toByteBuffer(BUFFER_SIZE);
                                if (globalMessageToSendByteBuffer.isEmpty()) {
                                    globalMessageReader.reset();
                                    break;
                                }
                                server.broadcast(globalMessageToSendByteBuffer.orElseThrow());
                                globalMessageReader.reset();
                            }
                            case REFILL -> {
                                System.out.println("REFILL global");
                                return;
                            }
                            case ERROR -> {
                                System.out.println("CLOSE global");
                                silentlyClose();
                                return;
                            }
                        }
                    }
                }
                case PERSONAL_MESSAGE -> {
                    for(;;){
                        Reader.ProcessStatus status = personalMessageReader.process(bbin);
                        switch (status) {
                            case DONE -> {
                                opCode = null;
                                var personalMessage = personalMessageReader.get();
                                var personalMessageToSend = new PersonalMessage(login, personalMessage.getValue());
                                var personalMessageToSendByteBuffer = personalMessageToSend.toByteBuffer(BUFFER_SIZE);
                                if (personalMessageToSendByteBuffer.isEmpty()) {
                                    personalMessageReader.reset();
                                    break;
                                }
                                server.sendPersonalMessage(personalMessage.getLogin(), personalMessageToSendByteBuffer.orElseThrow());
                                personalMessageReader.reset();
                            }
                            case REFILL -> {
                                System.out.println("REFILL perso");
                                return;
                            }
                            case ERROR -> {
                                System.out.println("CLOSE");
                                silentlyClose();
                                return;
                            }
                        }
                    }
                }
                case PRIVATE_CONNECTION_REQUEST -> {
                    for(;;){
                        Reader.ProcessStatus status = privateConnectionRequestReader.process(bbin);
                        switch (status) {
                            case DONE -> {
                                opCode = null;
                                var value = privateConnectionRequestReader.get();
                                var valueByteBuffer = value.toByteBuffer(BUFFER_SIZE);
                                if (valueByteBuffer.isEmpty()) {
                                    privateConnectionRequestReader.reset();
                                    break;
                                }
                                server.sendPersonalMessage(value.getLogin(), valueByteBuffer.orElseThrow());
                                privateConnectionRequestReader.reset();
                            }
                            case REFILL -> {
                                System.out.println("REFILL perso");
                                return;
                            }
                            case ERROR -> {
                                System.out.println("CLOSE");
                                silentlyClose();
                                return;
                            }
                        }
                    }
                }
                case PRIVATE_CONNECTION_RESPONSE -> {

                }
                case PRIVATE_CONNECTION_CLIENT_ESTABLISHMENT -> {

                }

            }
        }

        /**
         * Process the content of bbin
         *
         * The convention is that bbin is in write-mode before the call
         * to process and after the call
         *
         */
        private void processInOpCode() {
            if (opCode != null) {
                processIn();
            } else {
                for(;;){
                    Reader.ProcessStatus status = opReader.process(bbin);
                    switch (status) {
                        case DONE -> {
                            opCode = opReader.get();
                            System.out.println("Received: " + opCode);
                            opReader.reset();
                            processIn();
                            return;
                        }
                        case REFILL -> {
                            return;
                        }
                        case ERROR -> {
                            System.out.println("CLOSE opcode");
                            silentlyClose();
                            return;
                        }
                    }
                }

            }
        }

        /**
         * Tries to init the client according to its login
         * Asks the server to verify if the specified login is available
         * Return true if it is, false otherwise
         * @param loginBuffer
         * @return
         */

        /**
         * Add a message to the message queue, tries to fill bbOut and updateInterestOps
         *
         * @param buffMsg
         */
        private void queueMessage(ByteBuffer buffMsg) {
            queue.add(buffMsg);

            processOut();
            updateInterestOps();
        }

        /**
         * Try to fill bbout from the message queue
         */
        private void processOut() {
            while (!queue.isEmpty() && bbout.remaining() > Integer.BYTES){
                var notRemovedBuffMsg = queue.peek();

                if(bbout.remaining() < notRemovedBuffMsg.remaining()) {
                    return;
                }

                bbout.put(notRemovedBuffMsg);
                queue.remove();
            }
        }

        /**
         * Update the interestOps of the key looking
         * only at values of the boolean closed and
         * of both ByteBuffers.
         *
         * The convention is that both buffers are in write-mode before the call
         * to updateInterestOps and after the call.
         * Also it is assumed that process has been be called just
         * before updateInterestOps.
         */

        private void updateInterestOps() {
            int newInterestOps = 0;
            if (bbin.hasRemaining() && !closed) {
                newInterestOps |= SelectionKey.OP_READ;
            }
            if (bbout.position()!=0) {
                newInterestOps |= SelectionKey.OP_WRITE;
            }
            if (newInterestOps == 0) {
                silentlyClose();
            } else {
                key.interestOps(newInterestOps);
            }
        }

        private void silentlyClose() {
            try {
                sc.close();
            } catch (IOException e) {
                // ignore exception
            }
        }

        /**
         * Performs the read action on sc
         *
         * The convention is that both buffers are in write-mode before the call
         * to doRead and after the call
         *
         * @throws IOException
         */
        private void doRead() throws IOException {
            if (sc.read(bbin) == -1) {
                closed = true;
            }
            processInOpCode();
            updateInterestOps();
        }

        /**
         * Performs the write action on sc
         *
         * The convention is that both buffers are in write-mode before the call
         * to doWrite and after the call
         *
         * @throws IOException
         */

        private void doWrite() throws IOException {
            bbout.flip();
            sc.write(bbout);
            bbout.compact();
            processOut();
            updateInterestOps();
        }

        public boolean isInitialized() {
            return login != null;
        }
    }

    static private final int STRING_SIZE = 1_024;
    static private final Logger logger = Logger.getLogger(ChatOSServer.class.getName());

    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;

    public ChatOSServer(int port) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        selector = Selector.open();
    }

    public void launch() throws IOException {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while(!Thread.interrupted()) {
            printKeys(); // for debug
            System.out.println("Starting select");
            try {
                selector.select(this::treatKey);
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
            System.out.println("Select finished");
        }
    }

    private void treatKey(SelectionKey key) {
        printSelectedKey(key); // for debug
        try {
            if (key.isValid() && key.isAcceptable()) {
                doAccept(key);
            }
        } catch(IOException ioe) {
            // lambda call in select requires to tunnel IOException
            throw new UncheckedIOException(ioe);
        }
        try {
            if (key.isValid() && key.isWritable()) {
                ((Context) key.attachment()).doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                ((Context) key.attachment()).doRead();
            }
        } catch (IOException e) {
            logger.log(Level.INFO,"Connection closed with client due to IOException",e);
            silentlyClose(key);
        }
    }

    private void doAccept(SelectionKey key) throws IOException {
        var socketChannel = serverSocketChannel.accept();
        if (socketChannel == null) {
            logger.warning("The selector was wrong");
            return;
        }
        socketChannel.configureBlocking(false);
        var clientKey = socketChannel.register(selector, SelectionKey.OP_READ);
        clientKey.attach(new Context(this, clientKey));
    }

    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    private boolean isLoginAvailable(String login){
        logger.info("Checking availability for " + login);
        for (SelectionKey selectionKey : selector.keys()) {
            if (!selectionKey.channel().equals(serverSocketChannel)) {
                var context = (Context)selectionKey.attachment();
                if(context.isInitialized()) System.out.println("Found: " + context.login);
                if (context.isInitialized() && context.login.equals(login)) {
                    return false;
                }
            }
        }
        return true;
    }


    private void sendPersonalMessage(String login, ByteBuffer buffMsg) {
        logger.info("Sending " + buffMsg + " to " + login);
        for (SelectionKey selectionKey : selector.keys()) {
            if (!selectionKey.channel().equals(serverSocketChannel)) {
                var context = (Context)selectionKey.attachment();
                if (context.isInitialized() && context.login.equals(login)) {
                    context.queueMessage(buffMsg);
                    return;
                }
            }
        }
    }

    /**
     * Add a message to all connected clients queue
     *
     * @param buffMsg
     */
    private void broadcast(ByteBuffer buffMsg) {
        selector.keys().forEach(selectionKey -> {
            if (!selectionKey.channel().equals(serverSocketChannel)) {
                var context = (Context)selectionKey.attachment();
                if(context.isInitialized()) {
                    System.out.println("Sending: " + buffMsg + " to " + context.login);
                    context.queueMessage(buffMsg);
                }
            }
        });
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length!=1){
            usage();
            return;
        }
        new ChatOSServer(Integer.parseInt(args[0])).launch();
    }

    private static void usage(){
        System.out.println("Usage : ChatOSServer port");
    }

    /***
     *  Theses methods are here to help understanding the behavior of the selector
     ***/

    private String interestOpsToString(SelectionKey key){
        if (!key.isValid()) {
            return "CANCELLED";
        }
        int interestOps = key.interestOps();
        ArrayList<String> list = new ArrayList<>();
        if ((interestOps&SelectionKey.OP_ACCEPT)!=0) list.add("OP_ACCEPT");
        if ((interestOps&SelectionKey.OP_READ)!=0) list.add("OP_READ");
        if ((interestOps&SelectionKey.OP_WRITE)!=0) list.add("OP_WRITE");
        return String.join("|",list);
    }

    public void printKeys() {
        Set<SelectionKey> selectionKeySet = selector.keys();
        if (selectionKeySet.isEmpty()) {
            System.out.println("The selector contains no key : this should not happen!");
            return;
        }
        System.out.println("The selector contains:");
        for (SelectionKey key : selectionKeySet){
            SelectableChannel channel = key.channel();
            if (channel instanceof ServerSocketChannel) {
                System.out.println("\tKey for ServerSocketChannel : "+ interestOpsToString(key));
            } else {
                SocketChannel sc = (SocketChannel) channel;
                System.out.println("\tKey for Client "+ remoteAddressToString(sc) +" : "+ interestOpsToString(key));
            }
        }
    }

    private String remoteAddressToString(SocketChannel sc) {
        try {
            return sc.getRemoteAddress().toString();
        } catch (IOException e){
            return "???";
        }
    }

    public void printSelectedKey(SelectionKey key) {
        SelectableChannel channel = key.channel();
        if (channel instanceof ServerSocketChannel) {
            System.out.println("\tServerSocketChannel can perform : " + possibleActionsToString(key));
        } else {
            SocketChannel sc = (SocketChannel) channel;
            System.out.println("\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
        }
    }

    private String possibleActionsToString(SelectionKey key) {
        if (!key.isValid()) {
            return "CANCELLED";
        }
        ArrayList<String> list = new ArrayList<>();
        if (key.isAcceptable()) list.add("ACCEPT");
        if (key.isReadable()) list.add("READ");
        if (key.isWritable()) list.add("WRITE");
        return String.join(" and ",list);
    }
}
