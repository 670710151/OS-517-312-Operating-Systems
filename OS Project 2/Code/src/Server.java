import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.nio.channels.*;
import java.nio.file.StandardOpenOption;

public class Server {

    static boolean nio = false;

    public static void main(String[] args) throws IOException {
        nio = args.length >= 1 && args[0].equals("nio");
        File dir = new File(Protocol.FILE_DIR);

        try (ServerSocketChannel server = ServerSocketChannel.open();
                ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {

            server.bind(new InetSocketAddress(Protocol.PORT));
            System.out.println("Server พร้อม โหมด = " + (nio ? "NIO transferTo" : "Traditional I/O"));

            while (true) {
                SocketChannel ch = server.accept();
                pool.submit(() -> handleClient(ch, dir));
            }
        }
    }

    static void handleClient(SocketChannel ch, File dir) {
        try (ch) {
            InputStream in = ch.socket().getInputStream();
            OutputStream out = ch.socket().getOutputStream();

            String line;
            while ((line = Protocol.readLine(in)) != null) {
                String[] p = line.trim().split(" ");
                switch (p[0]) {
                    case "LIST" -> handleList(out, dir);
                    case "INFO" -> handleInfo(out, dir, p);
                    case "GET" -> handleGet(ch, out, dir, p);
                    default -> Protocol.writeLine(out, "ERROR 400 Unknown command");
                }
            }
        } catch (IOException e) {
            System.out.println("Client หลุด: " + e.getMessage());
        }
    }

    // LIST -> FILE <name> <size> ... END
    static void handleList(OutputStream out, File dir) throws IOException {
        File[] files = dir.listFiles(File::isFile);
        if (files != null) {
            for (File f : files) {
                Protocol.writeLine(out, "FILE " + f.getName() + " " + f.length());
            }
        }
        Protocol.writeLine(out, "END");
    }

    // INFO <filename> -> SIZE <bytes> หรือ ERROR
    static void handleInfo(OutputStream out, File dir, String[] p) throws IOException {
        File file = findFile(dir, p);
        if (file == null) {
            Protocol.writeLine(out, "ERROR 404 File not found");
            return;
        }
        Protocol.writeLine(out, "SIZE " + file.length());
    }

    // GET <filename> <offset> <length> -> OK <length> + byte ดิบ
    static void handleGet(SocketChannel ch, OutputStream out, File dir, String[] p) throws IOException {
        File file = findFile(dir, p);
        if (file == null || p.length != 4) {
            Protocol.writeLine(out, "ERROR 404 File not found or bad request");
            return;
        }

        long offset, length;
        try {
            offset = Long.parseLong(p[2]);
            length = Long.parseLong(p[3]);
        } catch (NumberFormatException e) {
            Protocol.writeLine(out, "ERROR 400 Offset/length must be numbers");
            return;
        }

        if (offset < 0 || length < 0 || offset + length > file.length()) {
            Protocol.writeLine(out, "ERROR 416 Range out of bounds");
            return;
        }
        
        Protocol.writeLine(out, "OK " + length);
        if (nio)
            sendNio(ch, file, offset, length);
        else
            sendTraditional(out, file, offset, length);
    }

    static void sendTraditional(OutputStream out, File file, long offset, long length) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(offset);
            byte[] buf = new byte[64 * 1024];
            long remaining = length;
            while (remaining > 0) {
                int n = raf.read(buf, 0, (int) Math.min(buf.length, remaining));
                if (n == -1)
                    break;
                out.write(buf, 0, n);
                remaining -= n;
            }
            out.flush();
        }
    }

    static void sendNio(SocketChannel ch, File file, long offset, long length) throws IOException {
        try (FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            long pos = offset;
            long remaining = length;
            while (remaining > 0) {
                long n = fc.transferTo(pos, remaining, ch); // ให้ OS ส่งตรงจากไฟล์ไป socket
                pos += n;
                remaining -= n;
            }
        }
    }
    // หาไฟล์จากชื่อ พร้อมกันการแอบเข้าโฟลเดอร์อื่น (เช่น ../../secret)
    // หาไฟล์จากชื่อ โดยชื่อต้องตรงกับไฟล์จริงทุกตัวอักษร (แยกตัวพิมพ์เล็ก/ใหญ่)
    static File findFile(File dir, String[] p) {
        if (p.length < 2) return null;
        String name = p[1];

        if (name.contains("/") || name.contains("\\") || name.contains(".."))
            return null;

        File[] files = dir.listFiles(File::isFile);   // ไฟล์ทั้งหมดในโฟลเดอร์ files/
        if (files == null) return null;

        for (File f : files) {
            if (f.getName().equals(name)) {           // equals = ตรงทุกตัวอักษร
                return f;
            }
        }
        return null;
    }
}