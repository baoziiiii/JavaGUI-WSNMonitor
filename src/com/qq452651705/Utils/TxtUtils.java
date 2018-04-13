package com.qq452651705.Utils;

import java.io.*;

/**
 * The type Txt utils. 文本输出类
 */
public class TxtUtils {

    /**
     * @param path the path  输出路径
     */
    private String path;

    /**
     * Instantiates a new Txt utils.
     *
     * @param path the path
     */
    public TxtUtils(String path) {
        setPath(path);
    }

    public void setPath(String path) {
        this.path = path;
        File file = new File(path);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
    }

    /**
     * Append to txt.  追加模式输出
     *
     * @param text 输出文本
     * @throws IOException the io exception   文件IO异常
     */
    public void appendToTxt(String text) throws IOException {
        FileWriter fw;
        //如果文件存在，则追加内容；如果文件不存在，则创建文件
        File f = new File(path);
        fw = new FileWriter(f, true);
        PrintWriter pw = new PrintWriter(fw);
        pw.print(text);
        pw.flush();
        fw.flush();
        pw.close();
        fw.close();
    }

    /**
     * Write to txt.   普通模式输出
     *
     * @param text 输出文本
     * @throws FileNotFoundException the file not found exception 输出路径不存在异常
     */
    public void writeToTxt(String text) throws FileNotFoundException {

        FileOutputStream output;
        output = new FileOutputStream(path);
        try {
            output.write(text.getBytes());
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String read() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
        StringBuffer sb = new StringBuffer();
        String temp;
        while ((temp = bufferedReader.readLine()) != null) {
            sb.append(temp + "\r\n");
        }
        return sb.toString();
    }
}
