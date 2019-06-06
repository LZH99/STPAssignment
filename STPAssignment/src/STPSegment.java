import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class STPSegment {
    // flags的相关参数
    public static final byte _ACK = (byte) (1 << 7);
    public static final byte _SYN = (byte) (1 << 6);
    public static final byte _FIN = (byte) (1 << 5);
    public static final byte _DAT = (byte) (1 << 4);
    public static final byte _RM2 = (byte) (1 << 3);
    public static final byte _RM3 = (byte) (1 << 2);
    public static final byte _RM4 = (byte) (1 << 1);
    public static final byte _RM5 = (byte) 1;

    /* ================================报文的格式============================== */
    private int seq; // 序号
    private int ack; // 确认号

    private byte flags; // flags，共8位，前四位分别是_ACK、_SYN、_FIN、_DAT剩下位保留
    private long timestamp = 0; // 时间戳，发送时写入

    private final int length; // 数据区长度，其实后面发现没有什么用
    private byte optLength; // opt长度
    private byte[] opt; // 可选项，便于今后扩展

    // 数据
    private byte[] data;

    /* ====================================================================== */

    public STPSegment(int seq, int ack, byte flags, byte optLength, byte[] opt, byte[] data) {
        if (optLength < 0) throw new IllegalArgumentException();
        this.seq = seq;
        this.ack = ack;
        this.flags = flags;
        this.optLength = optLength;
        this.opt = opt;
        this.data = data;
        this.length = this.data.length;
    }

    public STPSegment(int seq, int ack, byte flags, byte[] data) {
        this(seq, ack, flags, (byte) 0, new byte[0], data);
    }

    public STPSegment(int seq, int ack, byte flags) {
        this(seq, ack, flags, new byte[0]);
    }

    // 封装与拆解
    private byte[] toByteArray() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.writeInt(seq);
        dataOutputStream.writeInt(ack);
        dataOutputStream.writeByte(flags);
        dataOutputStream.writeLong(timestamp);
        dataOutputStream.writeInt(data.length);
        dataOutputStream.writeByte(optLength);
        dataOutputStream.write(opt);
        dataOutputStream.write(data);
        dataOutputStream.flush();
        byte[] result = byteArrayOutputStream.toByteArray();
        dataOutputStream.close();
        byteArrayOutputStream.close();
        return result;
    }


    public DatagramPacket toDatagramPacket(InetAddress address, int port) throws IOException {
        byte[] array = toByteArray();
        return new DatagramPacket(array, array.length, address, port);
    }

    public static STPSegment toSTPSegment(DatagramPacket packet) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(packet.getData());
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        int seq = dataInputStream.readInt();
        int ack = dataInputStream.readInt();
        byte flags = dataInputStream.readByte();
        long timestamp = dataInputStream.readLong();
        int length = dataInputStream.readInt();
        byte optLength = dataInputStream.readByte();
        byte[] opt = new byte[optLength];
        dataInputStream.read(opt);
        byte[] data = new byte[length];
        dataInputStream.read(data);
        STPSegment segment = new STPSegment(seq, ack, flags, optLength, opt, data);
        segment.setTimestamp(timestamp);
        return segment;
    }

    // Getter和Setter，没什么好看的
    public int getSeq() {
        return this.seq;
    }

    public int getAck() {
        return ack;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isData() {
        return (flags & _DAT) != 0 ;
    }

    public boolean isFIN() {
        return (flags & _FIN) != 0;
    }

    public boolean isSYN() {
        return (flags & _SYN) != 0;
    }

    public boolean isACK() {
        return (flags & _ACK) != 0;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getLength() {
        return length;
    }
}
