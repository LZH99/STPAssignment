import java.io.IOException;
import java.io.PrintWriter;

public class Log {
    private PrintWriter printWriter;
    private String fileName;
    private boolean isSender;

    public enum Type {SND, RCV, DROP}

    public enum AdditionalType {RETRANS, DUPLICATED}

    // 一些统计信息，其中部分信息Receiver用不到
    // 但考虑到Receiver也是Sender
    // 故共用Log类
    // Sender用到的
    private int dataTransferred = 0; // 单位：字节
    private int dataSegmentSent = 0; // 不包含重传的Segment
    private int packetsDropped = 0; // 通过PLD模块
    // private int packetsDelayed = 0; // 附加题，暂时不实现
    private int retransmittedSegments = 0;
    private int duplicatedAckReceived = 0; // 需要实现吗？

    // Receiver用到的
    private int dataReceived = 0; // 单位：字节，不包含重传的数据
    private int dataSegmentsReceived = 0; // original
    private int duplicatedSegmentsReceived = 0; // if any

    private long startTime;

    private boolean isStarted = false;

    public Log(String fileName, boolean isSender) {
        this.isSender = isSender;
        this.fileName = fileName;
    }

    public void start() throws IOException {
        isStarted = true;
        startTime = System.nanoTime();
        printWriter = new PrintWriter(fileName);
        printWriter.println("<snd/rcv/drop> <time> <type of packet> <seq-number> <number-of- bytes> <ack-number>");
    }

    @Deprecated // 为防止时间的偏差，尽量不用这个方法
    public void log(STPSegment segment, Type type) {
        log(segment, type, System.nanoTime() / 1000000.0);
    }

    @Deprecated // 为防止时间的偏差，尽量不用这个方法
    public void log(STPSegment segment, Type type, AdditionalType additionalType) {
        log(segment, type, additionalType, System.nanoTime() / 1000000.0);
    }

    // TODO
    public void log(STPSegment segment, Type type, double time) {
        StringBuilder builder = new StringBuilder();
        builder.append(type).append(" ");
        builder.append(String.format("%.3f", (time - startTime) / 1000000.0)).append(" ");
        String segType = "";
        if (segment.isData()) segType = "D";
        else if (segment.isFIN()) segType = "F";
        else if (segment.isSYN()) segType = "S";
        if (!segment.isData() && segment.isACK()) segType += "A";
        builder.append(segType).append(" ");
        builder.append(segment.getSeq()).append(" ");
        builder.append(segment.getData().length).append(" ");
        builder.append(segment.getAck());

        printWriter.println(builder.toString());

        switch (type) {
            case SND:
                if (segment.isData()) {
                    dataTransferred += segment.getLength();
                    dataSegmentSent += 1;
                }
                break;
            case RCV:
                if (segment.isData()) {
                    dataReceived += segment.getLength();
                    dataSegmentsReceived += 1;
                }
                break;
            case DROP:
                packetsDropped += 1;
                break;
            default:
                break;
        }
    }

    public void log(STPSegment segment, Type type, AdditionalType additionalType, double time) {
        if (additionalType == AdditionalType.RETRANS)
            retransmittedSegments += 1;
        if (additionalType == AdditionalType.DUPLICATED) {
            if (type == Type.RCV) {
                if (isSender) duplicatedAckReceived += 1;
                else {
                    dataSegmentsReceived -= 1;
                    duplicatedSegmentsReceived += 1;
                }
            }
        }
        log(segment, type, time);
    }

    public void printStatistics() {
        printWriter.println("===================================");
        if (isSender) {
            printWriter.println("Amount of (original) Data Transferred (in bytes): " + dataTransferred);
            printWriter.println("Number of Data Segments Sent (excluding retransmissions): " + dataSegmentSent);
            printWriter.println("Number of (all) Packets Dropped (by the PLD module): " + packetsDropped);
            printWriter.println("Number of Retransmitted Segments: " + retransmittedSegments);
            printWriter.println("Number of Duplicate Acknowledgements received: " + duplicatedAckReceived);
        } else {
            printWriter.println("Amount of (original) Data Received (in bytes) – do not include retransmitted data: " + dataReceived);
            printWriter.println("Number of (original) Data Segments Received: " + dataSegmentsReceived);
            printWriter.println("Number of duplicate segments received (if any): " + duplicatedSegmentsReceived);
        }
    }

    public void close() {
        isStarted = false;
        printStatistics();
        printWriter.close();
    }
}
