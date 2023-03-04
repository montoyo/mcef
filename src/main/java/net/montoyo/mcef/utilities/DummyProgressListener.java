package net.montoyo.mcef.utilities;

/**
 * Dummy progress listener. Does nothing.
 * @author montoyo
 * @see Util2#secure(IProgressListener)
 *
 */
public class DummyProgressListener implements IProgressListener {

    @Override
    public void onProgressed(double d) {
    }

    @Override
    public void onTaskChanged(String name) {
    }

    @Override
    public void onProgressEnd() {
    }

}
