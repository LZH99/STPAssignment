import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Receiver {
    private final int DEFAULT_SEGMENT_SIZE = 4098;

    private InetAddress ackAddress;
    private int ackPort;
    private int port;
    private String fileName;

    private int seq;
    private int ack;

    private PrintWriter writer;
    private Log logger;
    private DatagramSocket socket;

    private STPSegment receivedSegment;

    private boolean isReceiving = true;
    private boolean isSending = true;

    private ConcurrentHashMap<Integer, STPSegment> cache;
    private ConcurrentLinkedDeque<STPSegment> fileBuffer;
    private ConcurrentLinkedDeque<STPSegment> ackList;

    private Thread dataReceiver = new Thread() {
        public void run() {
            while (isReceiving) {
                try {
                    receivedSegment = receive();
                    synchronized (currentThread()) {
                        if (receivedSegment.isData() && !cache.containsKey(receivedSegment.getSeq())) {
                            cache.put(receivedSegment.getSeq(), receivedSegment);
                            if (receivedSegment.getSeq() == ack) {
                                fileBuffer.add(receivedSegment);
                                ackList.add(receivedSegment);
                                while (cache.containsKey(ack + receivedSegment.getLength())) {
                                    receivedSegment = cache.get(ack + receivedSegment.getLength());
                                    fileBuffer.add(receivedSegment);
                                    ackList.add(receivedSegment);
                                    ack = receivedSegment.getAck() + receivedSegment.getLength();
                                }
                                ack = receivedSegment.getSeq() + receivedSegment.getLength();
                            }
                        } else if (receivedSegment.isFIN()) {
                            isReceiving = false;
                            ack = receivedSegment.getSeq() + receivedSegment.getLength();
                            STPSegment segment = new STPSegment(seq, ack, STPSegment._ACK);
                            sendSegment(segment);
                        } else if (receivedSegment.isData() && !cache.containsKey(receivedSegment.getSeq()))
                            logger.log(receivedSegment, Log.Type.RCV, Log.AdditionalType.DUPLICATED, System.nanoTime());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private Thread fileWriter = new Thread() {
        public void run() {
            while (isReceiving || !fileBuffer.isEmpty()) {
                synchronized (currentThread()) {
                    if (!fileBuffer.isEmpty()) {
                        STPSegment segment = fileBuffer.pop();
                        writer.print(new String(segment.getData()));
                    }
                }
            }
            writer.close();
        }
    };


    private Thread ackSender = new Thread() {
        public void run() {
            while (isReceiving || !ackList.isEmpty()) {
                synchronized (currentThread()) {
                    if (!ackList.isEmpty()) {
                        try {
                            // TODO 序号相关问题
                            STPSegment rec = ackList.pop();
                            int temp = rec.getSeq() + rec.getLength();
                            STPSegment segment = new STPSegment(seq, temp, STPSegment._ACK);
                            sendSegment(segment);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    };

    private void start() throws IOException {
        // 接收第一次握手
        DatagramPacket packet = new DatagramPacket(new byte[DEFAULT_SEGMENT_SIZE], DEFAULT_SEGMENT_SIZE);
        logger.start();
        socket.receive(packet);
        receivedSegment = STPSegment.toSTPSegment(packet);
        seq = receivedSegment.getAck();
        ack = receivedSegment.getSeq() + 1;

        // 得到发送方的信息
        ackAddress = packet.getAddress();
        ackPort = packet.getPort();

        writer = new PrintWriter(fileName);

        // 第二次握手
        STPSegment segment = new STPSegment(this.seq, this.ack, (byte) (STPSegment._SYN + STPSegment._ACK));
        sendSegment(segment);

        // 接收第三次握手
        receivedSegment = receive();
        seq = receivedSegment.getAck();
        ack = receivedSegment.getSeq();
    }

    public void receiveData() {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.execute(dataReceiver);
        executorService.execute(fileWriter);
        executorService.execute(ackSender);
        executorService.shutdown();
        while (!executorService.isTerminated()) ;
    }

    private void close() throws IOException {
        STPSegment segment = new STPSegment(seq, ack, (byte) (STPSegment._FIN + STPSegment._ACK));
        sendSegment(segment);
        DatagramPacket packet = new DatagramPacket(new byte[DEFAULT_SEGMENT_SIZE], DEFAULT_SEGMENT_SIZE);
        socket.receive(packet);
        segment = STPSegment.toSTPSegment(packet);
        logger.close();
    }


    private void sendSegment(STPSegment segment) throws IOException {
        long timestamp = System.nanoTime();
        segment.setTimestamp(timestamp);
        socket.send(segment.toDatagramPacket(ackAddress, ackPort));
        logger.log(segment, Log.Type.SND, timestamp);
    }

    private STPSegment receive() throws IOException {
        DatagramPacket receivedPacket = new DatagramPacket(new byte[DEFAULT_SEGMENT_SIZE], DEFAULT_SEGMENT_SIZE);
        socket.receive(receivedPacket);
        long time = System.nanoTime();
        receivedSegment = STPSegment.toSTPSegment(receivedPacket);
        logger.log(receivedSegment, Log.Type.RCV, time);
        return receivedSegment;
    }


    public Receiver(String[] args) throws IOException {
        port = Integer.parseInt(args[0]);
        fileName = args[1];
        logger = new Log("receiver_Log.txt", false);
        socket = new DatagramSocket(port, InetAddress.getByName("localhost"));
        cache = new ConcurrentHashMap<>();
        fileBuffer = new ConcurrentLinkedDeque<>();
        ackList = new ConcurrentLinkedDeque<>();
        ack = 0;
        seq = 0;
    }

    public static void main(String[] args) {
        if (args.length != 2)
            System.out.println("usage: java Receiver receiver_port file.txt");
        else {
            try {
                Receiver receiver = new Receiver(args);
                receiver.start();
                receiver.receiveData();
                receiver.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
