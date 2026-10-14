# Mini Job Scheduler & Shared Resource Manager

## วิธี Compile และ Run
เปิด Terminal ที่โฟลเดอร์หลักของโปรเจ็กต์ แล้วรัน:

```powershell
cd .\src
javac -encoding UTF-8 *.java
java Main ..\workloads\jobs_standard.csv priority 3 1 2
```

- ถ้าภาษาไทยในผลลัพธ์เป็น `???` ให้สั่ง `chcp 65001` ก่อนรัน
- เก็บ log ลงไฟล์: `mkdir ..\logs` แล้วรัน `java Main ... | Out-File -Encoding utf8 ..\logs\ชื่อไฟล์.log`

## ค่าที่ใส่ตอนรัน (command-line arguments)

```
java Main <workload.csv> <fcfs|priority> <workers> <printerPermits> <databasePermits>
```

| ลำดับ | Argument | ความหมาย | ค่าที่รับได้ |
|---|---|---|---|
| 1 | `workload.csv` | ไฟล์ชุดงานทดสอบ | path ของไฟล์ CSV |
| 2 | `fcfs` \| `priority` | นโยบายจัดลำดับงาน | `fcfs` หรือ `priority` |
| 3 | `workers` | จำนวน Worker Thread | จำนวนเต็ม ≥ 1 |
| 4 | `printerPermits` | จำนวนงานที่ใช้ PRINTER พร้อมกันได้ | จำนวนเต็ม ≥ 1 |
| 5 | `databasePermits` | จำนวนงานที่ใช้ DATABASE พร้อมกันได้ | จำนวนเต็ม ≥ 1 |

ต้องใส่ครบ 5 ตัวพอดี ไม่มีค่า default ถ้าผิดจะแสดง Usage แล้วจบด้วย exit code 1

## ข้อจำกัดที่ควรรู้

**การใช้งานโปรแกรม**

- path ของ workload เป็น relative จากโฟลเดอร์ที่สั่งรัน (รันจาก `src` ต้องใช้ `..\workloads\...`)
- priority เลข 1 สูงสุด ถ้าเท่ากันตัดสินด้วย `arrivalMs` แล้วตามด้วยลำดับในไฟล์
- ไม่มี preemption งานที่เริ่มทำแล้วจะไม่ถูกแย่ง
- เวลาใน log เป็นค่าประมาณ (ใช้ `Thread.sleep`) รันซ้ำแล้วต่างกันเล็กน้อยได้ แต่ลำดับ START เหมือนเดิม
- ไฟล์ workload ผิดรูปแบบ โปรแกรมแจ้งหมายเลขบรรทัดแล้วหยุดทันที

**ข้อกำหนดของโค้ด**

- ห้ามเปลี่ยนชื่อคลาสและ method ที่กำหนดไว้ แต่เพิ่ม field, method หรือ constructor parameter ได้
- ต้องมี `Scheduler` คั่นระหว่าง `JobGenerator` กับ `ReadyQueue` ห้ามให้ Generator ใส่งานลง Ready Queue โดยตรง
- Worker ต้องทำงานตามลำดับที่โจทย์กำหนด ห้ามสลับช่วงทำงานหลักกับช่วงรอ/ใช้ resource
- Ready Queue ต้องปลอดภัยเมื่อเรียกจากหลาย Thread และ Worker ที่ไม่มีงานต้องรอโดยไม่ทำ busy waiting
- การหยุดระบบต้องรอจนงานครบ ห้ามใช้ `sleep` เดาเวลา และต้อง `join` Thread ที่เริ่มไว้
- ต้องคืน Semaphore permit เสมอ แม้เกิด exception หรือถูก interrupt
- ใช้ `logger.now()` เป็นนาฬิกากลางสำหรับค่าวัดผล ห้ามนำ `System.currentTimeMillis()` มาปน
