import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * รวบรวมและคำนวณค่าที่ใช้วัดผลของการรันหนึ่งครั้ง
 *
 * ===== ไฟล์นี้เป็นโครงเปล่า นักศึกษาต้องเขียนเอง =====
 *
 * ข้อกำหนดจากโจทย์ที่เกี่ยวกับคลาสนี้ (หัวข้อ 8):
 * - Waiting Time, Turnaround Time, Throughput, Resource Wait Time
 * - ต้องถูกอัปเดตจากหลาย Worker พร้อมกันได้อย่างปลอดภัย
 * - ผลต้องสอดคล้องกับสมการตรวจสอบ:
 * Turnaround = Waiting + workMs + Resource Wait + resourceMs
 * ใช้สมการนี้ตรวจงานทีละชิ้นได้ว่าค่าไหนคำนวณผิด
 *
 * ข้อควรระวัง: ค่าเฉลี่ยของ Resource Wait ให้คิดเฉพาะงานที่ใช้ resource
 * ส่วนงานที่ resource = NONE ให้ถือว่า Resource Wait เป็น 0
 */
public class Statistics {

    // TODO: เก็บข้อมูลของงานที่เสร็จแล้ว หรือเก็บผลรวมไว้คำนวณทีหลัง
    private final AtomicInteger completed = new AtomicInteger();
    private final AtomicInteger running = new AtomicInteger();

    public void runningStarted() {
        running.incrementAndGet();
    }

    public void runningEnded() {
        running.decrementAndGet();
    }

    public int runningCount() {
        return running.get();
    }

    /** บันทึกว่างานชิ้นหนึ่งเสร็จแล้ว เรียกโดย Worker หลายตัวพร้อมกันได้ */
    public void recordCompletion(Job job) {
        // TODO
        completed.incrementAndGet();
        // throw new UnsupportedOperationException("TODO: Statistics.recordCompletion");
    }

    /** จำนวนงานที่เสร็จแล้ว ใช้โดย Monitor และใช้ตรวจว่างานครบหรือยัง */
    public int completedCount() {
        // TODO
        return completed.get();
        // throw new UnsupportedOperationException("TODO: Statistics.completedCount");
    }

    /**
     * พิมพ์ตารางสรุปผลตอนจบโปรแกรม
     * อย่างน้อยต้องมี avg Waiting Time, avg Turnaround Time,
     * Throughput และ avg Resource Wait Time
     *
     * ตามหัวข้อ 14 ให้รายงานเวลาเป็นจำนวนเต็มหน่วย ms
     * และ Throughput อย่างน้อย 2 ตำแหน่งทศนิยม
     */
    public void printSummary(List<Job> allJobs, long makespanMs) {
        // TODO
        long sumWt = 0, sumTat = 0, sumRw = 0;
        int n = 0, rwCount = 0;

        System.out.println();
        System.out.println("---- ผลต่อ Job (ms) ----");
        System.out.printf("%-6s %7s %7s %7s %7s%n", "job", "WT", "TAT", "RW", "diff");

        for (Job job : allJobs) {
            if (job.completionTimeMs < 0) {
                System.out.printf("%-6s ยังไม่เสร็จ%n", job.id);
                continue;
            }
            long wt = job.startTimeMs - job.actualArrivalMs;
            long tat = job.completionTimeMs - job.actualArrivalMs;
            long rw = job.resourceWaitMs;
            long expected = wt + job.workMs + rw + job.resourceMs;
            System.out.printf("%-6s %7d %7d %7d %7d%n", job.id, wt, tat, rw, tat - expected);

            sumWt += wt;
            sumTat += tat;
            n++;
            if (job.resource != ResourceType.NONE) {
                sumRw += rw;
                rwCount++;
            }
        }

        System.out.println();
        System.out.println("---- สรุป ----");
        System.out.printf("เสร็จ %d / %d งาน%n", n, allJobs.size());
        if (n > 0) {
            System.out.printf("avg WT   = %d ms%n", Math.round((double) sumWt / n));
            System.out.printf("avg TAT  = %d ms%n", Math.round((double) sumTat / n));
        }
        System.out.printf("avg RW   = %d ms (เฉพาะงานที่ใช้ resource %d งาน)%n",
                rwCount == 0 ? 0 : Math.round((double) sumRw / rwCount), rwCount);
        System.out.printf(Locale.US, "Throughput = %.2f jobs/s (เวลารวม %d ms)%n",
                n / (makespanMs / 1000.0), makespanMs);
        // throw new UnsupportedOperationException("TODO: Statistics.printSummary");
    }
}
