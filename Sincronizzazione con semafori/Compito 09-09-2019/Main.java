import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        ResourceManager ma=new ResourceManager(5);
        ResourceManager mb=new ResourceManager(5);

        Queue qx = new Queue(6);
        Queue qy = new Queue(0);

        Producer[] pp = new Producer[3];
        for(int i=0;i<pp.length;i++){
            pp[i] = new Producer(ma, mb, qx);
            pp[i].start();
        }
        Consumer[] cc = new Consumer[2];
        for(int i=0; i<cc.length; i++) {
            cc[i] = new Consumer(qx, qy);
            cc[i].start();
        }
        Terminator t = new Terminator(qy, ma, mb);
        t.start();
        Thread.sleep(20000);
        for(int i=0; i<pp.length; i++) {
            pp[i].interrupt();
            pp[i].join();
            System.out.println("P"+i+" "+pp[i].nX);
        }
        for(int i=0; i<cc.length; i++) {
            cc[i].interrupt();
            cc[i].join();
            System.out.println("C"+i+" "+cc[i].nY);
        }
        t.interrupt();
        t.join();
        System.out.println("T "+t.nT);
        System.out.println("Qx: "+qx.getSize()+" Qy:"+qy.getSize());
        for(int i=0;i<qx.getSize(); i++) {
            ObjectX x = (ObjectX)qx.get();
            ma.releaseResource(x.a);
            mb.releaseResource(x.b);
        }
        for(int i=0;i<qy.getSize(); i++) {
            ObjectY y=(ObjectY) qy.get();
            ma.releaseResource(((ObjectX)y.x[0]).a);
            mb.releaseResource(((ObjectX)y.x[0]).b);
            ma.releaseResource(((ObjectX)y.x[1]).a);
            mb.releaseResource(((ObjectX)y.x[1]).b);
        }
        System.out.println("Na: "+ma.getSize()+" Nb:"+mb.getSize());
    }

}

class Resource {

}

class ResourceManager {
    private ArrayList<Resource> rs;
    private Semaphore r;
    private Semaphore mutex = new Semaphore(1);

    public ResourceManager(int nr) {
        rs=new ArrayList();
        for(int i=0;i<nr;i++)
            rs.add(new Resource());
        r=new Semaphore(nr);
    }

    public int getSize() {
        return rs.size();
    }

    public Resource getResource() throws InterruptedException {
        r.acquire();
        mutex.acquire();
        Resource rr = rs.remove(0);
        mutex.release();
        return rr;
    }

    public void releaseResource(Resource rr) throws InterruptedException {
        mutex.acquire();
        rs.add(rr);
        mutex.release();
        r.release();
    }
}

class Queue {
    private ArrayList data = new ArrayList();
    private int max = 0; // 0 = coda illimitata
    private Semaphore mutex = new Semaphore(1);
    private Semaphore piene = new Semaphore(0);
    private Semaphore vuote;

    public Queue(int max) {
        this.max = max;
        if(max>0)
            vuote = new Semaphore(max);
    }

    public int getSize() {
        return data.size();
    }

    public Object get() throws InterruptedException {
        piene.acquire();
        mutex.acquire();
        Object v = data.remove(0);
        mutex.release();
        if(max>0)
            vuote.release();
        return v;
    }

    public Object[] get(int n) throws InterruptedException {
        piene.acquire(n);
        mutex.acquire();
        Object[] v=new Object[n];
        for(int i=0;i<n;i++)
            v[i]=data.remove(0);
        mutex.release();
        if(max>0)
            vuote.release(n);
        return v;
    }

    public void put(Object v) throws InterruptedException {
        if(max>0)
            vuote.acquire();
        mutex.acquire();
        data.add(v);
        mutex.release();
        piene.release();
    }
}

class ObjectX {
    Resource a,b;

    public ObjectX(Resource a, Resource b) {
        this.a = a;
        this.b = b;
    }

}

class ObjectY {
    Object[] x;

    public ObjectY(Object[] x) {
        this.x = x;
    }
}

class Producer extends Thread {
    private ResourceManager ma,mb;
    private Queue q;
    public int nX;

    public Producer(ResourceManager ma, ResourceManager mb, Queue q) {
        this.ma = ma;
        this.mb = mb;
        this.q = q;
    }

    public void run() {
        try {
            while(true) {
                Resource a = ma.getResource();
                Resource b = null;
                try {
                    b = mb.getResource();
                    ObjectX x = new ObjectX(a, b);
                    nX++;
                    q.put(x);
                } catch(InterruptedException e){
                    ma.releaseResource(a);
                    if(b!=null)
                        mb.releaseResource(b);
                    break;
                }
            }
        } catch(InterruptedException e) {

        }
    }
}

class Consumer extends Thread {
    private Queue qx;
    private Queue qy;
    public int nY;

    public Consumer(Queue qx, Queue qy) {
        this.qx = qx;
        this.qy = qy;
    }

    public void run() {
        try {
            while(true) {
                Object[] xx=qx.get(2);
                ObjectY y = new ObjectY(xx);
                nY++;
                try {
                    qy.put(y);
                } catch(InterruptedException e) {
                    qy.put(y);
                    break;
                }
            }
        } catch(InterruptedException e) {

        }

    }
}

class Terminator extends Thread {
    private Queue qy;
    private ResourceManager ma,mb;
    public int nT = 0;

    public Terminator(Queue qy, ResourceManager ma, ResourceManager mb) {
        this.qy = qy;
        this.ma = ma;
        this.mb = mb;
    }

    public void run() {
        try {
            while(true) {
                ObjectY y = null;
                try {
                    y = (ObjectY)qy.get();
                } finally {
                    if(y!=null) {
                        ma.releaseResource(((ObjectX)y.x[0]).a);
                        mb.releaseResource(((ObjectX)y.x[0]).b);
                        ma.releaseResource(((ObjectX)y.x[1]).a);
                        mb.releaseResource(((ObjectX)y.x[1]).b);
                    }
                }
                nT++;
            }
        } catch(InterruptedException e) {

        }
    }
}