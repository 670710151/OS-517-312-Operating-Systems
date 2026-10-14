public class TestInterrupt {

    public static void main(String[] args) throws Exception {
        System.out.println("=== เคส 1: interrupt ตอน 'รอ' PRINTER ===");
        test(true);
        System.out.println();
        System.out.println("=== เคส 2: interrupt ตอน 'ถือ' PRINTER (กำลัง sleep) ===");
        test(false);
    }

    static void test(boolean interruptWhileWaiting) throws Exception {
        ProjectLogger logger = new ProjectLogger();
        ResourceManager resources = new ResourceManager(1, 2);
        ReadyQueue queue = new ReadyQueue(Config.Policy.FCFS);
        Statistics stats = new Statistics();

        queue.add(new Job("T01", 0, 1, 100, ResourceType.PRINTER, 5000, 0));
        queue.close();

        if (interruptWhileWaiting) {
            resources.acquire(ResourceType.PRINTER);   // ให้ main ถือไว้เอง ทำให้ Worker ต้องรอ
        }

        Worker w = new Worker("worker-test", queue, resources, stats, logger);
        w.start();

        // รอจน Worker เข้าสถานะที่ต้องการ แล้วค่อย interrupt
        Thread.State target = interruptWhileWaiting ? Thread.State.WAITING
                                                    : Thread.State.TIMED_WAITING;
        Thread.sleep(300);  // ให้พ้นช่วง work 100 ms ก่อน
        while (w.getState() != target) {
            Thread.sleep(10);
        }
        System.out.println("สถานะ Worker ก่อน interrupt = " + w.getState());
        w.interrupt();

        w.join(2000);
        System.out.println("Worker จบแล้วหรือไม่ = " + !w.isAlive());

        if (interruptWhileWaiting) {
            System.out.println("permit ที่เหลือ (main ยังถืออยู่) = "
                    + resources.status() + "  (คาดหวัง printer=1/1)");
            resources.release(ResourceType.PRINTER);
        }
        System.out.println("หลังคืนทั้งหมด = " + resources.status()
                + "  (คาดหวัง printer=0/1 database=0/2)");
    }
}