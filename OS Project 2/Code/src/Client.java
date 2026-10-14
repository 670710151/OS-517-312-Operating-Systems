import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.nio.channels.*;
import java.nio.file.StandardOpenOption;

public class Client {
    static final String HOST = "localhost";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("วิธีใช้: java Client <filename> [workers=10]");
            return;
        }
        // command line
        String filename = args[0];
        int workers = (args.length >= 2) ? Integer.parseInt(args[1]) : 10;
        boolean nio = args.length >= 3 && args[2].equals("nio");
        System.out.println("โหมด: " + (nio ? "NIO" : "Traditional I/O"));

        // 1) ถามขนาดไฟล์จาก Server
        long size = getFileSize(filename);
        System.out.println("ไฟล์ " + filename + " ขนาด " + size + " bytes, workers = " + workers);

        // 2) เตรียมไฟล์ปลายทาง: สร้างไฟล์ว่างขนาดเท่าของจริง
        new File("downloads").mkdirs();
        File dest = new File("downloads", filename);

        // เคลียร์ไฟล์เก่าจากรอบที่แล้วก่อน เพื่อเริ่มจากศูนย์ทุกรอบ
        if (dest.exists() && !dest.delete()) {
            throw new IOException("ลบไฟล์เก่าไม่สำเร็จ: " + dest + " (อาจถูกโปรแกรมอื่นเปิดอยู่)");
        }

        try (RandomAccessFile raf = new RandomAccessFile(dest, "rw")) {
            raf.setLength(size);
        }

        // 3) แบ่งช่วง + เริ่ม workers (Virtual Thread)
        long start = System.nanoTime();
        long chunk = size / workers;
        List<Future<?>> tasks = new ArrayList<>();

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < workers; i++) {
                long offset = i * chunk;
                long length = (i == workers - 1) ? size - offset : chunk; // ตัวสุดท้ายรับส่วนที่เหลือ
                int id = i + 1;
                tasks.add(pool.submit(() -> {
                    downloadPart(id, filename, dest, offset, length, nio);
                    return null;
                }));
            }
            for (Future<?> f : tasks) {
                f.get(); // รอให้เสร็จ ถ้า worker ไหนพังจะโยน exception ออกมาที่นี่
            }
        }
        double seconds = (System.nanoTime() - start) / 1e9;

        // 4) สรุปผล + ตรวจ size/hash
        double mb = size / 1024.0 / 1024.0;
        System.out.printf("เสร็จใน %.3f วินาที, throughput %.2f MB/s%n", seconds, mb / seconds);
        System.out.println("ขนาดไฟล์ที่ได้: " + dest.length() + (dest.length() == size ? " (ตรง)" : " (ไม่ตรง!)"));
        System.out.println("Hash หลังส่งไฟล์");
        System.out.println("SHA-256: " + sha256(dest));
    }

    // ขั้นเตรียมการก่อนดาวน์โหลด: ส่ง LIST แล้วส่ง INFO ใน connection เดียว ->
    // คืนขนาดไฟล์
    static long getFileSize(String filename) throws IOException {
        try (Socket socket = new Socket(HOST, Protocol.PORT)) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            // 1) LIST: อ่านรายการให้หมดจนเจอ END (ไม่แสดงผล)
            Protocol.writeLine(out, "LIST");
            System.out.println("ไฟล์ที่ดาวน์โหลดได้บน Server:");
            String line;
            while ((line = Protocol.readLine(in)) != null && !line.equals("END")) {
                if (!line.startsWith("FILE ")) {
                    throw new IOException("Server ตอบ: " + line);
                }
                int lastSpace = line.lastIndexOf(' ');
                String name = line.substring(5, lastSpace);
                long fileSize = Long.parseLong(line.substring(lastSpace + 1));
                System.out.printf("  %-30s %,15d bytes (%.2f MB)%n", name, fileSize, fileSize / 1024.0 / 1024.0);
            }
            System.out.println();

            // 2) INFO: ขอขนาดไฟล์
            Protocol.writeLine(out, "INFO " + filename);
            String reply = Protocol.readLine(in);
            if (reply == null || !reply.startsWith("SIZE ")) {
                throw new IOException("Server ตอบ: " + reply);
            }
            return Long.parseLong(reply.substring(5));
        }
    }

    // งานของ worker 1 ตัว: connection ของตัวเอง + ช่วง offset/length ของตัวเอง
    static void downloadPart(int id, String filename, File dest, long offset, long length, boolean nio)
            throws IOException {
        try (SocketChannel ch = SocketChannel.open(new InetSocketAddress(HOST, Protocol.PORT))) {
            InputStream in = ch.socket().getInputStream();
            Protocol.writeLine(ch.socket().getOutputStream(), "GET " + filename + " " + offset + " " + length);

            String header = Protocol.readLine(in); // อ่านทีละ byte ไม่กลืน payload
            if (header == null || !header.startsWith("OK ")) {
                throw new IOException("Worker " + id + " ได้รับ: " + header);
            }

            if (nio)
                receiveNio(ch, dest, offset, length);
            else
                receiveTraditional(in, dest, offset, length);

            System.out.println("Worker " + id + " เสร็จ (offset=" + offset + ", length=" + length + ")");
        }
    }

    static void receiveTraditional(InputStream in, File dest, long offset, long length) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(dest, "rw")) {
            raf.seek(offset);
            byte[] buf = new byte[64 * 1024];
            long remaining = length;
            while (remaining > 0) {
                int n = in.read(buf, 0, (int) Math.min(buf.length, remaining));
                if (n == -1)
                    throw new IOException("connection ปิดก่อนข้อมูลครบ");
                raf.write(buf, 0, n);
                remaining -= n;
            }
        }
    }

    static void receiveNio(SocketChannel ch, File dest, long offset, long length) throws IOException {
        try (FileChannel fc = FileChannel.open(dest.toPath(), StandardOpenOption.WRITE)) {
            long pos = offset;
            long remaining = length;
            while (remaining > 0) {
                long n = fc.transferFrom(ch, pos, remaining); // ดึงจาก socket เขียนลงไฟล์ตำแหน่ง pos
                if (n <= 0)
                    throw new IOException("connection ปิดก่อนข้อมูลครบ");
                pos += n;
                remaining -= n;
            }
        }
    }

    static String sha256(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) != -1)
                md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest())
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}