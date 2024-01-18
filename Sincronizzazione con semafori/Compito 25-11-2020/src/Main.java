/*

Un sistema deve gestire delle immagini di disco dove ognuna è fatta da N Layer.
Ogni Layer è rappresentato da un file zip che deve essere scaricato da un repository e devono essere
estratti (unzip) in sequenza. Sono presenti K DownloadThread che iterativamente acquisiscono da una coda
i Layer da scaricare ed impiegano un tempo proporzionale alla loro dimensione. I Layer scaricati devono essere
messi in una seconda coda dalla quale un ExtractorThread preleva i Layer e li estrae facendo in modo di estrarre
tutti i Layer in sequenza. Il thread deve attendere se il layer da estrarre ancora non è stato scaricato.
Il programma principale deve creare e far partire i thread quindi generare gli N layer ognuno di dimensioni
casuali (1-100) e quindi deve attendere che tutti i layer vengano acquisiti ed estratti e quindi termini tutti i thread.
I DownloadThread e l’ExtractorThead devono simulare le operazioni con uno sleep proporzionale alla dimensione del file,
size*100ms il download e size*50ms l’estrazione e al termine stampare il numero del layer e l’operazione completata.
Realizzare in Java il sistema descritto usando i semafori per la sincronizzazione tra thread (non usare attesa attiva).

*/

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Main {
    public static void main (String[] args) throws InterruptedException {
        int N = 10;
        Queue q1 = new Queue();
        Queue q2 = new Queue();

        for (int i = 0; i < N; i++) {
            q1.putLayer(new Layer(i));
        }

        int K = 3;
        DownloadThread[] dt = new DownloadThread[K];

        for (int i = 0; i < dt.length; i++) {
            dt[i] = new DownloadThread(q1,q2);
            dt[i].setName("DT" + i);
            dt[i].start();
        }

        ExtractorThread et = new ExtractorThread(q2, N);
        et.start();

        et.join();

        for (DownloadThread DT:dt)
            DT.interrupt();


        System.out.println("Operazione completata");
    }
}


class Layer {
    Zip z;
    private int n;
    private int size;

    public Layer(int n) {
        this.n = n;
        this.size = (int) (Math.random()*100);
    }
    public int getSize() {
        return size;
    }
    public int getN() {
        return n;
    }
}

class Zip {

}

class DownloadThread extends Thread {
    private Queue q1;
    private Queue q2;

    public DownloadThread (Queue q1, Queue q2) {
        this.q1 = q1;
        this.q2 = q2;
    }

    public void run() {
        try {
            while (true) {
                Layer l = q1.getLayer();
                System.out.println(getName() + " starts downloading Layer " + l.getN());
                sleep(l.getSize()*100);
                System.out.println(getName() + " downloaded Layer " + l.getN());
                q2.putLayer(l);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }
}

class ExtractorThread extends Thread {
    private Queue q2;
    private int N;

    public ExtractorThread (Queue q2, int N) {
        this.q2 = q2;
        this.N = N;
    }

    public void run() {
        try {
            for (int i = 0; i < N; i++) {
                Layer l = q2.getLayer();
                sleep(l.getSize()*50);
                System.out.println("Layer " + l.getN() + " estratto" + ", size: " + l.getSize());
            }
        } catch (InterruptedException e) {
            System.out.println("Extractor Thread interrotto");
        }
    }
}

class Queue {
    private ArrayList<Layer> data = new ArrayList<>();
    private Semaphore mutex = new Semaphore(1);
    private Semaphore piene = new Semaphore(0);

    public void putLayer(Layer x) throws InterruptedException {
        mutex.acquire();
        data.add(x);
        mutex.release();
        piene.release();
    }

    public Layer getLayer() throws InterruptedException {
        piene.acquire();
        mutex.acquire();
        Layer r = data.remove(0);
        mutex.release();
        return r;
    }

    public int getSize() throws InterruptedException {
        mutex.acquire();
        int size = data.size();
        mutex.release();
        return size;
    }

}