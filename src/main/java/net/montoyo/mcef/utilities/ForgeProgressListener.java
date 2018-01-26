package net.montoyo.mcef.utilities;

import net.minecraftforge.fml.common.ProgressManager;

public class ForgeProgressListener implements IProgressListener {

    private ProgressManager.ProgressBar progressBar = null;
    private int lastVal = 0;

    private void stepUntil(int val) {
        //FIXME: Bad, disgusting, and everything...
        while(lastVal < val) {
            progressBar.step("" + val + "%");
            lastVal++;
        }
    }

    @Override
    public void onProgressed(double d) {
        stepUntil((int) Util.clamp(d, 0.d, 100.d));
    }

    @Override
    public void onTaskChanged(String name) {
        if(progressBar != null) {
            stepUntil(100);
            ProgressManager.pop(progressBar);
        }

        progressBar = ProgressManager.push(name, 100, false);
        lastVal = 0;
    }

    @Override
    public void onProgressEnd() {
        if(progressBar != null) {
            stepUntil(100);
            ProgressManager.pop(progressBar);
            progressBar = null;
        }
    }

}
