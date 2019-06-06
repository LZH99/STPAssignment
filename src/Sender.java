import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Sender {
    static final int MAX_WINDOW = 400;
    // 定义发送数据报的目的地
    public static final int DEST_PORT = 30000;
    public static final String DEST_IP = "127.0.0.1";
    // 定义每个数据报的最大大小为4KB
    private static final int DATA_LEN = 228;
    private DatagramSocket socket;
    private FilePoint filePoint;
    private String input;
    private LogProcessor log = new LogProcessor();

    private Thread senderSend = new Thread(){
        @Override
        public void run(){
            try{
                int[] seg = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                byte[] buff = new byte[DATA_LEN];
                SegProcessor segProcessor = new SegProcessor();
                while(true) {
                    Thread.sleep(50);
                    if(filePoint.getByte_in_window()[filePoint.getWindow_point()]){
                        seg[0]++;
                        continue;
                    }
                    filePoint.setWindow_point(filePoint.getWindow_point()+1);
                    outPacket = new DatagramPacket(new byte[0], 0, InetAddress.getByName(DEST_IP), DEST_PORT);
                    seg[2] = input.substring(filePoint.getCurrent_place(),Math.min(filePoint.getFile_len(),filePoint.getCurrent_place()+100)).length();
                    buff = segProcessor.generate(seg,input.substring(filePoint.getCurrent_place(),Math.min(filePoint.getFile_len(),filePoint.getCurrent_place()+100)));
                    outPacket.setData(buff);
                    socket.send(outPacket);
                    log.sendLog(buff);
                    System.out.println("snd:"+segProcessor.getSequence());
                    filePoint.setCurrent_place(filePoint.getCurrent_place()+100);
                    seg[0]++;
                    if(filePoint.getCurrent_place() >= filePoint.getFile_len()){
                        break;
                    }
                    if(filePoint.getCurrent_place() >= filePoint.getWindow_end()){
                        filePoint.setCurrent_place(filePoint.getWindow_start());
                        seg[0] = filePoint.getWindow_start()/100;
                        filePoint.setWindow_point(0);
                    }
                }
                Thread.sleep(50);
                int[] segFIN = {0, 0, 0, 0, 0, 0, 1, 0, 0, 0};
                outPacket = new DatagramPacket(new byte[0], 0, InetAddress.getByName(DEST_IP), DEST_PORT);
                buff = segProcessor.generate(segFIN);
                System.out.println("handshake 4");
                outPacket.setData(buff);
                socket.send(outPacket);
                log.sendLog(buff);

                socket.receive(inPacket);
                log.receiveLog(inPacket.getData());
                if(inBuff[77]==1){
                    socket.receive(inPacket);
                    log.receiveLog(inPacket.getData());
                    if(inBuff[79]==1){
                        segFIN[4] = 1;
                        segFIN[6] = 0;
                        outPacket = new DatagramPacket(new byte[0], 0, InetAddress.getByName(DEST_IP), DEST_PORT);
                        buff = segProcessor.generate(segFIN);
                        System.out.println("handshake 7");
                        outPacket.setData(buff);
                        socket.send(outPacket);
                        log.sendLog(buff);
                        System.out.println("Sender status:"+false);
                    }
                }



            }catch (IOException e){
                System.out.println(e);
            }catch (InterruptedException e) {
                System.out.println("Thread interrupted.");
            }
        }
    };

    private Thread senderReceive = new Thread(){
      @Override
      public void run(){
          boolean end_or_not = false;
          boolean[] byte_in_window;
          SegProcessor segProcessor = new SegProcessor();
          try{
              while(true) {
                  socket.receive(inPacket);
                  log.receiveLog(inPacket.getData());
                  SegProcessor seg = new SegProcessor();
                  seg.disassemble(inBuff);
                  System.out.println("seg receive:"+seg.getSequence());
                  if(seg.getSequence()*100+100>=filePoint.getFile_len()){break;}
                  if (seg.getACK() == 1) {
                      System.out.println("try to move window!!!");
                      byte_in_window = filePoint.getByte_in_window();
                      segProcessor.disassemble(inBuff);
                      byte_in_window[(segProcessor.getSequence() * 100 - filePoint.getWindow_start()) / 100] = true;
                      for (int i = 0; i < 4; i++) {
                          if (!byte_in_window[0]) {
                              break;
                          } else {
                              byte_in_window[0] = byte_in_window[1];
                              byte_in_window[1] = byte_in_window[2];
                              byte_in_window[2] = byte_in_window[3];
                              byte_in_window[3] = false;
                              filePoint.setWindow_start(Math.min(filePoint.getWindow_end(),filePoint.getWindow_start()+100));
                              filePoint.setWindow_end(Math.min(filePoint.getFile_len(),filePoint.getWindow_end()+100));
                              filePoint.setWindow_point(Math.max(filePoint.getWindow_point()-1,0));
                          }
                      }
                      filePoint.setByte_in_window(byte_in_window);
                  }
              }
          }catch (IOException e){
              System.out.println(e);
          }
      }
    };

    //定义每个报文段的字节长
    private static final int BYTE_LEN = 100;
    // 定义接收网络数据的字节数组
    byte[] inBuff = new byte[DATA_LEN];
    // 以指定的字节数组创建准备接收数据的DatagramPacket对象
    private DatagramPacket inPacket = new DatagramPacket(inBuff , inBuff.length);
    // 定义一个用于发送的DatagramPacket对象
    private DatagramPacket outPacket = null;

    private boolean status = false;

    public void handshake1()throws IOException{
        int[] seg = {0, 0, 0, 0, 0, 1, 0, 0, 0, 0};

            outPacket = new DatagramPacket(new byte[0] , 0 , InetAddress.getByName(DEST_IP) , DEST_PORT);
            SegProcessor segment = new SegProcessor();
            byte[] buff = segment.generate(seg);
            outPacket.setData(buff);
            socket.send(outPacket);
            log.sendLog(buff);
            System.out.println("handshake1");
            socket.receive(inPacket);
            log.receiveLog(inPacket.getData());
            if(inBuff[77]==1 & inBuff[78]==1) {

                this.handshake3();
                this.status = true;
            }
            System.out.println("sender status: " + this.status);

    }

    public void handshake3()throws IOException{
        int[] seg = {0, 0, 0, 0, 1, 0, 0, 0, 0, 0};

            outPacket = new DatagramPacket(new byte[0] , 0 , InetAddress.getByName(DEST_IP) , DEST_PORT);
            SegProcessor segment = new SegProcessor();
            byte[] buff = segment.generate(seg);
            outPacket.setData(buff);
            socket.send(outPacket);
            log.sendLog(buff);
            System.out.println("handshake3");

    }

    public static void main(String[] args) throws IOException {
        System.out.println("Sender Start!");
            Sender sender = new Sender();
            sender.init();
            sender.handshake1();
            sender.segStart();
    }

    public String fileRead(){
        String file_path = "/Users/kangnan/Documents/GitHub/STP-homework/file_send";
        String outcome = "";
        try {
            FileReader fr = new FileReader(file_path);
            BufferedReader br = new BufferedReader(fr);
            String str2 = br.readLine();
            while (str2 != null) {
                outcome += str2 + "\n";
                str2 = br.readLine();
            }
            br.close();
            fr.close();
        }catch (IOException e){
            System.out.println("无法读取文件："+e.getMessage());
        }
        return outcome;
    }

    public void init(){
        try{// 创建DatagramSocket对象
            this.socket = new DatagramSocket();
        }catch (SocketException e){
            System.out.println(e);
        }
        this.input = fileRead();
        this.filePoint = new FilePoint(this.input.length());
    }

    public void segStart(){
        this.senderSend.start();
        this.senderReceive.start();
    }
}

