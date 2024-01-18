/*

Compito 03-02-2020

Si vuole realizzare in Java il seguente sistema:

- sono presenti N thread Producer
- ogni thread Producer iterativamente prende un valore da un contatore condiviso e lo inserisce in una coda illimitata,
  aspetta 10ms e ricomincia
- sono presenti dei thread Consumer che iterativamente prelevano un valore dalla coda e quindi aspettano 20ms
- è presente un thread QueueMonitor che fa partire un thread consumer e quindi ogni secondo controlla la coda:
    - se il numero dei valori in coda supera M*numConsumer fa partire un nuovo thread Consumer, ma solo se non ha
      fatto partire un thread Consumer negli ultimi 5 secondi (senza contare il primo thread che viene fatto partire
      all'inizio)
    - se invece la coda contiene meno di 10 valori negli ultimi 3 controlli e sono attivi almeno due thread Consumer
      allora ne termina uno
- il programma principale attiva il thread QueueMonitor e quindi fa partire gli N thread Procducer aspettando 10 secondi
  tra l'uno e l'altro e quindi dopo 30 secondi fa terminare tutti i thread Producer e quando la coda è vuota termina
  anche il thread QueueMonitor e tutti i Consumer eventualmente sono ancora attivi

Realizzare il sistema usando i semafori per la sincronizzazione tra thread.

*/

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Main {
    public static void main (String[] args) throws InterruptedException {

        Queue q = new Queue();
        SharedCounter sc = new SharedCounter();

        QueueMonitor qm = new QueueMonitor(q);
        qm.start();

        Producer[] p = new Producer[10];
        for (int i = 0; i < p.length; i++) {
            p[i] = new Producer(sc,q);
            p[i].start();
            Thread.sleep(10000);
        }

        Thread.sleep(30000);

        for (Producer pp:p)
            pp.interrupt();

        while (q.size() > 0)
            Thread.sleep(1000);

        qm.interrupt();
    }
}

class SharedCounter {
    private int v = 0;

    //abbiamo bisogno di un semaforo per dare l'accesso a questa risorsa condivisa (problema della Race Condition)
    private Semaphore mutex = new Semaphore(1); //gestisce l'accesso al contatore

    public int getValue () throws InterruptedException {
        mutex.acquire();                //fare solamente: mutex.acquire();
        int r = v;                      //                v++;
        v++;                            //                mutex.release();
        mutex.release();                //risolve il problema della race condition, però, ritornando v (return v;), il produttore
                                        //potrebbe prendere v con un valore alterato -> uso una variabile locale
        return r;                       //e quindi ritorno r, non v
    }
    //quindi ogni Produttore che chiamerà getValue() otterrà un valore diverso
}

class Queue {
    private ArrayList data = new ArrayList();
    private Semaphore mutex = new Semaphore(1); //gestisce l'accesso alla coda
    private Semaphore pieni = new Semaphore(0); //inizializzato a 0 -> ci basta solo un semaforo perché la coda è illimitata

    public void put (Object o) throws InterruptedException {
        //va sincronizzato l'accesso a data perché non è thread-safe
        mutex.acquire();
        data.add(o);
        mutex.release();
        pieni.release(); //incremento il numero di pieni
    }

    public Object get () throws InterruptedException {
        pieni.acquire(); //per acquisire il valore dal semaforo //diminuisco il numero di pieni
        mutex.acquire();
        Object r = data.remove(0);
        mutex.release();
        return r;
    }

    public int size () throws InterruptedException {
        mutex.acquire();
        int s = data.size();
        mutex.release();
        return s;
    }
}

class Producer extends Thread {
    private SharedCounter sc;
    private Queue q;

    public Producer (SharedCounter sc, Queue q) {
        this.sc = sc;
        this.q = q;
    }

    public void run() {
        try {
            while (true) {
                int v = sc.getValue();
                q.put(v);
                sleep(10);
            }
        } catch (InterruptedException e) {
            System.out.println("Producer " + getName() + " terminato");
        }
    }
}

class Consumer extends Thread {
    private Queue q;

    public Consumer (Queue q) { this.q = q; }

    public void run() {
        try {
            System.out.println("Consumer " + getName() + " partito");
            while (true) {
                Object o = q.get();
                sleep(20);
            }
        } catch (InterruptedException e) {
            System.out.println("Consumer " + getName() + " terminato");
        }
    }
}

class QueueMonitor extends Thread {
    private Queue q;
    private ArrayList<Consumer> cc = new ArrayList<>(); //tengo una lista dei Consumer

    public QueueMonitor (Queue q) { this.q = q; } //ha bisogno solo della coda -> perché?

    public void run() {
        try {

            int M = 100;
            Consumer c = new Consumer(q);
            c.start();
            cc.add(c);

            long lastStarted = 0; //tengo il valore del tempo al quale è stato fatto partire l'ultimo Consumer
            int nLow = 0; //variabile contatore

            while (true) {
                //deve guardare ogni secondo lo stato della coda
                int s = q.size();
                System.out.println("Queue size: " + s);
                if (s > (M * cc.size())) {
                    if (lastStarted == 0 || (System.currentTimeMillis() - lastStarted) > 5000) {
                        c = new Consumer(q);
                        c.start();
                        cc.add(c);
                        lastStarted = System.currentTimeMillis(); //aggiorno lastStarted
                    }
                    if (s < 10) {
                        nLow++;
                        if (nLow == 3 && cc.size() >= 2) { //vuol dire che è stato per 3 volte minore di 10
                            c = cc.remove(0);
                            c.interrupt();
                            nLow = 0; //ripristino la situazione
                        }
                    } else { nLow = 0; } //garantiso che nLow viene incrementato per 3 volte solo se s < 10 per 3 controlli di fila
                }
                sleep(1000);
            }
        } catch (InterruptedException e) {
            System.out.println("QueueMonitor terminato");
            for (Consumer c:cc) { //itero su tutti i Consumer
                c.interrupt();
            }
        }
    }
}