package fr.umlv.chatos.client;

import fr.umlv.chatos.client.command.CommandTransformer;
import fr.umlv.chatos.readers.Reader;
import fr.umlv.chatos.readers.Reader.ProcessStatus;
import fr.umlv.chatos.readers.initialization.InitializationMessage;
import fr.umlv.chatos.readers.initialization.InitializationMessageReader;
import fr.umlv.chatos.readers.serverglobal.ServerGlobalMessage;
import fr.umlv.chatos.readers.serverglobal.ServerGlobalMessageReader;
import fr.umlv.chatos.readers.opcode.OpCode;
import fr.umlv.chatos.readers.opcode.OpCodeReader;
import fr.umlv.chatos.readers.personal.PersonalMessage;
import fr.umlv.chatos.readers.personal.PersonalMessageReader;
import fr.umlv.chatos.readers.errorcode.ErrorCode;
import fr.umlv.chatos.readers.errorcode.ErrorCodeReader;
import fr.umlv.chatos.readers.trame.Trame;
import fr.umlv.chatos.readers.trame.TrameReader;

import fr.umlv.chatos.readers.privateconnection.acceptationrequest.*;
import fr.umlv.chatos.readers.privateconnection.clientestablishment.*;
import fr.umlv.chatos.readers.privateconnection.response.*;
import fr.umlv.chatos.readers.privateconnection.request.*;

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

public class ChatOSClient {
    static private class Context {

        final private SelectionKey key;
        final private SocketChannel sc;
        final private ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
        final private ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);
        final private Queue<Trame> queue = new LinkedList<>(); // buffers read-mode

        final private Reader<Trame> trameReader = new TrameReader();
        final private ClientTrameVisitor visitor = new ClientTrameVisitor();

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
            ProcessStatus status = trameReader.process(bbin);
            switch (status) {
                case DONE -> {
                    Trame trame = trameReader.get();
                    treatFrame(trame);
                    trameReader.reset();
                    return;
                }
                case REFILL -> {
                }
                case ERROR -> {
                    System.out.println("Error while processing input. Closing.");
                    silentlyClose();
                    return;
                }
            }
        }

        private void treatFrame(Trame frame){
            frame.accept(visitor);
        }

        /**
         * Add a message to the message queue, tries to fill bbOut and updateInterestOps
         *
         * @param trame
         */
        private void queueMessage(Trame trame) {
            synchronized (queue) {
                if(queue.size() == QUEUE_MAX_SIZE){
                    System.out.println("Too many commands to process. Closing.");
                    silentlyClose();
                } else {
                    queue.add(trame);
                    System.out.println("Added: " + trame.toString());
                    processOut();
                    updateInterestOps();
                }
            }
        }

        /**
         * Try to fill bbout from the message queue
         */
        private void processOut() {
            while(!queue.isEmpty()){
                var trame = queue.peek();
                System.out.println("Peek: " + trame);
                var optional = trame.asByteBuffer(BUFFER_SIZE);
                if(optional.isPresent()) {
                    var trameBuffer = optional.get();
                    if (trameBuffer.remaining() <= bbout.remaining()) {
                        queue.remove();
                        bbout.put(trameBuffer);
                    } else break;
                } else {
                    System.out.println("Trame too long");
                    queue.remove();
                }
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
    static private int QUEUE_MAX_SIZE = 50;
    static private Logger logger = Logger.getLogger(ChatOSClient.class.getName());


    private final SocketChannel sc;
    private final Selector selector;
    private final InetSocketAddress serverAddress;
    private final Thread console;
    final private CommandTransformer commandTransformer = new CommandTransformer();
    private final ArrayBlockingQueue<String> commandQueue = new ArrayBlockingQueue<>(10);
    private Context uniqueContext;

    public ChatOSClient(InetSocketAddress serverAddress) throws IOException {
        this.serverAddress = serverAddress;
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
                Optional<Trame> optional = commandTransformer.asByteBuffer(commandLine);
                if(optional.isEmpty()) {
                    System.out.println("Could not process: " + commandLine);
                    continue;
                }
                Trame commandBuffer = optional.get();
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
     * Main function of the client
     * @param args
     * @throws NumberFormatException
     * @throws IOException
     */
    public static void main(String[] args) throws NumberFormatException, IOException {
        if (args.length!=2){
            usage();
            return;
        }
        new ChatOSClient(new InetSocketAddress(args[0],Integer.parseInt(args[1]))).launch();
    }

    /**
     * Defines how to launch client
     */
    private static void usage(){
        System.out.println("Usage : ChatOsClient hostname port");
    }
}
