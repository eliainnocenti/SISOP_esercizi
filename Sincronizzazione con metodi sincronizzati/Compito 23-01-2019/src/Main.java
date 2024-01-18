import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int N = 3; //numero Genrator
        int T = 100; //sleep
        int P = 3; //numero priorità
        int nMsg = 1000;

        Queue[] q = new Queue[P];
        for (int i = 0; i < q.length; i++) {
            q[i] = new Queue();
        }

        Generator[] g = new Generator[N];
        for (int i = 0; i < g.length; i++) {
            g[i] = new Generator(N*nMsg, q, T);
            g[i].setName("G" + i);
            g[i].start();
        }

        Processor p = new Processor(N, nMsg, q, T);
        p.setName("PROCESSOR");
        p.start();

        for (Generator gg:g)
            gg.join();

        p.join();

        System.out.println("nProcessed: " + p.getNProcessed());
        System.out.println("Sistema terminato");

    }
}
class Msg {
    private int value;
    private int priority;
    private int idMsg;
    private long enterQueue;
    private long exitQueue;

    public Msg(int priority, int idMsg) {
        this.value = priority * 1000 + idMsg;
        this.priority = priority;
        this.idMsg = idMsg;
    }

    public int getValue() {
        return value;
    }
    public int getPriority() {
        return priority;
    }
    public void setEnterQueue(long enterQueue) {
        this.enterQueue = enterQueue;
    }
    public void setExitQueue(long exitQueue) {
        this.exitQueue = exitQueue;
    }
    public int getTempoAttesa() {
        return (int)(exitQueue-enterQueue);
    }
}
class Queue {
    private ArrayList<Msg> data = new ArrayList<>();
    public synchronized void put(Msg m) throws InterruptedException {
        data.add(m);
                //System.out.println(Thread.currentThread().getName() + " ha messo nella coda un vuovo messaggio");
        notifyAll();
    }
    public synchronized Msg get() throws InterruptedException {
        while (data.size() == 0)
            wait();
        Msg m = data.remove(0);
        notifyAll();
        return m;
    }
    public int getSize() {
        return data.size();
    }
}
class Generator extends Thread {
    private int nMsg;
    private int count = 0;
    private Queue[] q;
    private int T;
    public Generator(int nMsg, Queue[] q, int T) {
        this.nMsg = nMsg;
        this.q = q;
        this.T = T;
    }

    public void run() {
        try {
            for (int i = 0; i < nMsg; i++) {
                int priority = (int) (Math.random() * 3);
                Msg m = new Msg(priority, count);
                q[priority].put(m);
                m.setEnterQueue(System.currentTimeMillis());
                System.out.println(getName() + " ha generato il messaggio: Msg: { value = " + m.getValue() + ", priority = " + m.getPriority() + " }");
                count++;
                sleep(T);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " ha finito di generare 1000 messaggi");
        }
    }
}
class Processor extends Thread {
    private int nProcessed = 0;
    private int nMsgtot;
    private Queue[] q;
    private int T;
    public Processor(int N, int nMsg, Queue[] q, int T) {
        this.nMsgtot = N * nMsg;
        this.q = q;
        this.T = T;
    }

    public void run() {
        try {
            while (nProcessed != nMsgtot) {
                Msg m;
                        //System.out.println(getName() + " è qui *********************+");
                if (q[0].getSize() != 0)
                    m = q[0].get();
                else if (q[0].getSize() == 0 && q[1].getSize() != 0)
                    m = q[1].get();
                else /*if (q[0].getSize() == 0 && q[1].getSize() == 0 && q[2].getSize() != 0)*/ {
                    m = q[2].get();
                }
                nProcessed++;
                m.setExitQueue(System.currentTimeMillis());
                System.out.println(getName() + " stampa il messaggio Msg: { value = " + m.getValue() + ", priority = " + m.getPriority() + ", tempo di attesa: " + m.getTempoAttesa() + " }");
                sleep(T);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " ha stampato tutti i messaggi");
        }
    }
    public int getNProcessed() {
        return nProcessed;
    }
}