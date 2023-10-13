package au.org.ala.images;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * Hi res timer
 *
 */
public class CodeTimer {

    Logger logger = Logger.getLogger("CodeTimer");

    private static String EOL = System.getProperty("line.separator");

    /** description */
    private String _description;
    /** keeps a running tally of elapsed times */
    private List<Long> _times;

    private long _startCounter;

    private long _endCounter;

    private long _frequency;

    /**
     * ctor Starts the timer
     *
     * @param description
     *            a description of this timer
     */
    public CodeTimer(String description) {
        _description = description;
        _times = new ArrayList<Long>();
        _startCounter = System.nanoTime();
        _frequency = 1000000000;
    }

    /** start/restart the timer */
    public void start() {
        _startCounter = System.nanoTime();
    }

    /** restart the timer and clear the running tally */
    public void reset() {
        _times = new ArrayList<Long>();
        _startCounter = System.nanoTime();
    }

    /**
     * Stop the timer
     */
    public void stop() {
        stop(false, false, null);
    }

    /**
     * Stop the timer. optionally print the elapsed time to standard out
     *
     * @param printelapsed
     *            if true a message containing the elapsed time will be printed to standard out.
     */
    public void stop(boolean printelapsed) {
        stop(printelapsed, false, null);
    }

    /**
     * @param printelapsed
     * @param asdouble
     */
    public void stop(boolean printelapsed, boolean asdouble) {
        stop(printelapsed, asdouble, null);
    }

    /**
     * @param printelapsed
     * @param asdouble
     * @param auxmsg
     */
    public void stop(boolean printelapsed, boolean asdouble, String auxmsg) {
        _endCounter = System.nanoTime();
        String msg = null;
        if (printelapsed) {
            if (asdouble) {
                msg = String.format("%s: %f ms%s", _description, getElapsedMillisDouble(), (auxmsg == null ? "" : " (" + auxmsg + ")"));
            } else {
                msg = String.format("%s: %d ms%s", _description, getElapsedMillis(), (auxmsg == null ? "" : " (" + auxmsg + ")"));
            }
            if (msg != null && logger != null) {
                writeln(msg);
            }
        }
        _times.add(getElapsedMillis());
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return _description;
    }

    /**
     * @return the number of elapsed milliseconds since start and stop (or now if stop hasn't been called)
     */
    public long getElapsedMillis() {
        double elapsed = ((double) (_endCounter - _startCounter) / (double) _frequency) * 1000;
        return (long) elapsed;
    }

    public double getElapsedMillisDouble() {
        return ((double) (_endCounter - _startCounter) / (double) _frequency) * 1000;
    }

    /**
     * @return the average
     */
    public long getAverage() {
        return getAverage(false);
    }

    private void writeln(String msg) {
        logger.fine(msg);
    }

    /**
     * @param dump
     *            if true will print a message to std out
     * @return the average of all elapsed times recorded in the running tally
     */
    public long getAverage(boolean dump) {
        long total = 0;
        for (Long l : _times) {
            total += l;
        }
        long avg = (_times.size() == 0 ? 0 : total / _times.size());
        if (dump && logger != null) {
            writeln("Average : " + avg);
        }
        return avg;
    }

    /**
     * @return the running tally of elapsed times
     */
    public List<Long> getElapsedTimes() {
        return _times;
    }

}