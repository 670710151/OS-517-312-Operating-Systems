import java.io.*;
import java.nio.charset.StandardCharsets;

public class Protocol {
    public static final int PORT = 5000;
    public static final String FILE_DIR = "files";

    // อ่านทีละ byte จนเจอ '\n' แล้วแปลงเป็นข้อความ
    // คืน null ถ้าอีกฝั่งปิด connection แล้ว
    public static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                return buf.toString(StandardCharsets.UTF_8);
            }
            buf.write(b);
        }
        return buf.size() == 0 ? null : buf.toString(StandardCharsets.UTF_8);
    }

    // ส่งข้อความ 1 บรรทัด (ต่อท้ายด้วย '\n') แล้ว flush
    public static void writeLine(OutputStream out, String line) throws IOException {
        out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}