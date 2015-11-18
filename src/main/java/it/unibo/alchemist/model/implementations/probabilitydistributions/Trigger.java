/**
 * 
 */
package it.unibo.alchemist.model.implementations.probabilitydistributions;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.ITime;

/**
 * @param <T>
 *            Concentration type
 */
public class Trigger<T> extends AbstractDistribution<T> {

    private static final long serialVersionUID = 5207992119302525618L;
    private boolean dryRunDone;

    /**
     * @param event
     *            the time at which the event will happen
     */
    public Trigger(final ITime event) {
        super(event);
    }

    @Override
    public double getRate() {
        return Double.NaN;
    }

    @Override
    protected void updateStatus(final ITime curTime, final boolean executed, final double param, final IEnvironment<T> env) {
        if (dryRunDone && curTime.compareTo(getNextOccurence()) >= 0 && executed) {
            setTau(new DoubleTime(Double.POSITIVE_INFINITY));
        }
        dryRunDone = true;
    }

    @Override
    @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
    public Trigger<T> clone() {
        return new Trigger<>(getNextOccurence());
    }

}
