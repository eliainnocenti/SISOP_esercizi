/*

Si vuole realizzare in java una classe PriorityResourceManager che gestisce N risorse.
Le richieste possono essere fatte da M thread Requester, nella richiesta il thread specifica il suo id e
la priorità della richiesta, un numero tra 0 e P-1 con 0 la priorità più alta. Se la risorsa non è disponibile l
a richiesta viene accodata (con la priorità indicata), quando la risorsa sarà rilasciata verrà concessa al thread
in attesa a più alta priorità e a parità di priorità a quello in attesa da più tempo. Se invece al momento della
richiesta la risorsa è disponibile il primo thread che la trova disponibile la acquisisce.
Ogni thread Requester Ri (i=0..M-1) iterativamente richiede una risorsa a priorità i se i<P e a priorità P-1 se i >= P,
aspetta T1>0 secondi, rilascia la risorsa e aspetta T2>=0 secondi.
Il programma principale deve far partire i thread Requester distanziati di un secondo e dopo un minuto fermare i thread
in modo che rilascino l’eventuale risorsa posseduta e stampi il numero di volte che ogni thread ha acquisito la risorsa.
Nel caso di N = 2, M = 4 e P = 3 si determinino due valori per T1 e T2 per i quali almeno un thread entra in starvation
e altri due valori di T1 e T2 per i quali non si ha starvation.
Realizzare in Java il sistema descritto usando i metodi sincronizzati per la sincronizzazione tra thread.
*/

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int N = 2; //numero risorse
        int M = 4; //numero thread Requester
        int P = 3; //numero priorità
        int T1 = 3; //attesa 1
        int T2 = 0; //attesa 2

        PriorityResourseManager psm = new PriorityResourseManager(P, N);

        Requester[] r = new Requester[M];
        for (int i = 0; i < r.length; i++) {
            r[i] = new Requester(i, P, T1, T2, psm);
            r[i].setName("R" + i);
            r[i].start();
            Thread.sleep(1000);
        }

        Thread.sleep(60000);

        for (Requester rr:r)
            rr.interrupt();

        for (Requester rr:r) {
            rr.join();
            System.out.println(rr.getName() + " ha acquisito la risorsa: " + rr.getCount() + " volte");
        }

    }
}
class PriorityResourseManager {
    private ArrayList<Request>[] q;
    private int nResources;
    public PriorityResourseManager(int numP, int N) {
        q = new ArrayList[numP];
        for (int i = 0; i < q.length; i++) {
            q[i] = new ArrayList<Request>();
        }
        this.nResources = N;
    }
    public synchronized void acquire(int id, int priority) throws InterruptedException {
        q[priority].add(new Request(id, priority));
        while (!check(id,priority)) {
            System.out.println(Thread.currentThread().getName() + " è in attesa");
            wait();
        }
        nResources--;
    }
    private boolean check(int id, int priority) {
        //se non ci sono risorse -> aspetta
        if (nResources == 0)
            return false;
        //se ci sono richieste a più alta priorità -> aspetta
        for (int i = 0; i < priority; i++)
            if (q[i].size() > 0)
                return false;
        //se è il primo della coda, non attente
        if (q[priority].get(0).getIdRequester() == id) {
            q[priority].remove(0);
            return true;
        }
        return false;
    }
    public synchronized void release() {
        nResources++;
        System.out.println(Thread.currentThread().getName() + " released");
        notifyAll();
    }

}
class Request {
    private int idRequester;
    private int priority;
    public Request(int idRequester, int priority) {
        this.idRequester = idRequester;
        this.priority = priority;
    }
    public int getIdRequester() {
        return idRequester;
    }
}
class Requester extends  Thread {
    private int id;
    private int P;
    private int T1;
    private int T2;
    private PriorityResourseManager psm;
    private int count = 0;
    public Requester(int id, int P, int T1, int T2, PriorityResourseManager psm) {
        this.id = id;
        this.P = P;
        this.T1 = T1;
        this.T2 = T2;
        this.psm = psm;
    }

    public void run() {
        try {
            while (true) {
                int priority = (id < P) ? id : (P-1); //priotity = i se i < P, priority = P-1 se i >= P
                psm.acquire(id,priority);
                try {
                    sleep(T1*1000);
                    count++;
                } finally {
                    psm.release();
                }
                sleep(T2*1000);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }
    public int getCount() {
        return count;
    }
}