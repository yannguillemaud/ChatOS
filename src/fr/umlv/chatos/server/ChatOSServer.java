package fr.umlv.chatos.server;

import fr.umlv.chatos.readers.Reader;
import fr.umlv.chatos.readers.clientglobal.ClientGlobalMessage;
import fr.umlv.chatos.readers.privateconnection.acceptationrequest.PrivateConnectionAcceptationRequest;
import fr.umlv.chatos.readers.privateconnection.request.PrivateConnectionRequest;
import fr.umlv.chatos.readers.privateconnection.response.PrivateConnectionResponse;
import fr.umlv.chatos.readers.privateconnection.serverestablishment.PrivateConnectionServerEstablishment;
import fr.umlv.chatos.readers.success.SuccessMessage;
import fr.umlv.chatos.readers.trame.Trame;
import fr.umlv.chatos.readers.serverglobal.ServerGlobalMessage;
import fr.umlv.chatos.readers.initialization.InitializationMessage;
import fr.umlv.chatos.readers.personal.PersonalMessage;
import fr.umlv.chatos.readers.trame.TrameReader;
import fr.umlv.chatos.server.token.TokenGenerator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static fr.umlv.chatos.readers.errorcode.ErrorCode.*;

public class ChatOSServer {

    static class Context {
        enum StateContext { PENDING, LOGGED }

        final private SelectionKey key;
        final private SocketChannel sc;
        final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
        final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
        final private Queue<Trame> queue = new LinkedList<>();
        final private ChatOSServer server;
        private boolean closed = false;

        private final TrameReader trameReader = new TrameReader();
        private final ServerFrameVisitor frameVisitor;

        String login = null;
        boolean isInitialized = false;
        StateContext state = StateContext.PENDING;

        private Context(ChatOSServer server, SelectionKey key){
            this.key = key;
            this.sc = (SocketChannel) key.channel();
            this.server = server;
            this.frameVisitor = new ServerFrameVisitor(server, this);
        }

        private void processIn() {
            for(;;){
                Reader.ProcessStatus status = trameReader.process(bbin);
                switch (status) {
                    case DONE -> {
                        System.out.println("DONE");
                        Trame trame = trameReader.get();
                        trameReader.reset();
                        treatFrame(trame);
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

        private void treatFrame(Trame frame){
            frame.accept(frameVisitor);
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
         * @param trame
         */
        void queueMessage(Trame trame) {
            queue.add(trame);

            processOut();
            updateInterestOps();
        }

        /**
         * Try to fill bbout from the message queue
         */
        private void processOut() {
            while (!queue.isEmpty() && bbout.remaining() > Integer.BYTES){
                var sendable = queue.peek();
                var sendableByteBuffer = sendable.asByteBuffer(BUFFER_SIZE);



                if(sendableByteBuffer.isEmpty() || bbout.remaining() < sendableByteBuffer.orElseThrow().remaining()) {
                    return;
                }

                bbout.put(sendableByteBuffer.orElseThrow());
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
                if (isInitialized) {
                    server.delete(login);
                }
            }
            processIn();
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
            return isInitialized;
        }
    }

    static private final Logger logger = Logger.getLogger(ChatOSServer.class.getName());
    static final int BUFFER_SIZE = 1024;

    private final HashMap<String, Context> contextMap = new HashMap<>();
    private final HashMap<Map.Entry<String, String>, Boolean> privateConnections = new HashMap<>();
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

    private void delete(String login) {
        logger.info("Delete user " + login);
        contextMap.remove(login);
    }

    boolean isRegistered(String login){
        logger.info("Checking availability of " + login);
        var res = contextMap.get(login);
        return res != null;
    }

    boolean trySendTrameTo(String login, Trame trame) {
        var context = contextMap.get(login);
        if (context == null) {
            return false;
        } else {
            context.queueMessage(trame);
            return true;
        }
    }

    /**
     * Add a message to all connected clients queue
     *
     * @param serverGlobalMessage
     */
    void broadcast(ServerGlobalMessage serverGlobalMessage) {
        selector.keys().forEach(selectionKey -> {
            if (!selectionKey.channel().equals(serverSocketChannel)) {
                var context = (Context)selectionKey.attachment();
                if(context.isInitialized()) {
                    System.out.println("Sending: " + serverGlobalMessage.getValue() + " to " + context.login);
                    context.queueMessage(serverGlobalMessage);
                }
            }
        });
    }

    String generateNewToken() {
        return TokenGenerator.token();
    }

    boolean isPrivateConnectionDemandInitiated(String loginSender, String loginReceiver) {
        Boolean connexion = privateConnections.get(Map.entry(loginSender, loginReceiver));
        return connexion != null && !connexion;
    }

    boolean isPrivateConnectionEstablished(String loginSender, String loginReceiver) {
        Boolean connexion = privateConnections.get(Map.entry(loginSender, loginReceiver));
        return connexion != null && connexion;
    }

    void privateConnectionDemandInit(String loginSender, String loginReceiver) {
        privateConnections.put(Map.entry(loginSender, loginReceiver), false);
    }

    void abortPrivateConnectionDemand(String loginSender, String loginReceiver) {
        privateConnections.remove(Map.entry(loginSender, loginReceiver), false);
    }

    void initPrivateConnection(String loginSender, String loginReceiver, String token) {
        privateConnections.computeIfPresent(Map.entry(loginSender, loginReceiver), (k, v) -> true);
    }

    boolean tryRegister(String login, Context context) {
        logger.info("Trying registering for " + login);
        return contextMap.computeIfAbsent(login, key -> context).equals(context);
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
