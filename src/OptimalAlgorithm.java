import java.util.ArrayList;
import java.util.List;

public class OptimalAlgorithm implements PageReplacementAlgorithm{
    @Override
    public int run(int[] pages, int frameCapacity) {
        if (frameCapacity <= 0 || pages == null) return 0;

        List<Integer> memory = new ArrayList<>(frameCapacity);
        int pageFaults = 0;

        for (int i = 0; i < pages.length; i++) {
            int currentPage = pages[i];

            if (!memory.contains(currentPage)) {
                pageFaults++;

                if (memory.size() < frameCapacity) {
                    memory.add(currentPage);
                } else {
                    int victimIndex = selectOptimalVictim(memory, pages, i + 1);
                    memory.set(victimIndex, currentPage);
                }
            }
        }

        return pageFaults;
    }

    private int selectOptimalVictim(List<Integer> memory, int[] pages, int startIndex) {
        int farthestIndex = -1;
        int victimIndex = 0;

        for (int m = 0; m < memory.size(); m++) {
            int pageInMemory = memory.get(m);
            int nextUse = Integer.MAX_VALUE;

            for (int j = startIndex; j < pages.length; j++) {
                if (pages[j] == pageInMemory) {
                    nextUse = j;
                    break;
                }
            }


            if (nextUse == Integer.MAX_VALUE) {
                return m;
            }


            if (nextUse > farthestIndex) {
                farthestIndex = nextUse;
                victimIndex = m;
            }
        }

        return victimIndex;
    }
}
