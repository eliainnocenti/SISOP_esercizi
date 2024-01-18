import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Queue q = new Queue();
        SharedCounter sc = new SharedCounter();

        Generator[] g = new Generator[3];
        Consumer[] c = new Consumer[3];

        for (int i = 0; i < g.length; i++) {
            g[i] = new Generator(sc, q, 500);
            g[i].start();
            g[i].setName("P" + i);
        }

        for (int i = 0; i < c.length; i++) {
            c[i] = new Consumer(q, 100);
            c[i].start();
            c[i].setName("C" + i);
        }

        Thread.sleep(10000);

        for (Generator gg:g) {
            gg.interrupt();
        }

        q.closing = true;
        if(q.getSize()>0)
            q.empty.acquire();

        for(Consumer cc:c) {
            cc.interrupt();
        }

        System.out.println("c = " + (sc.getValue()-1));
    }
}

class SharedCounter {
    private int v = 0;
    private Semaphore mutex = new Semaphore(1);

    public int getValue () throws InterruptedException {
        mutex.acquire();
        int r = ++v;
        mutex.release();
        return r;
    }

}

class Generator extends Thread {
    private SharedCounter sc;
    private Queue q;
    private int TG;

    public Generator(SharedCounter sc, Queue q, int TG) {
        this.sc = sc;
        this.q = q;
        this.TG = TG;
    }

    public void run() {
        try {
            while (true) {
                double casualNum = Math.random() * 100;
                Msg msg = new Msg(sc.getValue(), (int) casualNum);
                q.put(msg);
                sleep(TG);
            }
        } catch (InterruptedException e) {

        }
    }
}

class Queue {
    private ArrayList<Msg> queue = new ArrayList<>();
    private Semaphore mutex = new Semaphore(1);
    private Semaphore piene = new Semaphore(0);
    Semaphore empty = new Semaphore(0);
    boolean closing = false;

    public void put(Msg msg) throws InterruptedException {
        mutex.acquire();
        queue.add(msg);
        mutex.release();
        piene.release();
    }

    public Msg get() throws InterruptedException {
        piene.acquire();
        mutex.acquire();
        int pmin = 0;
        for (int i = 0; i < queue.size(); i++) {
            if(queue.get(i).t < queue.get(pmin).t) {
                pmin = i;
            }
        }
        Msg m = queue.remove(pmin);
        if(closing && queue.size()==0)
            empty.release();
        mutex.release();
        return m;
    }

    public int getSize() throws InterruptedException {
        mutex.acquire();
        int dim = queue.size();
        mutex.release();
        return dim;
    }
}

class Msg {
    int t, v;

    public Msg (int t, int v) {
        this.t = t;
        this.v = v;
    }

    @Override
    public String toString() {
        return "(t: " + t + ", v: " + v + ")";
    }
}

class Consumer extends Thread {
    private Queue q;
    private int TC;

    public Consumer (Queue q, int TC) {
        this.q = q;
        this.TC = TC;
    }

    public void run() {
        try {
            while (true) {
                Msg m = q.get();
                System.out.println(getName() + " msg: " + m);
                sleep(TC);
            }
        } catch (InterruptedException e) {

        }
    }

}