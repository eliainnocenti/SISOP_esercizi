public class Main {
    public static void main(String[] args) throws InterruptedException {
        int N = 100; //numero studenti
        int M = 2; //numero aule
        int K = 5; //capienza aula
        int T = 20; //durata lezione

        long ts = System.currentTimeMillis();

        Tornello tt[] = new Tornello[4];
        for (int i = 0; i < tt.length; i++) {
            tt[i] = new Tornello();
        }
        Aula[] aa = new Aula[M];
        Lezione[] ll = new Lezione[M];
        for (int i = 0; i < aa.length; i++) {
            aa[i] = new Aula(K);
            ll[i] = new Lezione(aa[i], T);
            ll[i].setName("L" + i);
            ll[i].start();
        }
        Studente[] ss = new Studente[N];
        for (int i = 0; i < ss.length; i++) {
            ss[i] = new Studente(tt, aa);
            ss[i].setName("S" + i);
            ss[i].start();
        }

        while (Studente.aCasa < ss.length) {
            int t = Studente.aCasa;
            System.out.print("acasa:" + Studente.aCasa + " ai tornelli: ");
            for (int i = 0; i < tt.length; i++) {
                System.out.print(" " + tt[i].waiting);
                t += tt[i].waiting;
            }
            System.out.println();
            for (int i = 0; i < aa.length; i++) {
                System.out.println("  aula" + i + " -> " + aa[i]);
                t += aa[i].waiting + aa[i].n;
            }
            System.out.println("NS:" + t);
            Thread.sleep(100);
        }

        for (int i = 0; i < ss.length; i++) {
            ss[i].interrupt();
        }
        for (int i = 0; i < aa.length; i++) {
            ll[i].interrupt();
        }

        long tf = System.currentTimeMillis();

        System.out.println("Tempo di esecuzione: " + (tf-ts) + "ms");
    }

}

class Studente extends Thread {
    static int aCasa = 0;
    Tornello[] t;
    Aula[] a;

    public Studente(Tornello[] t, Aula[] a) {
        this.t = t;
        this.a = a;
    }

    public void run() {
        try {

            //cerca il tornello con meno studenti in attesa
            int tr = 0;
            for (int i = 1; i < t.length; i++) {
                if (t[i].waiting < t[tr].waiting) {
                    tr = i;
                }
            }

            t[tr].request();
            sleep(1000);
            t[tr].exit();

            Aula au = a[(int) (Math.random() * a.length)];
            au.enter();
            //System.out.println(getName()+" in aula "+au.getName());
            au.waitLessonEnd();
            //System.out.println(getName()+" lezione in aula "+au.getName());
            au.exit();
            //System.out.println(getName()+" finito");
        } catch (InterruptedException e) {

        }

        synchronized (Studente.class) {
            aCasa++;
        }
    }
}

class Tornello {

    int waiting = 0;
    boolean locked = false;

    public synchronized void request() throws InterruptedException {
        while (locked) {
            waiting++;
            wait();
            waiting--;
        }
        locked = true;
    }

    public synchronized void exit() {
        locked = false;
        notify();
    }
}

class Aula {

    int n;
    int max;
    int waiting;

    final static int WAIT_LESSON_START = 0;
    final static int LESSON = 1;
    final static int LESSON_END = 2;

    int state = WAIT_LESSON_START;

    public Aula(int max) {
        this.max = max;
    }

    public synchronized void enter() throws InterruptedException {
        //studente aspetta se aula piena o lezione finita
        while (n == max || state==LESSON_END) {
            waiting++;
            wait();
            waiting--;
        }

        n++;

        if (state == WAIT_LESSON_START && n == 1) {
            state = LESSON;
        }
        notifyAll();
    }

    public synchronized void waitLessonEnd() throws InterruptedException {
        while (state != LESSON_END) {
            //System.out.println(Thread.currentThread().getName()+" wait lesson end");
            wait();
        }
    }

    public synchronized void endLesson() {
        state = LESSON_END;
        notifyAll();
    }

    public synchronized void waitLessonStart() throws InterruptedException {
        while (state != LESSON) {
            wait();
        }
    }

    public synchronized void exit() {
        n--;
        if (n == 0) {
            state = WAIT_LESSON_START;
        }
        notifyAll();
    }

    @Override
    public String toString() {
        return "wait: " + waiting + " in:" + n + " lesson:" + state;
    }
}

class Lezione extends Thread {

    Aula a;
    int T;

    public Lezione(Aula a, int T) {
        this.a = a;
        this.T = T;
    }

    @Override
    public void run() {
        try {
            while (true) {
                //aspetta che ci sia uno studente in aula
                //System.out.println(getName()+" attesa studenti");
                a.waitLessonStart();
                //System.out.println(getName()+" inizio lezione");

                sleep(T*1000);

                //segnala fine lezione
                a.endLesson();

                //System.out.println(getName()+" fine lezione");
            }
        } catch (InterruptedException e) {

        }
    }
}
