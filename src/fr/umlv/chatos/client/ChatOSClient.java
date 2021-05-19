package fr.umlv.chatos.client;

import fr.umlv.chatos.readers.Reader.ProcessStatus;
import fr.umlv.chatos.readers.global.GlobalMessage;
import fr.umlv.chatos.readers.global.GlobalMessageReader;
import fr.umlv.chatos.readers.initialization.InitializationMessage;
import fr.umlv.chatos.readers.initialization.InitializationMessageReader;
import fr.umlv.chatos.readers.personal.PersonalMessage;
import fr.umlv.chatos.readers.personal.PersonalMessageReader;
import fr.umlv.chatos.readers.serverop.ServerErrorCode;
import fr.umlv.chatos.readers.serverop.ServerErrorReader;
import fr.umlv.chatos.readers.serverop.ServerMessageOpCode;
import fr.umlv.chatos.readers.serverop.ServerOpReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import static fr.umlv.chatos.client.CommandTransformer.asByteBuffer;

public class ChatOSClient {
    static private class Context {

        final private SelectionKey key;
        final private SocketChannel sc;
        final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
        final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
        final private Queue<ByteBuffer> queue = new LinkedList<>(); // buffers read-mode

        final private ServerOpReader serverOpReader = new ServerOpReader();
        final private ServerErrorReader serverErrorReader = new ServerErrorReader();
        final private InitializationMessageReader initializationMessageReader = new InitializationMessageReader();
        final private GlobalMessageReader globalMessageReader = new GlobalMessageReader();
        final private PersonalMessageReader personalMessageReader = new PersonalMessageReader();

        private boolean closed = false;

        private Context(SelectionKey key) {
            this.key = key;
            this.sc = (SocketChannel) key.channel();
        }

        /**
         * Process the content of bbin
         * <p>
         * The convention is that bbin is in write-mode before the call
         * to process and after the call
         */
        private void processIn() {
            ProcessStatus status = serverOpReader.process(bbin);
            switch(status){
                case DONE:
                    ServerMessageOpCode opCode = serverOpReader.get();
                    logger.info("Received opcode: " + opCode);
                    processOpCode(opCode);
                    serverOpReader.reset();
                case REFILL:
                    System.out.println("Refill processIn");
                    return;
                case ERROR: {
                    System.out.println("Error processIn");
                    silentlyClose();
                    return;
                }
            }
        }

        private void processOpCode(ServerMessageOpCode opCode){
            System.out.println("Received: " + opCode);
            switch (opCode){
                case SUCCESS:
                    processInitializationMessage();
                    return;
                case GLOBAL_MESSAGE:
                    processGlobalMessage();
                    return;
                case PERSONAL_MESSAGE:
                    processPersonalMessage();
                    return;
                case PRIVATE_CONNECTION_ESTABLISHMENT:
                    processSuccessConnexion();
                case FAIL:
                    for(;;){
                        ProcessStatus status = serverErrorReader.process(bbin);
                        switch (status){
                            case DONE:
                                ServerErrorCode errorCode = serverErrorReader.get();
                                System.out.println("Done. ErrorCode: " + errorCode);
                                serverErrorReader.reset();
                                return;
                            case REFILL: continue;
                            case ERROR:
                                logger.severe("ErrorStatus. Closing.");
                                silentlyClose();
                                return;
                        }
                    }
            }
        }

        private void processInitializationMessage(){
            for(;;) {
                ProcessStatus status = initializationMessageReader.process(bbin);
                switch (status) {
                    case DONE:
                        InitializationMessage message = initializationMessageReader.get();
                        System.out.println(message);
                        initializationMessageReader.reset();
                        return;
                    case REFILL:
                        continue;
                    case ERROR: {
                        logger.severe("ErrorStatus. Closing.");
                        silentlyClose();
                        return;
                    }
                }
            }
        }

        private void processGlobalMessage(){
            for(;;) {
                ProcessStatus status = globalMessageReader.process(bbin);
                switch (status) {
                    case DONE:
                        GlobalMessage message = globalMessageReader.get();
                        System.out.println(message);
                        globalMessageReader.reset();
                        return;
                    case REFILL:
                        continue;
                    case ERROR: {
                        logger.severe("ErrorStatus. Closing.");
                        silentlyClose();
                        return;
                    }
                }
            }
        }

        private void processPersonalMessage(){
            for(;;) {
                ProcessStatus status = personalMessageReader.process(bbin);
                switch (status) {
                    case DONE:
                        System.out.println("DONE perso");
                        PersonalMessage message = personalMessageReader.get();
                        System.out.println(message);
                        System.out.println(message);
                        personalMessageReader.reset();
                        return;
                    case REFILL:
                        System.out.println("REFILL perso");
                        continue;
                    case ERROR: {
                        logger.severe("ErrorStatus. Closing.");
                        silentlyClose();
                        return;
                    }
                }
            }
        }

        private void processSuccessConnexion(){
            //TODO
        }

        /**
         * Add a message to the message queue, tries to fill bbOut and updateInterestOps
         *
         * @param bb
         */
        private void queueMessage(ByteBuffer bb) {
            synchronized (queue) {
                queue.add(bb);
                processOut();
                updateInterestOps();
            }
        }

        /**
         * Try to fill bbout from the message queue
         */
        private void processOut() {
            while(!queue.isEmpty()){
                var bb = queue.peek();
                if(bb.remaining() <= bbout.remaining()){
                    queue.remove();
                    bbout.put(bb);
                } else break;
            }
        }

        /**
         * Update the interestOps of the key looking
         * only at values of the boolean closed and
         * of both ByteBuffers.
         * <p>
         * The convention is that both buffers are in write-mode before the call
         * to updateInterestOps and after the call.
         * Also it is assumed that process has been be called just
         * before updateInterestOps.
         */

        private void updateInterestOps() {
            var interesOps=0;
            if (!closed && bbin.hasRemaining()){
                interesOps=interesOps|SelectionKey.OP_READ;
            }
            if (bbout.position()!=0){
                interesOps|=SelectionKey.OP_WRITE;
            }
            if (interesOps==0){
                silentlyClose();
                return;
            }
            key.interestOps(interesOps);
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
         * <p>
         * The convention is that both buffers are in write-mode before the call
         * to doRead and after the call
         *
         * @throws IOException
         */
        private void doRead() throws IOException {
            if (sc.read(bbin) == -1) {
                closed = true;
            }
            processIn();
            updateInterestOps();
        }

        /**
         * Performs the write action on sc
         * <p>
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

        public void doConnect() throws IOException {
            // TODO
            if (!sc.finishConnect()) {
                return; //bad hint
            }
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    static private int BUFFER_SIZE = 10_000;
    static private Logger logger = Logger.getLogger(ChatOSClient.class.getName());


    private final SocketChannel sc;
    private final Selector selector;
    private final InetSocketAddress serverAddress;
    private final String login;
    private final Thread console;
    private final ArrayBlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
    private Context uniqueContext;

    public ChatOSClient(String login, InetSocketAddress serverAddress) throws IOException {
        this.serverAddress = serverAddress;
        this.login = login;
        this.sc = SocketChannel.open();
        this.selector = Selector.open();
        this.console = new Thread(this::consoleRun);
    }

    private void consoleRun() {
        try {
            var scan = new Scanner(System.in);
            while (scan.hasNextLine()) {
                var msg = scan.nextLine();
                sendCommand(msg);
            }
        } catch (InterruptedException e) {
            logger.info("Console thread has been interrupted");
        } finally {
            logger.info("Console thread stopping");
        }
    }

    /**
     * Send a command to the selector via commandQueue and wake it up
     *
     * @param msg
     * @throws InterruptedException
     */
    private void sendCommand(String msg) throws InterruptedException {
        // TODO
        synchronized (commandQueue){
            commandQueue.put(msg);
            selector.wakeup();
        }
    }

    /**
     * Processes the command from commandQueue
     */
    private void processCommands(){
        // TODO
        synchronized (commandQueue){
            while(!commandQueue.isEmpty()){
                String commandLine = commandQueue.poll();
                Optional<ByteBuffer> optional = asByteBuffer(commandLine);
                if(optional.isEmpty()) {
                    System.out.println("Could not process: " + commandLine);
                    continue;
                }
                ByteBuffer commandBuffer = optional.get();
                uniqueContext.queueMessage(commandBuffer);
            }
        }
    }

    /**
     * Start the client
     * @throws IOException
     */
    public void launch() throws IOException {
        sc.configureBlocking(false);
        var key = sc.register(selector, SelectionKey.OP_CONNECT);
        uniqueContext = new Context(key);
        key.attach(uniqueContext);
        sc.connect(serverAddress);

        console.start();

        while(!Thread.interrupted()) {
            try {
                selector.select(this::treatKey);
                processCommands();
            } catch (UncheckedIOException tunneled) {
                throw tunneled.getCause();
            }
        }
    }

    private void treatKey(SelectionKey key) {
        try {
            if (key.isValid() && key.isConnectable()) {
                uniqueContext.doConnect();
            }
            if (key.isValid() && key.isWritable()) {
                uniqueContext.doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                uniqueContext.doRead();
            }
        } catch(IOException ioe) {
            // lambda call in select requires to tunnel IOException
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Safely close the channel associated to the given key
     * @param key
     */
    private void silentlyClose(SelectionKey key) {
        Channel sc = (Channel) key.channel();
        try {
            sc.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    /**
     * Main function of the client
     * @param args
     * @throws NumberFormatException
     * @throws IOException
     */
    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length!=3){
            usage();
            return;
        }
        new ChatOSClient(args[0],new InetSocketAddress(args[1],Integer.parseInt(args[2]))).launch();
    }

    /**
     * Defines how to launch client
     */
    private static void usage(){
        System.out.println("Usage : ChatOsClient login hostname port");
    }
}
