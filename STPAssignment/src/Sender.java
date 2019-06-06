import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Sender {
    private final int DEFAULT_RECEIVE_LENGTH = 4098;
    private final int DEFAULT_SOCKET_TIMEOUT = 100;

    private InetAddress address;
    private int port;
    private String fileName;
    private int MWS;
    private int MSS;
    private long timeout;
    private double pdrop;
    private int seed;
    private Random random;

    private int seq;
    private int ack;

    private DatagramSocket socket;
    private byte[] fileData;
    private STPSegment ackSegment;

    private Log logger;

    private boolean isReceiving = true;
    private boolean isSending = true;

    private int fileDataLength;

    // 滑动窗口
    private int p1;
    private int p2;
    private int p3;

    private ConcurrentLinkedDeque<STPSegment> cache; // 用于存放发出但还未接收的报文
    private List<Integer> receivedSeq;
    private List<Integer> sentSeq = new ArrayList<>();

    private Thread ackReceiver = new Thread() {
        public void run() {
            while (isReceiving && !isInterrupted()) {
                try {
                        receiveSegment();
                        synchronized (currentThread()) {
                        if (ackSegment == null)
                            break;
                        // TODO Seq和Ack的处理
                        int temp = ackSegment.getAck();
                        for (int i : sentSeq) {
                            if (i < temp && !receivedSeq.contains(i)) {
                                receivedSeq.add(i);
                            }
                        }
                        if (temp > seq) {
                            seq = temp;
                            p1 = seq - 1;
                            p3 = p1 + MWS;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private Thread dataSender = new Thread() {
        public void run() {
            while (isSending) {
                synchronized (currentThread()) {
                    try {
                        int selectedLength = (p2 + MSS <= fileDataLength) ? MSS : (fileDataLength - p2);
                        if (p2 + selectedLength > p3) selectedLength = p3 - p2;
                        if (selectedLength > 0) {
                            byte[] selectedData = Arrays.copyOfRange(fileData, p2, p2 + selectedLength);
                            STPSegment segment = new STPSegment(seq, ack, STPSegment._DAT, selectedData);
                            sendSegmentWithPLD(segment);
                            p2 += selectedLength;
                        }
                        if (p2 >= fileDataLength) {
                            isSending = false;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private Thread resender = new Thread() {
        public void run() {
            while (isReceiving) {
                // TODO
                if (!cache.isEmpty()) {

                    synchronized (currentThread()) {
                        STPSegment peek = cache.peekFirst();
                        if (!receivedSeq.contains(peek.getSeq())
                                && (System.nanoTime() - peek.getTimestamp()) / 1000000 > timeout) {
                            STPSegment segment = cache.pop();
                            try {
                                sendSegmentWithPLD(segment, Log.AdditionalType.RETRANS);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (receivedSeq.contains(peek.getSeq())) {
                            cache.pop();

                        }
                    }
                } else if (!isSending) {isReceiving = false; ackReceiver.interrupt();}
            }

        }
    };

    private void start() throws IOException {
        socket = new DatagramSocket();
        socket.setSoTimeout(DEFAULT_SOCKET_TIMEOUT);
        // 第一次握手
        STPSegment segment = new STPSegment(this.seq, this.ack, STPSegment._SYN);
        logger.start();
        sendSegment(segment);

        // 接收ACK
        // TODO seq和ack的处理
        receiveSegment();
        this.seq = ackSegment.getAck();
        this.ack = ackSegment.getSeq() + 1;

        // 第三次握手
        segment = new STPSegment(this.seq, this.ack, STPSegment._ACK);
        sendSegment(segment);
    }

    private void send() {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.execute(dataSender);
        executorService.execute(ackReceiver);
        executorService.execute(resender);
        executorService.shutdown();
        while (!executorService.isTerminated());
    }

    private void close() throws IOException {
        STPSegment segment = new STPSegment(this.seq, this.ack, STPSegment._FIN);
        sendSegment(segment);
        receiveSegment();
        receiveSegment();
        segment = new STPSegment(this.seq, this.ack, STPSegment._FIN);
        sendSegment(segment);
    }


    private long sendSegment(STPSegment segment) throws IOException {
        long timestamp = System.nanoTime();
        segment.setTimestamp(timestamp);
        socket.send(segment.toDatagramPacket(address, port));
        logger.log(segment, Log.Type.SND, timestamp);
        return timestamp;
    }

    private long sendSegmentWithPLD(STPSegment segment) throws IOException {
        long timestamp = System.nanoTime();

        segment.setTimestamp(timestamp);
        if (random.nextDouble() > pdrop) {

            socket.send(segment.toDatagramPacket(address, port));
            sentSeq.add(segment.getSeq());
            logger.log(segment, Log.Type.SND, timestamp);
        } else {
            logger.log(segment, Log.Type.DROP, timestamp);
        }
        cache.addLast(segment);
        return timestamp;
    }

    private long sendSegmentWithPLD(STPSegment segment, Log.AdditionalType type) throws IOException {
        long timestamp = System.nanoTime();
        segment.setTimestamp(timestamp);
        if (random.nextDouble() > pdrop) {
            socket.send(segment.toDatagramPacket(address, port));
            sentSeq.add(segment.getSeq());
            logger.log(segment, Log.Type.SND, type, timestamp);
        } else {
            logger.log(segment, Log.Type.DROP, type, timestamp);
        }
        cache.addLast(segment);
        return timestamp;
    }

    private void receiveSegment() throws IOException {
        try {
            DatagramPacket receivedPacket = new DatagramPacket(new byte[DEFAULT_RECEIVE_LENGTH], DEFAULT_RECEIVE_LENGTH);
            socket.receive(receivedPacket);
            long timestamp = System.nanoTime();
            ackSegment = STPSegment.toSTPSegment(receivedPacket);
            logger.log(ackSegment, Log.Type.RCV, timestamp);
        } catch (SocketTimeoutException e) {
            ackSegment = null;
        }
    }


    private byte[] readFile(String fileName) throws IOException {
        File file = new File(fileName);
        int length = (int) file.length();
        byte[] allData = new byte[length];
        FileInputStream fileInputStream = new FileInputStream(file);
        fileInputStream.read(allData);
        fileInputStream.close();
        return allData;
    }

    private Sender(String[] args) throws Exception {
        this.address = InetAddress.getByName(args[0]);
        this.port = Integer.parseInt(args[1]);
        this.fileName = args[2];
        this.MWS = Integer.parseInt(args[3]);
        this.MSS = Integer.parseInt(args[4]);
        this.timeout = Long.parseLong(args[5]);
        this.pdrop = Double.parseDouble(args[6]);
        this.seed = Integer.parseInt(args[7]);
        this.random = new Random(seed);
        this.fileData = readFile(fileName);
        this.fileDataLength = fileData.length;
        this.logger = new Log("sender_Log.txt", true);
        this.seq = 0;
        this.ack = 0;
        this.cache = new ConcurrentLinkedDeque<>();
        this.receivedSeq = new ArrayList<>();
        p1 = 0;
        p2 = 0;
        p3 = MWS;
    }

    public static void main(String[] args) {
        if (args.length != 8)
            System.out.println("usage: java Sender receiver_host_ip receiver_port file.txt MWS MSS timeout pdrop seed");
        else {
            try {
                Sender sender = new Sender(args);
                sender.start();
                sender.send();
                sender.close();
                sender.logger.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
