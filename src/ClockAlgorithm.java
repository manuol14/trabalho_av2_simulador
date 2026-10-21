import java.util.Arrays;

public class ClockAlgorithm implements PageReplacementAlgorithm{


    @Override
    public int run(int[] pages, int frameCapacity) {
        if (frameCapacity <= 0 || pages == null) return 0;

        int[] frames = new int[frameCapacity];
        boolean[] referenceBits = new boolean[frameCapacity];
        Arrays.fill(frames, -1);

        int pointer = 0;
        int pageFaults = 0;

        for (int page : pages) {
            int pageIndex = findPageIndex(frames, page);

            if (pageIndex != -1) {

                referenceBits[pageIndex] = true;
            } else {

                pageFaults++;


                while (true) {
                    if (frames[pointer] == -1) {

                        frames[pointer] = page;
                        referenceBits[pointer] = true;
                        pointer = (pointer + 1) % frameCapacity;
                        break;
                    }

                    if (referenceBits[pointer]) {

                        referenceBits[pointer] = false;
                        pointer = (pointer + 1) % frameCapacity;
                    } else {

                        frames[pointer] = page;
                        referenceBits[pointer] = true;
                        pointer = (pointer + 1) % frameCapacity;
                        break;
                    }
                }
            }
        }

        return pageFaults;
    }

    private int findPageIndex(int[] frames, int page) {
        for (int i = 0; i < frames.length; i++) {
            if (frames[i] == page) return i;
        }
        return -1;
    }
}
