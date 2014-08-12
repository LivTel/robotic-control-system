package ngat.rcs.iss;

public interface AutoguiderAdjustmentListener {

    public void startingOffset();

    public void endingOffset();

    public void guideReAcquired();

    public void guideNotReAcquired(String message);

}
