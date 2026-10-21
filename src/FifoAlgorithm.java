import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class FifoAlgorithm implements PageReplacementAlgorithm{


    @Override
    public int run(int[] pages, int frameCapacity) {
        if (frameCapacity <= 0 || pages == null) return 0;

        Set<Integer> memory = new HashSet<>();
        Queue<Integer> orderQueue = new ArrayDeque<>();
        int pageFaults = 0;

        for (int page : pages) {

            if (!memory.contains(page)) {

                pageFaults++;

                if (memory.size() == frameCapacity) {
                    int oldestPage = orderQueue.poll();
                    memory.remove(oldestPage);
                }

                memory.add(page);
                orderQueue.offer(page);
            }
        }

        return pageFaults;
    }
}
