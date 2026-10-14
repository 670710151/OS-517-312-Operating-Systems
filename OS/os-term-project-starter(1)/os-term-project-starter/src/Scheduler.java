import java.util.concurrent.BlockingQueue;
/* รับงานจาก JobGenerator แล้วจัดเข้า Ready Queue
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * ข้อกำหนดจากโจทย์ (หัวข้อ 2 และ 4):
 *   - Scheduler เป็น Thread บังคับ ห้ามให้ JobGenerator ใส่งานลง Ready Queue โดยตรง
 *   - รับผิดชอบการจัดลำดับตามนโยบาย FCFS หรือ Priority
 *
 * ข้อควรคิด:
 *   - Scheduler รับงานจาก JobGenerator ผ่านอะไร และรอโดยไม่กิน CPU อย่างไร
 *   - เมื่อ JobGenerator ปล่อยงานครบแล้ว Scheduler รู้ได้อย่างไรว่าควรหยุด
*/

public class Scheduler extends Thread {

    // TODO: เก็บช่องทางรับงานจาก JobGenerator, ReadyQueue ปลายทาง และ logger
    //
    // หมายเหตุ: constructor ด้านล่างยังไม่มี parameter สำหรับ "ช่องทางรับงาน"
    // ให้เพิ่มเข้าไปให้ตรงกับที่ออกแบบไว้ใน JobGenerator
    // เพิ่ม parameter ได้ แต่อย่าเปลี่ยนชื่อคลาส
    /* งานปลอมที่ใช้เป็นสัญญาณ "ส่งงานครบแล้ว" */
    public static final Job POISON_PILL = new Job("POISON_PILL", 0, 0, 0, ResourceType.NONE, 0, -1);

    private final BlockingQueue<Job> arrivalQueue;
    private final ReadyQueue readyQueue;
    private final ProjectLogger logger;

    public Scheduler(BlockingQueue<Job> arrivalQueue,ReadyQueue readyQueue, ProjectLogger logger) {
        super("scheduler");
        this.arrivalQueue = arrivalQueue;
        this.readyQueue = readyQueue;
        this.logger = logger;
        //throw new UnsupportedOperationException("TODO: Scheduler constructor");
    }

    @Override
    public void run() {
        // TODO: วนรับงานเข้ามาแล้วใส่ ReadyQueue จนกว่าจะได้รับสัญญาณให้หยุด
        try {
            while (true) {
                Job job = arrivalQueue.take();
                if (job == POISON_PILL) {
                    break;
                }
                readyQueue.add(job);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            readyQueue.close();
        }
    }
}
