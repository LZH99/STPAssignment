public class FilePoint {
    private final int MAX_WINDOW = 400;
    private int file_len;
    private int window_start;
    private int window_end;
    private boolean[] byte_in_window = {false, false, false, false};
    private int current_place;
    private int window_point;

    public FilePoint(int file_len){
        this.window_start = 0;
        this.window_end = MAX_WINDOW;
        this.file_len = file_len;
        this.current_place = 0;
        this.window_point = 0;
    }

    public int getWindow_start() {
        return this.window_start;
    }

    public int getWindow_end(){
        return this.window_end;
    }

    public boolean[] getByte_in_window(){
        return this.byte_in_window;
    }

    public int getCurrent_place(){
        return this.current_place;
    }

    public int getFile_len(){
        return this.file_len;
    }

    public int getWindow_point() {return this.window_point;}

    public void setWindow_start(int start){
        this.window_start = start;
        System.out.println("window start:"+start);
    }

    public void setWindow_end(int end){
        this.window_end = end;
        System.out.println("window end:"+end);
    }

    public void setCurrent_place(int place){
        this.current_place = place;
    }

    public void setByte_in_window(boolean[] byte_in_window){
        this.byte_in_window = byte_in_window;
    }

    public void setWindow_point(int window_point){this.window_point = window_point;}
}
