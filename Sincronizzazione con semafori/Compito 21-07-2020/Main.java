import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int M = 10;
        int N = 5;
        int K = 3;

        StanzeMonitor sm = new StanzeMonitor(M);

        Persona[] p = new Persona[N];
        for (int i = 0; i < p.length; i++) {
            p[i] = new Persona(sm,M,K);
            p[i].setName("P" + i);
            p[i].start();
        }

        Thread.sleep(30000);

        for (Persona pp:p)
            pp.interrupt();

        for (Persona pp:p)
            System.out.println(pp.getName() + " ha effettuato " + pp.getNVisitate() + " visite");

        for (int i = 0; i < M; i++)
            System.out.println("La stanza " + i + " ha rievuto " + sm.getNVisite(i) + " visite");
    }
}

class Stanza {
    //int nVisite;
}

class StanzeMonitor {
    private Stanza[] stanza;
    private Semaphore[] s;

    private int[] nVisite;

    public StanzeMonitor (int M) {
        stanza = new Stanza[M];
        s = new Semaphore[M];
        for (int i = 0; i < s.length; i++) {
            stanza[i] = new Stanza();
            s[i] = new Semaphore(1);
        }
        nVisite = new int[M];
    }

    public void enter(int p) throws InterruptedException {
        s[p].acquire();
    }
    public void exit(int p) {
        s[p].release();
        nVisite[p]++;
    }

    public int getMinNVisite() {
        int pmin = nVisite[0];
        ArrayList<Integer> index = new ArrayList<>();
        for (int i = 0; i < nVisite.length; i++) {
            if (nVisite[i] == pmin)
                index.add(i);
            else if (nVisite[i] < pmin) {
                pmin = i;
                index.clear();
                index.add(i);
            }
        }
        int p = (int)(index.size() * Math.random());
        return p;
    }

    /*
    public int getStanzaMin(List<Integer> visited) {
        int min = -1;
        ArrayList<Integer> pmins = new ArrayList<>();
        for(int i=0;i<used.length;i++) {
            if(!visited.contains(i)) {
                if(min==-1 || used[i]<min) {
                    min=used[i];
                    pmins.clear();
                    pmins.add(i);
                } else if (used[i]==min) {
                    pmins.add(i);
                }
            }
        }
        int p = (int)(pmins.size()*Math.random());
        System.out.println(Thread.currentThread().getName()+" mins: "+pmins+" scelgo:"+p);
        return pmins.get(p);
    }
     */

    public int getNVisite(int p) {
        return nVisite[p];
    }
}

class Persona extends Thread {
    private StanzeMonitor sm;
    private int nVisitate = 0;
    private int K;
    private int M;

    public Persona (StanzeMonitor sm, int M, int K) {
        this.sm = sm;
        this.K = K;
        this.M = M;
    }

    public void run() {
        try {
            int pIn = (int) (Math.random() * M);
            sm.enter(pIn);
            System.out.println(getName() + " inizia la visita nella stanza " + pIn);
            sleep(100);
            sm.exit(pIn);
            System.out.println(getName() + " ha finito la visita nella stanza " + pIn);
            nVisitate++;
            for (int i = 1; i < K; i++) {
                int pmin = sm.getMinNVisite();
                sm.enter(pmin);
                System.out.println(getName() + " inizia la visita nella stanza " + pmin);
                sleep(100);
                sm.exit(pmin);
                System.out.println(getName() + " ha finito la visita nella stanza " + pmin);
                nVisitate++;
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }

        /*
        public void run() {
        try {
            while(true) {
                ArrayList<Integer> visited = new ArrayList<>();
                for(int i=0;i<K; i++) {
                    int st = gs.getStanzaMin(visited);
                    System.out.println(getName()+" visita stanza "+st+" "+visited.toString());
                    gs.getStanza(st);
                    try {
                        nVisited++;
                        System.out.println(getName()+" dentro "+st);
                        visited.add(st);
                        sleep(100);
                    } finally {
                        System.out.println(getName()+" lascia "+st);
                        gs.releaseStanza(st);
                    }
                }
            }
        } catch(InterruptedException e) {

        }
    }
         */
    }

    public int getNVisitate() {
        return nVisitate;
    }
}