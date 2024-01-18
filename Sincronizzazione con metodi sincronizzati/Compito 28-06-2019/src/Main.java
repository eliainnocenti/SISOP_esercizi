public class Main {
    public static void main(String[] args) throws InterruptedException {

        Counter c = new Counter();
        ResourceManager rm = new ResourceManager(1, 1);

        InputThread[] it = new InputThread[3];
        MidLevelThread[] mt = new MidLevelThread[4];
        OutputThread[] ot = new OutputThread[2];

        Level l1 = new Level(it.length,mt.length);
        Level l2 = new Level(mt.length,ot.length);

        for(int i = 0; i < it.length; i++) {
            it[i] = new InputThread(c, l1, i);
            it[i].setName("InT" + i);
            it[i].start();
        }

        for(int i = 0; i < mt.length; i++) {
            mt[i] = new MidLevelThread(l1, l2, i, rm);
            mt[i].setName("MidT" + i);
            mt[i].start();
        }

        for(int i = 0; i < ot.length; i++){
            ot[i] = new OutputThread(l2, i, rm);
            ot[i].setName("OutT" + i);
            ot[i].start();
        }

        Thread.sleep(10000);

        for(int i = 0; i < it.length; i++)
            it[i].interrupt();

        for(int i = 0; i < mt.length; i++)
            mt[i].interrupt();

        for(int i = 0; i < ot.length; i++)
            ot[i].interrupt();
    }
}

class Counter {
    private  int value  = 0;

    public synchronized int getValue() {
        return value++;
    }
}
class ResourceManager {
    private int na, nb;

    public ResourceManager(int na, int nb) {
        this.na = na;
        this.nb = nb;
    }

    public synchronized int[] getAorB() throws InterruptedException {
        while (na == 0 && nb == 0) //se non ci sono né a né b, aspetta
            wait();
        if (na > 0) {
            na--;
            return new int[] {1,0};
        } else {
            nb--;
            return new int[] {0,1};
        }
    }

    public synchronized int[] getBorA() throws InterruptedException {
        while (na == 0 && nb == 0) //se non ci sono né a né b, aspetta
            wait();
        if (nb > 0) {
            nb--;
            return new int[] {0,1};
        } else {
            na--;
            return new int[] {1,0};
        }
    }

    public synchronized void release(int[] v) {
        if (v[0] > 0)
            na++;
        if (v[1] > 0)
            nb++;
        notifyAll(); //per notificare che lo stato è cambiato e quindi chi è in attesa o di A o di B, può riprovare
    }
}
class Level {
    private Integer[] values;
    private int nWrite = 0;
    private boolean[] read;
    private int nRead = 0;

    public Level(int nWriters, int nReaders) {
        values = new Integer[nWriters];
        read = new boolean[nReaders];
    }
    public synchronized void write(int v, int writer) throws InterruptedException {
        while (values[writer] != null)
            wait();
        values[writer] = v;
        nWrite++;
        notifyAll();
    }
    public synchronized Integer[] read(int reader) throws InterruptedException {
        while (nWrite < values.length || read[reader]) //se ancora non hanno finito di scrivere o se ho già letto
            wait();
        Integer[] r = values;
        read[reader] = true;
        nRead++;
        if(nRead == read.length) {//vuol dire che è l'ultimo a leggere
            nRead = 0;
            nWrite = 0;
            values = new Integer[values.length];
            read = new boolean[read.length];
        }
        notifyAll();
        return r;
    }
}
class InputThread extends Thread {
    private Counter c;
    private Level nextLevel;
    int id;

    public InputThread(Counter c, Level nextLevel, int id) {
        this.c = c;
        this.nextLevel = nextLevel;
        this.id = id;
    }

    public void run() {
        try {
            while (true) {
                int v = c.getValue();
                nextLevel.write(v, id);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }
}
class MidLevelThread extends Thread {
    private Level precLevel;
    private Level nextLevel;
    private int id;
    private ResourceManager rm;

    public MidLevelThread(Level precLevel, Level nextLevel, int id, ResourceManager rm) {
        this.precLevel = precLevel;
        this.nextLevel = nextLevel;
        this.id = id;
        this.rm = rm;
    }

    public void run() {
        try {
            while(true) {
                Integer[] v = precLevel.read(id);
                int[] r = rm.getAorB();
                int x = 0;
                for(int i = 0; i < v.length; i++) {
                    x += v[i];
                }
                rm.release(r);
                nextLevel.write(x * (id+1), id);
            }
        } catch(InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }

}
class OutputThread extends Thread {
    private Level precLevel;
    private int id;
    private ResourceManager rm;

    public OutputThread(Level precLevel, int id, ResourceManager rm) {
        this.precLevel = precLevel;
        this.id = id;
        this.rm = rm;
    }

    public void run() {
        try {
            while(true) {
                Integer[] v = precLevel.read(id);
                int[] r = rm.getBorA();
                int x = 0;
                for(int i = 0; i < v.length; i++) {
                    x += v[i];
                }
                rm.release(r);
                System.out.println(getName() + ": " + x * (id+1));
            }
        } catch(InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }
}