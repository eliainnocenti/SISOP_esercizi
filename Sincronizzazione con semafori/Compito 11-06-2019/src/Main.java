/*

Compito 11-06-2019

Si vuole realizzare il seguente sistema:
- sono presenti N thread Requester che richiedono servizi a M thread Worker tramite K thread Assigner
- le richieste dei Requester sono inserite in una coda limitata di max R richieste dalla quale i thread Assignere prelevano
  le richieste e le assegnano ad uno degli M worker disponibili
- i thread Worker ricevono la richiesta, la elaborano e quindi devono restituire il risultato al Requester
  che ha fatto la richiesta originaria
- ogni thread Requester iterativamente:
    - richiede un numero progressivo ad un contatore condiviso tra tutti i Requester
    - inserisce nella coda la sua richiesta con il valore del contatore e quindi
    - attende il risultato, quindi stampa il valore inviato, quello ricevuto ed il tempo impiegato
- può accadere che il thread Worker si blocchi (accade in media nel 10% dei casi), in questo caso il thread non risponde
  all’interrupt e l’unica possibilità è di chiamare il metodo stop
  il thread Worker è dichiarato bloccato se non produce il risultato entro 1 secondo
- fare in modo di identificare i thread Worker bloccati, prima provare a fermarli con interrupt e se non terminano
  entro 1 secondo fermarli usando lo stop, una volta fermato un thread far partire un altro thread che prenda in carico
  la richiesta bloccata, fare in modo che se la richiesta fallisce per 3 volte questa fallisca ed il requester
  proceda con un’altra richiesta
- per semplificare il testing il thread Worker restituisce come risultato il valore inviato moltiplicato per 2,
  inoltre fare in modo che nel 10% dei casi si blocchi e non termini quando viene chiamato interrupt
- come politica di assegnamento il thread Assigner deve assegnare il thread Worker usato il minor numero di volte
- il programma principale deve far partire i thread e dopo 20 secondi deve far terminare tutti i thread requester
  (facendo finire le richieste in corso) e quando tutti i Requester sono terminati stampare il numero di volte
  in cui ogni worker è stato assegnato, quanti worker sono stati terminati e fatti ripartire

Realizzare in java il sistema descritto usando i semafori per la sincronizzazione tra thread
Utilizzare il metodo statico long System.currentTimeMillis() per ottenere il numero di milli secondi trascorsi da 1/1/1970

*/

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Main {
    public static void main(String[] args) {

    }
}

class Counter { //contatore condiviso
    private int value = 0;
    private Semaphore mutex = new Semaphore(1); //semaforo che gestisce l'accesso a questo contatore

    public int getValue() throws InterruptedException {
        mutex.acquire();
        int r = value++;
        mutex.release();
        return r;
    }
}

class Queue {
    private ArrayList data = new ArrayList();
    private Semaphore mutex = new Semaphore(1);
    private Semaphore piene = new Semaphore(0);
    private Semaphore vuote; //inizializzo nel costruttore perché non conosco la dimensione

    public Queue(int max) {
        vuote = new Semaphore(max);
    }

    public void put(Object x) throws InterruptedException {
        vuote.acquire();        //prima vuote
        mutex.acquire();        //e poi mutex!!
        data.add(x);
        mutex.release();
        piene.release();
    }

    public Object get() throws InterruptedException {
        piene.acquire();        //prima piene
        mutex.acquire();        //e poi mutex!!
        Object r = data.remove(0);
        mutex.release();
        vuote.release();
        return r;
    }
}

class Buffer {
    private Object v;

    //non ci serve un mutex
    private Semaphore pieno = new Semaphore(0);
    private Semaphore vuoto  = new Semaphore(1);

    public void set(Object x) throws InterruptedException {
        vuoto.acquire();
        v = x;
        pieno.release();
    }

    public Object get() throws InterruptedException {
        pieno.acquire();
        Object r = v;
        v = null;
        vuoto.release();
        return r;
    }
}

class Msg {
    Object v;
    Requester r;

    public Msg (Object v, Requester r) {
        this.v = v;
        this.r = r;
    }
}

class Requester extends Thread {
    private Counter c;
    private Queue q;
    Buffer result;

    public Requester (Counter c, Queue q) {
        this.c = c;
        this.q = q;
        this.result = new Buffer();
    }

    public void run() {
        try {
            while (true) {
                int v = c.getValue();
                long tempostart = System.currentTimeMillis();
                q.put(new Msg(v,this));
                Object r = result.get();
                System.out.println(getName() + " " + v + ", r:" + r + " " + (System.currentTimeMillis() - tempostart) + "ms");
            }
        } catch (InterruptedException e) {

        }
    }
}

class Worker extends Thread {
    boolean free = true;
    int nUsed = 0; //indica quante volte è stato indicato questo oggetto
    long started;
    Buffer in = new Buffer();
    WorkersManager wm;

    public Worker (WorkersManager wm) {
        this.wm = wm;
    }

    public void run() {
        try {
            while (true) {
                Msg m = (Msg) in.get();
                started = System.currentTimeMillis();
                int x = (int) m.v;
                if (Math.random() < 0.1) {
                    //blocco
                    int c = 0;
                    while (true) {
                        c++; //contatore eterno -> se chiamo interrupt(), questo thread non risponde -> uso stop()
                    }
                } else {
                    m.r.result.set(x*2);
                    wm.releaseWorker(this);
                }
            }
        } catch (InterruptedException e) {

        }
    }
}

class WorkersManager {
    Worker ws[];
    Semaphore mutex = new Semaphore(1);
    Semaphore wrkAvailable;

    public WorkersManager (int nW) {
        ws = new Worker[nW];
        for (int i = 0; i < ws.length; i++) {
            ws[i] = new Worker(this);
            ws[i].start();
        }
        wrkAvailable = new Semaphore(nW);
    }

    public Worker getWorker() throws InterruptedException {
        wrkAvailable.acquire();
        mutex.acquire();
        int minUsed = -1;
        int pmin = 0;
        for (int i = 0; i < ws.length; i++) {
            if (ws[i].free && minUsed == -1 || ws[i].nUsed < minUsed) {
                minUsed = ws[i].nUsed;
                pmin = i;
            }
        }
        ws[pmin].free = false;
        ws[pmin].nUsed++;
        mutex.release();
        return ws[pmin];
    }

    public void releaseWorker(Worker w) {
        w.free = true;
        wrkAvailable.release();
    }
}

class Assigner extends Thread {
        WorkersManager wm;
        Queue q;

        public Assigner (WorkersManager wm, Queue q) {
            this.wm = wm;
            this.q = q;
        }

        public void run() {
            try {
                while (true) {
                    Object m = q.get();
                    Worker w = wm.getWorker();
                    w.in.set(m);
                }
            } catch (InterruptedException e) {

            }
        }
}