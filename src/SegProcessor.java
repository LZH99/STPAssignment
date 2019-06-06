import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SegProcessor {
    private byte[] head = new byte[128];//报文头部
    private byte[] sequence = new byte[32];//序号
    private byte[] ensure = new byte[32];//确认号
    private byte[] excursion = new byte[8];//数据偏移
    private byte[] remain = new byte[5];//保留
    private byte[] ACK = new byte[1];
    private byte[] SYN = new byte[1];
    private byte[] FIN = new byte[1];
    private byte[] verify = new byte[16];//校验和
    private byte[] selection = new byte[16];//选项
    private byte[] fill = new byte[16];//填充

    public byte[] generate(int[] seg_in){
        int i;
        int[] seg = new int[seg_in.length];
        for (i=0;i<seg_in.length;i++){
            seg[i] = seg_in[i];
        }
        for(i = 31;i>=0;i--){
            if(seg[0]%2==1){
                sequence[i] = 1;
            }else{
                sequence[i] = 0;
            }

            head[i] = sequence[i];
            seg[0] = seg[0]/2;
        }

        for(i = 31;i>=0;i--){
            if(seg[1]%2==1){
                ensure[i] = 1;
            }else{
                ensure[i] = 0;
            }

            head[i+32] = ensure[i];
            seg[1] = seg[1]/2;
        }

        for(i = 7;i>=0;i--){
            if(seg[2]%2==1){
                excursion[i] = 1;
            }else{
                excursion[i] = 0;
            }

            head[i+64] = excursion[i];
            seg[2] = seg[2]/2;
        }

        for(i = 4;i>=0;i--){
            if(seg[3]%2==1){
                remain[i] = 1;
            }else{
                remain[i] = 0;
            }

            head[i+72] = remain[i];
            seg[3] = seg[3]/2;
        }

        if(seg[4] == 1){
            ACK[0] = 1;
        }else{
            ACK[0] = 0;
        }
        head[77] = ACK[0];

        if(seg[5] == 1){
            SYN[0] = 1;
        }else{
            SYN[0] = 0;
        }
        head[78] = SYN[0];

        if(seg[6] == 1){
            FIN[0] = 1;
        }else{
            FIN[0] = 0;
        }
        head[79] = FIN[0];

        for(i = 15;i>=0;i--){
            if(seg[7]%2==1){
                verify[i] = 1;
            }else{
                verify[i] = 0;
            }

            head[i+80] = verify[i];
            seg[7] = seg[7]/2;
        }

        for(i = 15;i>=0;i--){
            if(seg[8]%2==1){
                selection[i] = 1;
            }else{
                selection[i] = 0;
            }

            head[i+96] = selection[i];
            seg[8] = seg[8]/2;
        }

        for(i = 15;i>=0;i--){
            if(seg[9]%2==1){
                fill[i] = 1;
            }else{
                fill[i] = 0;
            }

            head[i+112] = fill[i];
            seg[9] = seg[9]/2;
        }
        return head;
    }

    public byte[] generate(int[] seg, String message){
        byte[] outcome = new byte[4096];
        byte[] seg2 = generate(seg);
        for(int i = 0; i<128;i++){
            outcome[i] = seg2[i];
        }
        seg2 = message.getBytes();

        for(int i = 128; i<128+seg2.length;i++){
            outcome[i] = seg2[i-128];
        }
        return outcome;
    }

    public void disassemble(byte[] seg_byte) {
        int i = 0;
        for(i = 0;i<128;i++){
            this.head[i] = seg_byte[i];
        }

        for(i = 0;i<32;i++){
            this.sequence[i] = seg_byte[i];
        }

        for(i = 0;i<32;i++){
            this.ensure[i] = seg_byte[i+32];
        }

        for(i = 0;i<8;i++){
            this.excursion[i] = seg_byte[i+64];
        }

        for(i = 0;i<5;i++){
            this.remain[i] = seg_byte[i+72];
        }

        this.ACK[0] = seg_byte[77];
        this.SYN[0] = seg_byte[78];
        this.FIN[0] = seg_byte[79];

        for(i = 0;i<16;i++){
            this.verify[i] = seg_byte[i+80];
        }

        for(i = 0;i<16;i++){
            this.selection[i] = seg_byte[i+96];
        }

        for(i = 0;i<16;i++){
            this.fill[i] = seg_byte[i+112];
        }
    }

    public int getSequence(){
        int outcome = 0;
        for(int i = 0;i<32;i++){
            outcome = outcome*2+this.sequence[i];
        }
        return outcome;
    }

    public int getEnsure(){
        int outcome = 0;
        for(int i = 0;i<32;i++){
            outcome = outcome*2+this.ensure[i];
        }
        return outcome;
    }

    public int getExcursion(){
        int outcome = 0;
        for(int i = 0;i<8;i++){
            outcome = outcome*2+this.excursion[i];
        }
        return outcome;
    }

    public int getRemain(){
        int outcome = 0;
        for(int i = 0;i<5;i++){
            outcome = outcome*2+this.excursion[i];
        }
        return outcome;
    }

    public int getACK(){
        return this.ACK[0];
    }

    public int getSYN(){
        return this.SYN[0];
    }

    public int getFIN(){
        return this.FIN[0];
    }

    public int getVerify(){
        int outcome = 0;
        for(int i = 0;i<16;i++){
            outcome = outcome*2+this.verify[i];
        }
        return outcome;
    }

    public int getSelection(){
        int outcome = 0;
        for(int i = 0;i<16;i++){
            outcome = outcome*2+this.selection[i];
        }
        return outcome;
    }

    public int getFill(){
        int outcome = 0;
        for(int i = 0;i<16;i++){
            outcome = outcome*2+this.fill[i];
        }
        return outcome;
    }
}
