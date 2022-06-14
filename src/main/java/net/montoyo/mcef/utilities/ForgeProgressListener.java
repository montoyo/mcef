package net.montoyo.mcef.utilities;


public class ForgeProgressListener implements IProgressListener {

    private int lastVal = 0;

    private void stepUntil(int val) {
        // FIXME: oh this doesn't do anything rlly
        while(lastVal < val) {
            lastVal++;
        }
    }

    @Override
    public void onProgressed(double d) {
        stepUntil((int) Util.clamp(d, 0.d, 100.d));
    }

    @Override
    public void onTaskChanged(String name) {

    }

    @Override
    public void onProgressEnd() {

    }

}
