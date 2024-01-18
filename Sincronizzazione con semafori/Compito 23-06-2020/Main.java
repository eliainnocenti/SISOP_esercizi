import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int K1 = 5;
        int K2 = 8;
        int M = 6;

        int NC = (M * K1 + K2 - 1) / K2;

        Queue s = new Queue(K2);
        Collector c = new Collector(NC);
        ProcessorThread[] t = new ProcessorThread[10];

        for (int i = 0; i < t.length; i++) {
            t[i] = new ProcessorThread(s, c, K2);
            t[i].setName("PR" + i);
            t[i].start();
        }
        PrinterThread pt = new PrinterThread(c, NC);
        pt.setName("PRT");
        pt.start();

        for (int i = 0; i < M; i++) {
            ArrayList<Integer> x = new ArrayList<>();
            for (int j = 0; j < K1; j++) {
                x.add(j);
            }
            s.put(x);
        }
        s.put(null);
        pt.join();
        for (int i = 0; i < t.length; i++) {
            t[i].interrupt();
        }
    }

}
class Queue {
    private ArrayList<Integer> data = new ArrayList<>();
    private Semaphore mutex = new Semaphore(1);
    private Semaphore pieni = new Semaphore(0);
    private int id = 0;
    private int K2;

    public Queue(int K2) {
        this.K2 = K2;
    }

    public void put(ArrayList<Integer> v) throws InterruptedException {
        if (v == null) {
            pieni.release(K2);
            return;
        }
        mutex.acquire();
        data.addAll(v);
        mutex.release();
        pieni.release();
    }

    public ArrayList<Integer> get(int n) throws InterruptedException {
        pieni.acquire();
        mutex.acquire();
        ArrayList<Integer> r = new ArrayList<>();
        r.add(id++);
        for (int i = 0; data.size() > 0 && i < n; i++) {
            r.add(data.remove(0));
        }
        mutex.release();
        if(r.size()==1)
            return null;
        return r;
    }
}

class Collector {
    private ArrayList<Integer>[] data;
    private Semaphore[] s;

    public Collector(int NC) {
        data = new ArrayList[NC];
        s = new Semaphore[NC];
        for (int i = 0; i < s.length; i++) {
            s[i] = new Semaphore(0);
        }
    }

    public void put(ArrayList<Integer> v) throws InterruptedException {
        data[v.get(0)] = v;
        s[v.get(0)].release();
    }

    public ArrayList<Integer> get(int p) throws InterruptedException {
        s[p].acquire();
        return data[p];
    }
}

class ProcessorThread extends Thread {
    Queue s;
    Collector c;
    int K2;

    public ProcessorThread(Queue s, Collector c, int K2) {
        this.s = s;
        this.c = c;
        this.K2 = K2;
    }

    public void run() {
        try {
            while (true) {
                ArrayList<Integer> v = s.get(K2);

                if(v == null)
                    break;

                sleep(1000 + (int) (Math.random() * 2000));

                for (int i = 1; i < v.size(); i++)
                    v.set(i,v.get(i)*2);

                c.put(v);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }
}

class PrinterThread extends Thread {
    private Collector c;
    private int NC;

    public PrinterThread(Collector c, int NC) {
        this.c = c;
        this.NC = NC;
    }

    public void run() {
        try {
            ArrayList<Integer> xx;

            for(int i=0;i<NC; i++) {
                xx = c.get(i);
                xx.remove(0);
                System.out.println(i + ": " + xx);
            }

        } catch(InterruptedException e) {
            System.out.println("Printer Thread interrotto");
        }
    }
}

