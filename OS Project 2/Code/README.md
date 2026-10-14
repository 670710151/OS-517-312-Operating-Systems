// ทำไมใช้ Virtual Thread?

Thread ปกติเป็น thread ของ OS ราคาแพง สร้างได้ไม่กี่พันตัว ส่วน Virtual Thread (Java 21) เป็น thread เบามาก JVM จัดการเอง เวลามันติดรอ I/O (เช่นรอข้อมูลจาก socket) JVM จะพักมันแล้วเอา thread จริงไปทำงานอื่นให้ เหมาะกับ Server ที่มี connection เยอะ ๆ และเราเขียนโค้ดแบบ blocking ธรรมดาได้เลย ไม่ต้องใช้ callback ซับซ้อน ในโจทย์นี้ Client เปิด 10 connection ต่อการดาวน์โหลด 1 ครั้ง Server จึงสร้าง Virtual Thread 10 ตัวพร้อมกัน

//scope
ทั้งหมดวิ่งผ่าน loopback บนเครื่องเดียว ใช้ดิสก์/Page Cache ก้อนเดียวกัน และ CPU ต้องแบ่งกันทำทั้งฝั่ง Server และ Client

Traditional I/O (read เข้า byte\[] แล้ว write ออก socket): ข้อมูลวิดีโอ 2 GB ต้องถูก ก๊อปหลายรอบ
ดิสก์ → Page Cache (kernel) → byte\[] ใน JVM (user space) → Socket buffer (kernel) → การ์ดเครือข่าย

zero copy (ไม่มีการก๊อปเข้า user space) ประหยัด CPU และไม่สร้างขยะใน heap



-----How to compile-----
first terminal
--> cd src
--> javac *.java
--> chcp 65001

this run traditional
--> java Server
this run NIO
--> java Server nio

second terminal
--> cd src
--> javac *.java
--> chcp 65001
traditional
--> java Client video.mp4 10
NIO
--> java Client video.mp4 10 nio

hash(ต้นฉบับกับหลังส่งต้องตรงกัน)
ต้นฉบับ
--> cd files

certutil -hashfile video.mp4 SHA256
หลังส่ง
--> cd..

certutil -hashfile video.mp4 SHA256

LIST INFO GET
javac FileClient.java MultiClient.java
java FileClient --list
java FileClient --info video.mp4
java FileClient --get video.mp4 0 1000
dir part\_video.mp4

\----คิด offset lenght ----
chunk  = size / n                (หารปัดลง)
worker i: offset = i \* chunk
length = chunk                  ถ้า i ไม่ใช่ตัวสุดท้าย
length = size - offset          ถ้า i เป็นตัวสุดท้าย (รับเศษด้วย)

\----I/O-----
Traditional:  ดิสก์ → เมมโมรี่ของ kernel → buffer ใน Java (byte\[]) → kernel socket buffer → เครือข่าย
NIO transfer: ดิสก์ → เมมโมรี่ของ kernel ────────────────────────→ socket   (ไม่ผ่าน byte\[] ใน Java)



