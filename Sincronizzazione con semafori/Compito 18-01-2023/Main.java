import java.util.ArrayList;
import java.util.PrimitiveIterator;
import java.util.concurrent.Semaphore;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int N = 5; //numero thread Generator
        int K = 5; //dimensione Queue
        int TG = 10; //tempo attesa Generator
        int M = 4; //numero thread Worker
        int TA = 50; //tempo di utilizzo della risorsa A
        int TB = 10; //tempo di utilizzo della risorsa B
        int NA = 2; //numero risorse A
        int NB = 3; //numero risorse B

        SharedCounter sc = new SharedCounter();

        Queue q1 = new Queue(K);
        Queue q2 = new Queue(0);

        ResourseManager rm = new ResourseManager(NA, NB, TA, TB);

        Generator[] g = new Generator[N];
        for (int i = 0; i < g.length; i++) {
            g[i] = new Generator(sc, q1, i, TG);
            g[i].setName("G" + i);
            g[i].start();
        }

        Worker[] w = new Worker[M];
        for (int i = 0; i < w.length; i++) {
            w[i] = new Worker(q1, q2, rm, i);
            w[i].setName("W" + i);
            w[i].start();
        }

        Collector c = new Collector(q2);
        c.setName("C");
        c.start();

        Thread.sleep(10000);

        for (Generator gg:g)
            gg.interrupt();

        for (Worker ww:w)
            ww.interrupt();

        c.interrupt();
        c.join();

        int gtot = 0;
        for (Generator gg:g) {
            System.out.println("Il generatore " + gg.getName() + " ha generato: " + gg.getnGenerated() + " messaggi");
            gtot += gg.getnGenerated();
        }
        System.out.println("Sono stati generati in totale: " + gtot + " messaggi");

        int ptot = 0;
        for (Worker ww:w) {
            System.out.println("Il worker " + ww.getName() + " ha processato: " + ww.getnProcessed() + " messaggi");
            ptot += ww.getnProcessed();
        }
        System.out.println("Sono stati processati in totale: " + ptot + " messaggi");

        System.out.println("Le risorse disponibili sono in totale: " + rm.getNA() + " risorse di tipo A e " + rm.getNB() + " risorse di tipo B");
    }
}
class SharedCounter {
    private int p = 0; //numero progressivo
    private Semaphore mutex = new Semaphore(1);
    public int getP() throws InterruptedException {
        mutex.acquire();
        int r = p++;
        mutex.release();
        return r;
    }
}
class Msg {
    private int p;
    private int idGenerator;
    private int idWorker;
    private int value;
    private int initialValue;
    public Msg(int p, int idGenerator, int v) {
        this.p = p;
        this.idGenerator = idGenerator;
        this.value = v;
        this.initialValue = v;
    }
    public int getP() {
        return p;
    }
    public int getInitialValue() {
        return initialValue;
    }
    public int getValue() {
        return value;
    }
    public int getIdGenerator() {
        return idGenerator;
    }
    public int getIdWorker() {
        return idWorker;
    }
    public void setNewValue(int id){
        value *= id;
    }
    public void setIdWorker(int id) {
        this.idWorker = id;
    }
}
class Queue {
    private ArrayList<Msg> data = new ArrayList<Msg>();
    private Semaphore mutex = new Semaphore(1);
    private Semaphore piene = new Semaphore(0);
    private Semaphore vuote;
    private int K;
    public Queue(int K) {
        this.K = K;
        if (K > 0)
            vuote = new Semaphore(K);
    }
    public void putMsg(Msg m) throws InterruptedException {
        if (K > 0)
            vuote.acquire();
        mutex.acquire();
        data.add(m);
        mutex.release();
        piene.release();
    }

    public Msg getMsg() throws InterruptedException {
        piene.acquire();
        mutex.acquire();
        int pmin = 0;
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getP() < data.get(pmin).getP())
                pmin = i;
        }
        Msg m = data.remove(pmin);
        mutex.release();
        if (K > 0)
            vuote.release();
        return m;
    }
    public int getSize() throws InterruptedException {
        mutex.acquire();
        int s = data.size();
        mutex.release();
        return s;
    }
}
class Generator extends Thread {
    private int v = 0;
    private SharedCounter sc;
    private Queue q1;
    private int id;
    private int TG;
    private int nGenerated = 0;
    public Generator(SharedCounter sc, Queue q1, int id, int TG) {
        this.sc = sc;
        this.q1 = q1;
        this.id = id;
        this.TG = TG;
    }

    public void run() {
        try {
            while (true) {
                q1.putMsg(new Msg(sc.getP(), id, v++));
                nGenerated++;
                sleep(TG);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }
    public int getnGenerated() {
        return nGenerated;
    }
}
class Worker extends Thread {
    private Queue q1;
    private Queue q2;
    private ResourseManager rm;
    private int id;
    private int nProcessed = 0;
    public Worker(Queue q1, Queue q2, ResourseManager rm, int id) {
        this.q1 = q1;
        this.q2 = q2;
        this.rm = rm;
        this.id = id;
    }
    public void run() {
        try {
            while (true) {
                Msg m = q1.getMsg();
                rm.getA();
                try {
                    sleep(rm.getTA());
                    rm.getB();
                    try {
                        sleep(rm.getTB());
                    } finally {
                        rm.releaseB();
                    }
                } finally {
                        rm.releaseA();
                }

                m.setNewValue(id);
                m.setIdWorker(id);
                q2.putMsg(m);
                nProcessed++;
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }
    public int getnProcessed() {
        return nProcessed;
    }
}
class ResourseManager {
    private int NA, NB, TA, TB;
    private Semaphore rA;
    private Semaphore rB;
    public ResourseManager(int NA, int NB, int TA, int TB) {
        this.NA = NA;
        this.NB = NB;
        this.TA = TA;
        this.TB = TB;
        rA = new Semaphore(NA);
        rB = new Semaphore(NB);
    }
    public void getA() throws InterruptedException{
        rA.acquire();
    }
    public void getB() throws InterruptedException{
        rB.acquire();
    }
    public void releaseA(){
        rA.release();
    }
    public void releaseB(){
        rB.release();
    }
    public int getTA() {
        return TA;
    }
    public int getTB() {
        return TB;
    }
    public int getNA() {
        return NA;
    }
    public int getNB() {
        return NB;
    }
}
class Collector extends Thread {
    private Queue q2;
    public Collector(Queue q2) {
        this.q2 = q2;
    }

    public void run() {
        try {
            while (true) {
                Msg m = q2.getMsg();
                System.out.println(getName() + " stampa nuovo messaggio -> p: " + m.getP() + ", id Generatore: " + m.getIdGenerator() + ", id Worker: " + m.getIdWorker() + ", initial value: " + m.getInitialValue() + ", final value: " + m.getValue());
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }
}