import java.util.ArrayList;
import java.util.List;

public class LruAlgorithm implements PageReplacementAlgorithm{
    @Override
    public int run(int[] pages, int frameCapacity) {
        if (frameCapacity <= 0 || pages == null) return 0;

        List<Integer> memory = new ArrayList<>(frameCapacity);
        int pageFaults = 0;

        for (int page : pages) {
            if (memory.contains(page)) {

                memory.remove((Integer) page);
                memory.add(page);
            } else {

                pageFaults++;

                if (memory.size() == frameCapacity) {

                    memory.remove(0);
                }
                memory.add(page);
            }
        }

        return pageFaults;
    }
}
