import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.IOException;
public class LogProcessor {
    public void receiveLog(byte[] buff) throws IOException {
        SegProcessor seg = new SegProcessor();
        seg.disassemble(buff);
        String sequence = String.valueOf(seg.getSequence());
        String ensure = String.valueOf(seg.getEnsure());
        String type = "";
        int l =0;
        if(buff[128]!=0){
            type+="D";
            for(int i=buff.length-1; i>=128;i--){
                if(buff[i]==0){
                    l++;
                }else{
                    break;
                }
            }
        }
        if(buff[79]==1){
            type+="F";
        }
        if(buff[78]==1){
            type+="S";
        }
        if(buff[77]==1){
            type+="A";
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        String receiveLog = "rcv "+df.format(new Date())+" "+type+" "+sequence+" 0 "+ensure+" \n";
        File file =new File("/Users/kangnan/Documents/GitHub/STP-homework/file-log");
        FileWriter fileWritter = new FileWriter(file,true);
        fileWritter.write(receiveLog);
        fileWritter.close();
    }
    public void sendLog(byte[] buff) throws IOException {
        SegProcessor seg = new SegProcessor();
        seg.disassemble(buff);
        String sequence = String.valueOf(seg.getSequence());
        String ensure = String.valueOf(seg.getEnsure());
        String type = "";
        int l= 0;
        if(buff.length>128){
            for(int i=buff.length-1; i>=128;i--){
                if(buff[i]==0){
                    l++;
                }else{
                    break;
                }
            }
            if(l+128!=buff.length){
                type+="D";
            }
        }
        if(buff[79]==1){
            type+="F";
        }
        if(buff[78]==1){
            type+="S";
        }
        if(buff[77]==1){
            type+="A";
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        String receiveLog = "snd "+df.format(new Date())+" "+type+" "+sequence+" "+String.valueOf(buff.length-128-l)+" "+ensure+" \n";
        File file =new File("/Users/kangnan/Documents/GitHub/STP-homework/file-log");
        FileWriter fileWritter = new FileWriter(file,true);
        fileWritter.write(receiveLog);
        fileWritter.close();
    }

    public void compute(){

    }

    public void clear() throws IOException {
        File file =new File("/Users/kangnan/Documents/GitHub/STP-homework/file-log");
        FileWriter fileWritter = new FileWriter(file);
        fileWritter.write("");
        fileWritter.close();
    }
}
