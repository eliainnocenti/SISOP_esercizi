import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        Counter c = new Counter();
        Queue q = new Queue(10);

        Requester[] r = new Requester[3];
        for(int i = 0; i < r.length; i++) {
            r[i] = new Requester(c, q);
            r[i].setName("R" + i);
            r[i].start();
        }

        WorkerManager wm = new WorkerManager(3);

        Assigner[] a = new Assigner[2];
        for(int i = 0; i < a.length; i++) {
            a[i] = new Assigner(q, wm);
            a[i].setName("A" + i);
            a[i].start();
        }

        for(int i = 0; i < 20; i++){
            Thread.sleep(1000);
            wm.print();
        }

        for(int i = 0; i < r.length; i++)
            r[i].interrupt();

        for(int i = 0; i <a.length; i++)
            a[i].interrupt();

        wm.interrupt();
        wm.print();
    }
}
class Counter {
    private int value = 0;

    public synchronized int getValue() {
        return value++;
    }
}
class Msg {
    Requester r;
    private int value;

    public Msg(Requester r, int value) {
        this.r = r;
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}
class Queue {
    private ArrayList<Msg> data;
    private int max;

    public Queue(int max) {
        this.max = max;
        data = new ArrayList<Msg>();
    }

    public synchronized void putMsg(Msg m) throws InterruptedException {
        while (data.size() >= max)
            wait();
        data.add(m);
        notifyAll();
    }

    public synchronized Msg getMsg() throws InterruptedException {
        while (data.size() == 0)
            wait();
        Msg m = data.remove(0);
        notifyAll();
        return m;
    }
}
class Requester extends Thread {
    private Counter c;
    private Queue q;
    private Integer result;

    public Requester(Counter c, Queue q) {
        this.c = c;
        this.q = q;
    }

    public synchronized int getResult() throws InterruptedException {
        while (result == null)
            wait();
        int r = result;
        result = null;
        notifyAll();
        return r;
    }

    public synchronized void putResult(int r) throws InterruptedException {
        while (result != null)
            wait();
        result = r;
        notifyAll();
    }

    public void run() {
        try {
            while (true) {
                int v = c.getValue();
                long ts = System.nanoTime();
                q.putMsg(new Msg(this, v));
                int r = getResult();
                long te = System.nanoTime();
                System.out.println(getName() + " sent: " + v + ", received: " + r + ", time: " + (te-ts) + "ns");
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }

}
class Assigner extends Thread {
    private Queue q;
    private WorkerManager wm;

    public Assigner(Queue q, WorkerManager wm) {
        this.q = q;
        this.wm = wm;
    }

    public void run() {
        try {
            while(true) {
                Msg m = q.getMsg();
                Worker w = wm.getWorker();
                w.putMsg(m);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }
}
class Worker extends Thread {
    int nUsed = 0;
    boolean free = true;
    private WorkerManager wm;
    private Msg msg = null;

    public Worker(WorkerManager wm) {
        this.wm = wm;
    }

    public void run() {
        try {
            while(true) {
                Msg m = this.getMsg();
                m.r.putResult(m.getValue() * 2);
                wm.releaseWorker(this);
            }
        } catch(InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }

    public synchronized Msg getMsg() throws InterruptedException {
        while (msg == null)
            wait();
        Msg r = msg;
        msg = null;
        notifyAll();
        return r;
    }

    public synchronized void putMsg(Msg m) throws InterruptedException {
        while (msg != null)
            wait();
        msg = m;
        notifyAll();
    }
}
class WorkerManager {
    private Worker[] ws;
    private int nFree;

    public WorkerManager(int n) {
        ws = new Worker[n];
        for (int i = 0; i < n; i++) {
            ws[i] = new Worker(this);
            ws[i].setName("W" + i);
            ws[i].start();
        }
        nFree = n;
    }

    public synchronized Worker getWorker() throws InterruptedException {
        while(nFree == 0)
            wait();
        int pmin = -1;
        for (int i = 0; i < ws.length; i++) {
            if (ws[i].free) {
                if(pmin == -1 || ws[i].nUsed<ws[pmin].nUsed) {
                    pmin = i;
                }
            }
        }
        Worker r = ws[pmin];
        nFree--;
        ws[pmin].nUsed++;
        ws[pmin].free = false;
        return r;
    }

    public synchronized void releaseWorker(Worker w) {
        w.free = true;
        nFree++;
        notify();
    }
    public void interrupt() {
        for(int i = 0; i < ws.length; i++)
            ws[i].interrupt();
    }
    public void print() {
        for(int i = 0; i < ws.length; i++)
            System.out.print(ws[i].getName() + ": " + ws[i].nUsed + " ");
        System.out.println();
    }
}