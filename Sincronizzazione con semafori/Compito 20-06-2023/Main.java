import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int N = 10;
        int M = 4;
        int L = 40;
        int X = 50;
        int T = 20;
        int D = 5;

        Queue q = new Queue(L);
        OutputManager om = new OutputManager();

        Generator[] g = new Generator[N];
        for (int i = 0; i < g.length; i++) {
            g[i] = new Generator(q,X);
            g[i].setName("G" + i);
            g[i].start();
        }

        Worker[] w = new Worker[M];
        for (int i = 0; i < w.length; i++) {
            w[i] = new Worker(N, q, om, T, D);
            w[i].setName("W" + i);
            w[i].start();
        }

        OutputThread[] ot = new OutputThread[2];
        for (int i = 0; i < ot.length; i++) {
            ot[i] = new OutputThread(om, M);
            ot[i].setName("OT" + i);
            ot[i].start();
        }

        Thread.sleep(10000);

        int nWorktot_g = 0;
        for (Generator gg:g) {
            gg.interrupt();
            System.out.println(gg.getName() + " ha generato " + gg.getNWork() + " messggi");
            nWorktot_g += gg.getNWork();
        }
        System.out.println("Sono stati generati in totale " + nWorktot_g + " messaggi");

        int nWorktot_w = 0;
        for (Worker ww:w) {
            ww.interrupt();
            System.out.println(ww.getName() + " ha effettuato " + ww.getNWork() + " elaborazioni");
            nWorktot_w += ww.getNWork();
        }
        System.out.println("Sono state effettuate in totale " + nWorktot_w + " elaborazioni");

        int stampetot = 0;
        for (OutputThread oot:ot) {
            oot.interrupt();
            //oot.join();
            System.out.println(oot.getName() + " ha effettuato " + oot.getStampe() + " stampe");
            stampetot += oot.getStampe();
        }
        System.out.println("Sono state effettuate in totale " + stampetot + " stampe");
    }
}

class Msg {
    private String id;
    private int value;

    public Msg(String id, int value) {
        this.id = id;
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

class Queue {
    private ArrayList<Msg> data;
    private Semaphore mutex = new Semaphore(1);
    private Semaphore piene = new Semaphore(0);
    private Semaphore vuote;

    public Queue (int L) {
        data = new ArrayList<Msg>(L);
        vuote = new Semaphore(L);
    }

    public void put(Msg m) throws InterruptedException {
        vuote.acquire();
        mutex.acquire();
        data.add(m);
        mutex.release();
        piene.release();
    }

    public Msg[] get(int N) throws InterruptedException {
        piene.acquire(N);
        mutex.acquire();
        Msg[] m = new Msg[N];
        for (int i = 0; i < N; i++)
            m[i] = data.remove(0);
        mutex.release();
        vuote.release(N);
        return m;
    }
}

class Generator extends Thread {
    private Queue q;
    private int value = 1;
    private int X;
    private int nWork = 0;

    public Generator(Queue q, int X) {
        this.q = q;
        this.X = X;
    }

    public void run() {
        try {
            while (true) {
                q.put(new Msg(getName(), value));
                System.out.println(getName() + " ha generato il messaggio: (" + getName() + ", " + value + ")");
                value++;
                nWork++;
                sleep(X);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }

    public int getNWork() {
        return nWork;
    }
}

class Worker extends Thread {
    private Queue q;
    private OutputManager om;
    private int N, T, D;
    private int result = 0;
    private int nWork = 0;

    public Worker(int N, Queue q, OutputManager om, int T, int D) {
        this.N = N;
        this.q = q;
        this.om = om;
        this.T = T;
        this.D = D;
    }

    public void run() {
        try {
            while (true) {
                Msg[] m = q.get(N);
                for (int i = 0; i < m.length; i++)
                    result += m[i].getValue();
                System.out.println(getName() + " ha prodotto il risultato: " + result);
                nWork++;
                sleep(T + (int)(Math.random() * D));
                om.put(result);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }

    public int getNWork() {
        return nWork;
    }
}

class OutputManager {
    private ArrayList<Integer> data = new ArrayList<Integer>();
    private Semaphore mutex = new Semaphore(1);
    private Semaphore piene = new Semaphore(0);

    public void put(int result) throws InterruptedException {
            //System.out.println("1p put: " + piene);
            //System.out.println("1m put: " + mutex);
        mutex.acquire();
            //System.out.println("2p put: " + piene);
            //System.out.println("2m put: " + mutex);
        data.add(result);
        mutex.release();
        piene.release();
            //System.out.println("3p put: " + piene);
            //System.out.println("3m put: " + mutex);
    }

    public int[] get(int M) throws InterruptedException {
            //System.out.println("1p get: " + piene);
            //System.out.println("1m get: " + mutex);
        piene.acquire(M);
        mutex.acquire();
            //System.out.println("2p get: " + piene);
            //System.out.println("2m get: " + mutex);
        int[] r = new int[M];
        for (int i = 0; i < r.length; i++)
            r[i] = data.remove(0);
        mutex.release();
            //System.out.println("3p get: " + piene);
            //System.out.println("3m get: " + mutex);
        return r;
    }
}

class OutputThread extends Thread {
    private OutputManager om;
    private int M;
    private int stampe = 0;

    public OutputThread(OutputManager om, int M) {
        this.om = om;
        this.M = M;
    }

    public void run() {
        try {
            while (true) {
                int[] result = om.get(M);
                System.out.print(getName() + " stampa: [ ");
                for (int i = 0; i < result.length; i++)
                    System.out.print(result[i] + " ");
                System.out.println("]");
                stampe++;
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }

    public int getStampe() {
        return stampe;
    }
}