import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
/**
 * ปล่อยงานเข้าสู่ระบบตามเวลา arrivalMs ของแต่ละ Job
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * หน้าที่ (หัวข้อ 3 ของโจทย์):
 *   - รอจนถึงเวลา arrivalMs ของแต่ละงาน แล้วส่งงานต่อไปยัง Scheduler
 *   - บันทึกเวลาที่งานเข้าสู่ระบบ "จริง" ลงใน Job
 *     (อาจไม่ตรงกับ arrivalMs เป๊ะ เพราะ Thread ถูกปลุกช้าได้)
 *   - เรียก logger.jobArrived(job) ทุกครั้งที่ปล่อยงาน
 *
 * ข้อควรคิด:
 *   - รายการงานที่ได้จาก WorkloadLoader เรียงตามลำดับในไฟล์ ไม่ได้เรียงตามเวลา
 *   - เมื่อปล่อยงานครบทุกชิ้นแล้ว ต้องมีวิธีบอกระบบว่า "จะไม่มีงานเข้ามาอีก"
 *     ดู TODO เรื่องการปิดระบบใน Main
 */
public class JobGenerator extends Thread {

    // TODO: เก็บรายการงาน, ช่องทางส่งงานไปยัง Scheduler และ logger
    //
    // หมายเหตุ: constructor ด้านล่างยังไม่มี parameter สำหรับ "ช่องทางส่งงาน"
    // เพราะเป็นสิ่งที่กลุ่มต้องออกแบบเอง (หัวข้อ 2 ห้ามให้ JobGenerator
    // ใส่งานลง ReadyQueue โดยตรง ต้องผ่าน Scheduler เสมอ)
    // ให้เพิ่ม parameter เข้าไปตามที่ออกแบบ เช่น BlockingQueue<Job>
    // หรือคลาสของกลุ่มเอง — เพิ่ม parameter ได้ แต่อย่าเปลี่ยนชื่อคลาส
    
    private final List<Job> jobs;
    private final BlockingQueue<Job> arrivalQueue;
    private final ProjectLogger logger;

    public JobGenerator(List<Job> jobs,BlockingQueue<Job> arrivalQueue, ProjectLogger logger) {
        super("generator");
        // TODO
        this.jobs = new ArrayList<>(jobs);
        this.jobs.sort(Comparator.comparingLong((Job j) -> j.arrivalMs).thenComparingInt(j -> j.sequence));
        this.arrivalQueue = arrivalQueue;
        this.logger = logger;
       //throw new UnsupportedOperationException("TODO: JobGenerator constructor");
    }

    @Override
    public void run() {
        // TODO: วนปล่อยงานตามเวลา แล้วแจ้งเมื่อปล่อยครบ
        try {
            for (Job job : jobs) {
                long wait = job.arrivalMs - logger.now();
                if (wait > 0) {
                    Thread.sleep(wait);
                }
                job.actualArrivalMs = logger.now();
                logger.jobArrived(job);
                arrivalQueue.put(job);
            }
            arrivalQueue.put(Scheduler.POISON_PILL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
