import java.util.ArrayList;
import java.util.Collections;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int N = 10;
        int M = 7;
        int X = 50;
        int T = 20;
        int TT = 10;
        int L = 50;

        Queue q = new Queue(L, N, M);
        Queue_2 q2 = new Queue_2(M);

        Generator[] g = new Generator[N];
        for (int i = 0; i < g.length; i++) {
            g[i] = new Generator(q, i, X);
            g[i].setName("G" + i);
            g[i].start();
        }

        Worker[] w = new Worker[M];
        for (int i = 0; i < w.length; i++) {
            w[i] = new Worker(M, q, q2, i, T, TT);
            w[i].setName("W" + i);
            w[i].start();
        }

        PrinterThread pt = new PrinterThread(q2);
        pt.setName("PT");
        pt.start();

        Thread.sleep(10000);

        for (Generator gg:g)
            gg.interrupt();

        for (Worker ww:w)
            ww.interrupt();

        pt.interrupt();
    }
}
class Msg {
    private int value;

    public Msg(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
class Queue {
    private ArrayList<Msg> data;
    private int max;
    private boolean[] generated;
    private boolean[] worked;
    private int nworker;
    private int wrk = 0;

    public Queue(int max, int ngenerator, int nworker) {
        data = new ArrayList<Msg>();
        this.max = max;
        generated = new boolean[ngenerator];
        worked = new boolean[nworker];
        this.nworker = nworker;
    }

    public synchronized void putMsg(Msg m, int generator) throws InterruptedException {
       while (generated[generator])
           wait();
       data.add(m);
       generated[generator] = true;
       notifyAll();
    }

    public synchronized Msg getMsg(int worker) throws InterruptedException {
        while (data.size() < nworker || worked[worker])
            wait();
                //System.out.println(Thread.currentThread().getName() + " è qui");
        Msg m = data.get(0);
                //System.out.println(Thread.currentThread().getName() + " ha preso il primo elemento della coda");
        worked[worker] = true;
        wrk++;
        if (wrk == nworker) {
                //System.out.println(Thread.currentThread().getName() + " è qui");
            wrk = 0;
            data.clear();
            generated = new boolean[generated.length];
            worked = new boolean[worked.length];
        }
        notifyAll();
        return m;
    }
}
class Queue_2 {
    private ArrayList<Integer> data = new ArrayList<Integer>();
    private int nworker;

    public Queue_2(int nworker) {
        this.nworker = nworker;
    }

    public synchronized void put(Integer[] v) throws InterruptedException {
        while (data.size() != 0)
            wait();
        for (int i = 0; i < v.length; i++) {
            data.add(v[i]);
                //System.out.println(Thread.currentThread().getName() + " ha inserito in q2 il valore: " + v[i]);
        }
        notifyAll();
    }

    public synchronized Integer[] get() throws InterruptedException {
        while (data.size() != nworker)
            wait();
            //System.out.println("data.size: " + data.size());
        Integer[] r = new Integer[nworker];
        for (int i = 0; i < nworker; i++)
            r[i] = data.remove(0);
        notifyAll();
        return r;
    }
}
class Generator extends Thread {
    private int value;
    private int id;
    private Queue q;
    private int X;
    private int generations = 0;

    public Generator(Queue q, int id, int X) {
        this.q = q;
        this.id = id;
        this.X = X;
        value = id * 100 +1 ;
    }

    public void run() {
        try {
            while (true) {
                q.putMsg(new Msg(value), id);
                    //System.out.println(getName() + " ha messo un nuovo messaggio nella coda");
                generations++;
                sleep(X);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }

    public int getGenerations() {
        return generations;
    }
}
class Worker extends Thread {
    public static Integer[] data = new Integer[7];
    private int nworker;
    private static int wrkDone = 0;
    private Queue q;
    private Queue_2 q2;
    private int id;
    private int T, TT;

    public Worker(int M, Queue q, Queue_2 q_2, int id, int T, int TT) {
        this.nworker = M;
        this.q = q;
        this.q2 = q_2;
        this.id = id;
        this.T = T;
        this.TT = TT;
    }

    public void run() {
        try {
            while (true) {
                Msg m = q.getMsg(id);
                int result = m.getValue() * (id+1);
                data[id] = result;
                /*
                    for (int i = 0; i < data.length; i++) {
                        System.out.print(data[i] + " ");
                    }
                    System.out.println();
                 */
                wrkDone++;
                    //System.out.println(getName() + " wrkDone: " + wrkDone);
                if (wrkDone == nworker) {
                        //System.out.println(getName() + " è qui ****");
                    q2.put(data);
                    wrkDone = 0;
                    data = new Integer[nworker];
                }
                sleep(T + (int)(Math.random() * TT));
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }
}
class PrinterThread extends Thread {
    private Queue_2 q2;
    private int nstampe = 0;

    public PrinterThread(Queue_2 q2) {
        this.q2 = q2;
    }

    public void run() {
        try {
            while (true) {
                Integer[] r = q2.get();
                System.out.print(getName() + " stampa: [ ");
                for (int i = 0; i < r.length; i++)
                    System.out.print(r[i] + " ");
                System.out.println("]");
                nstampe++;
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }

    public int getNstampe() {
        return nstampe;
    }
}