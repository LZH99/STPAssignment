import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.net.*;
import java.io.File;
import java.io.FileWriter;

public class Receiver
{
    public static final int PORT = 30000;
    // 定义每个数据报的最大大小为4KB
    private static final int DATA_LEN = 228;
    // 定义接收网络数据的字节数组
    byte[] inBuff = new byte[DATA_LEN];
    // 创建DatagramSocket对象
    private DatagramSocket socket = new DatagramSocket(PORT);
    // 以指定字节数组创建准备接收数据的DatagramPacket对象
    private DatagramPacket inPacket = new DatagramPacket(inBuff , inBuff.length);
    // 定义一个用于发送的DatagramPacket对象
    private DatagramPacket outPacket;
    private boolean status = false;
    private String text ="";
    private Map<String,String> map = new HashMap<String,String>();
    private LogProcessor log = new LogProcessor();
    public Receiver() throws SocketException {
    }

    public void handshake2()throws IOException {
        int[] seg = {0, 0, 0, 0, 1, 1, 0, 0, 0, 0};
        log.clear();
        // 读取Socket中的数据，读到的数据放入inPacket封装的数组里
        socket.receive(inPacket);
        System.out.println("hand shake 2");
        // 将接收到的内容转换成字符串后输出
        if(inBuff[77]==0 & inBuff[78]==1) {
            // 从字符串数组中取出一个元素作为发送数据
            SegProcessor segment = new SegProcessor();
            byte[] sendData = segment.generate(seg);
            // 以指定的字节数组作为发送数据，以刚接收到的DatagramPacket的
            // 源SocketAddress作为目标SocketAddress创建DatagramPacket
            outPacket = new DatagramPacket(sendData, sendData.length, inPacket.getSocketAddress());
            // 发送数据
            socket.send(outPacket);
            System.out.println("hand shake 2");
        }

        socket.receive(inPacket);
        //System.out.println(str_receive);
        if(inBuff[77]==1 & inBuff[78]==0){
            this.status = true;
        }
        System.out.println("receiver status: "+this.status);

    }

    public static void main(String[] args) throws IOException {
        System.out.println("Receiver Start!");
        Receiver receiver = new Receiver();
        receiver.handshake2();
        receiver.messageReceive();
    }

    public String messageReceive() throws IOException {
        System.out.println("messageReceive start!");
        String outcome = "";
        while(true) {
            socket.receive(inPacket);
            //对接受到的报文进行处理
            SegProcessor receiveSeg = new SegProcessor();
            receiveSeg.disassemble(inBuff);
            //如果FIN是1则退出循环
            if(inBuff[79]==1){
                System.out.println("END!!!!!");
                break;
            }
            //获得内容
            byte[] body = new byte[receiveSeg.getExcursion()];
            for (int i = 0; i < receiveSeg.getExcursion(); i++) {
                body[i] = inBuff[128 + i];
            }
            //String encoded = Base64.getEncoder().encodeToString(body);
            //byte[] decoded = Base64.getDecoder().decode(encoded);
            String bodyStr = new String(body);
            System.out.println(body.length);
            //获得序号
            int sequence = receiveSeg.getSequence();
            //保存内容
            String sequenceStr = String.valueOf(sequence);
            if(!map.containsKey(sequenceStr)){
                map.put(sequenceStr,bodyStr);
            }
            //生成即将发送的报文
            int[] sendSeg = {receiveSeg.getSequence(),receiveSeg.getSequence(),receiveSeg.getExcursion(),receiveSeg.getRemain(),1,receiveSeg.getSYN(),receiveSeg.getFIN(),receiveSeg.getVerify(),receiveSeg.getSelection(),receiveSeg.getFill()};
            outPacket = new DatagramPacket(receiveSeg.generate(sendSeg), receiveSeg.generate(sendSeg).length, inPacket.getSocketAddress());
            socket.send(outPacket);
            System.out.println("成功发送确认:"+receiveSeg.getSequence());
        }
        //对map按key排序
        int[] arr = new int[map.size()];
        int i=0;
        for(String key: map.keySet()){
            arr[i]=Integer.parseInt(key);
            i++;
        }
        Arrays.sort(arr);
        for(int j=0; j<arr.length;j++){
            text=text+map.get(String.valueOf(arr[j]));
        }
        File file =new File("/Users/kangnan/Documents/GitHub/STP-homework/file_receive");
        FileWriter fileWritter = new FileWriter(file);
        fileWritter.write(text);
        fileWritter.close();
        SegProcessor segProcessorFIN = new SegProcessor();
        int[] segFIN = {0, 0, 0, 0, 1, 0, 0, 0, 0, 0};
        outPacket = new DatagramPacket(segProcessorFIN.generate(segFIN),segProcessorFIN.generate(segFIN).length,inPacket.getSocketAddress());
        System.out.println("handshake 5");
        socket.send(outPacket);

        segFIN[4] = 0;
        segFIN[6] = 1;
        outPacket = new DatagramPacket(segProcessorFIN.generate(segFIN),segProcessorFIN.generate(segFIN).length,inPacket.getSocketAddress());
        System.out.println("handshake 6");
        socket.send(outPacket);

        socket.receive(inPacket);
        if(inBuff[77]==1) {
            this.status = false;
            System.out.println("receiver status: " + this.status);
        }
        return outcome;
    }

}