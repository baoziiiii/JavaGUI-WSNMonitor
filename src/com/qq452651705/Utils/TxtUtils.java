package com.qq452651705.Utils;

import java.io.*;

public class TxtUtils {
    private String path;

    public TxtUtils(String path) {
        this.path = path;
    }

    public void appendToTxt(String s) throws IOException {
        FileWriter fw = null;
//如果文件存在，则追加内容；如果文件不存在，则创建文件
        File f = new File(path);
        fw = new FileWriter(f, true);
        PrintWriter pw = new PrintWriter(fw);
        pw.print(s);
        pw.flush();
        fw.flush();
        pw.close();
        fw.close();
    }

    public void writeToTxt(String s) throws FileNotFoundException {
        FileOutputStream output;
        output = new FileOutputStream(path);
        try {
            output.write(s.getBytes());
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
