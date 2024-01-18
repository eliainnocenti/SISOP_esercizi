import java.util.ArrayList;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int N = 1000; //numero array iniziali (== valori da ordinare)
        int M = 4; //numero mergethread

        MergeQueue mq = new MergeQueue(N);

        for (int i = 0; i < N; i++) {
            int value = (int) (Math.random() * 100);
            mq.insert(new int[]{value});
        }

        /*
        System.out.print("MergeQueue: [ ");
        for (int i = 0; i < mq.data.size(); i++) {
            System.out.print("[ ");
            for (int j = 0; i < mq.data.get(i).length; i++) {
                System.out.print(mq.data.get(i)[j] + " ");
            }
            System.out.print("] ");
        }
        System.out.print("]");
        System.out.println();
        */

        MergeThread[] mt = new MergeThread[M];

        for (int i = 0; i < mt.length; i++) {
            mt[i] = new MergeThread(mq);
            mt[i].setName("MT" + i);
            mt[i].start();
        }

        int[] finalArray = mq.getFinalArray();
        System.out.print("MergeQueue: [ ");
        for (int i = 0; i < N; i++)
            System.out.print(finalArray[i] + " ");
        System.out.println("]");
        for (MergeThread mmt:mt)
            mmt.interrupt();

    }
}
class MergeQueue {
    ArrayList<int[]> data = new ArrayList();
    private int N;
    public MergeQueue(int N) {
        this.N = N;
    }
    public synchronized void insert(int[] newArray) {
        data.add(newArray);
        notifyAll();
    }
    public synchronized int[][] get2() throws InterruptedException {
        while (data.size() < 2)
            wait();
        int[][] r = new int[2][];
        r[0] = data.remove(0);
        r[1] = data.remove(0);
        return r;
    }
    public synchronized int[] getFinalArray() throws InterruptedException {
        while (data.get(0).length != N)
            wait();
        return data.get(0);
    }
}
class MergeThread extends Thread {
    MergeQueue mq;
    public MergeThread(MergeQueue mq) {
        this.mq = mq;
    }

    public void run() {
        try {
            while (true) {
                int[][] r = mq.get2();
                int[] newArray = merge(r);
                mq.insert(newArray);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrotto");
        }
    }

    private int[] merge(int[][] r) {
        int[] array1 = r[0];
        int[] array2 = r[1];
        int[] mergedArray = new int[array1.length + array2.length];

        int i = 0, j = 0, k = 0;
        while (i < array1.length && j < array2.length) {
            if (array1[i] <= array2[j]) {
                mergedArray[k] = array1[i];
                i++;
            } else {
                mergedArray[k] = array2[j];
                j++;
            }
            k++;
        }

        while (i < array1.length) {
            mergedArray[k] = array1[i];
            i++;
            k++;
        }

        while (j < array2.length) {
            mergedArray[k] = array2[j];
            j++;
            k++;
        }

        return mergedArray;
    }
}