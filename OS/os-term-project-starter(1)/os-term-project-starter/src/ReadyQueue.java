import java.util.Comparator;
import java.util.PriorityQueue;
/**
 * คิวงานที่พร้อมถูกหยิบไปทำ
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * สิ่งที่คลาสนี้ต้องทำได้:
 *   - เก็บงานที่รอ Worker อยู่
 *   - หยิบงานถัดไปตามนโยบายที่เลือก (FCFS หรือ Priority)
 *   - ถูกเรียกจากหลาย Thread พร้อมกันได้อย่างปลอดภัย
 *
 * ข้อกำหนดจากโจทย์ที่เกี่ยวกับคลาสนี้:
 *   - หัวข้อ 4: priority = 1 สูงสุด เมื่อเท่ากันต้องมีกติกาตัดสินลำดับ (tie-break)
 *     ที่ตัดสินจากข้อมูลของ Job ไม่ขึ้นกับว่า Thread ใดเข้าถึงคิวก่อน
 *   - หัวข้อ 7: ห้ามวนลูปเช็กแบบกิน CPU (busy waiting) — Worker ที่ไม่มีงานทำ
 *     ต้องถูกพักไว้ ไม่ใช่วนถามซ้ำ ๆ
 *
 * จะออกแบบเป็นคลาสเดียวที่รับนโยบายเข้ามา หรือแยกเป็นสองคลาส
 * หรือใช้โครงสร้างข้อมูลสำเร็จรูปของ Java ก็ได้ ขอให้อธิบายเหตุผลได้ใน Demo
 */
public class ReadyQueue {

    // TODO: เก็บนโยบาย (Config.Policy) และโครงสร้างข้อมูลที่ใช้เก็บงาน
    private final PriorityQueue<Job> queue;
    private boolean closed = false;

    public ReadyQueue(Config.Policy policy) {
        // TODO
        Comparator<Job> order;
        if (policy == Config.Policy.PRIORITY) {
            order = Comparator.comparingInt((Job j) -> j.priority).thenComparingLong(j -> j.arrivalMs).thenComparingInt(j -> j.sequence);
        } else {
            order = Comparator.comparingLong((Job j) -> j.arrivalMs).thenComparingInt(j -> j.sequence);
        }
        this.queue = new PriorityQueue<>(order);
        //throw new UnsupportedOperationException("TODO: ReadyQueue constructor");
    }

    /** ใส่งานเข้าคิว เรียกโดย Scheduler Thread */
    public synchronized void add(Job job) {
        // TODO
        queue.add(job);
        notifyAll();
        //ฟthrow new UnsupportedOperationException("TODO: ReadyQueue.add");
    }

    /**
     * หยิบงานถัดไปตามนโยบาย เรียกโดย Worker Thread
     *
     * ถ้ายังไม่มีงาน ต้องรอโดยไม่กิน CPU
     * ต้องคิดด้วยว่าจะบอก Worker อย่างไรเมื่อไม่มีงานเหลือแล้วและควรหยุดทำงาน
     */
    public synchronized Job take() throws InterruptedException {
        // TODO
        while (queue.isEmpty() && !closed) {
            wait();
        }
        return queue.poll();   // คิวว่าง+ปิดแล้ว -> poll() ได้ null
        //throw new UnsupportedOperationException("TODO: ReadyQueue.take");
    }
    public synchronized void close() {
        closed = true;
        notifyAll();
    }

    /** จำนวนงานที่รออยู่ตอนนี้ ใช้โดย Monitor — ต้องอ่านได้อย่างปลอดภัย */
    public synchronized int size() {
        // TODO
        return queue.size();
        //throw new UnsupportedOperationException("TODO: ReadyQueue.size");
    }
}
