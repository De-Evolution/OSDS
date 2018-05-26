/*
 * Copyright (c) 2014, 2015 Qualcomm Technologies Inc
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * (subject to the limitations in the disclaimer below) provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Qualcomm Technologies Inc nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS LICENSE. THIS
 * SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.qualcomm.robotcore.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.robocol.Heartbeat;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.files.LogOutputStream;

import java.io.File;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Allows consistent logging across all RobotCore packages
 */
@SuppressWarnings("WeakerAccess")
public class RobotLog {

    /*
     * Currently only supports android style logging, but may support more in the future.
     */

    /*
     * Only contains static utility methods
     */
    private RobotLog() {
    }

    //------------------------------------------------------------------------------------------------
    // State
    //------------------------------------------------------------------------------------------------

    public static final String ERROR_PREPEND = "### ERROR: ";

    private static String globalErrorMessage = "";
    private static final Object globalWarningLock = new Object();
    private static String globalWarningMessage = "";
    private static WeakHashMap<GlobalWarningSource, Integer> globalWarningSources = new WeakHashMap<GlobalWarningSource, Integer>();
    private static double msTimeOffset = 0.0;

    public static final String TAG = "RobotCore";

    private static Thread loggingThread = null;

    public static String logcatCommand = "logcat";
    public static int kbLogcatQuantum = 4 * 1024;
    public static int logcatRotatedLogsMax = 4;
    public static String logcatFormat = "threadtime";
    public static String logcatFilter = "UsbRequestJNI:S UsbRequest:S *:V";

    //------------------------------------------------------------------------------------------------
    // Time Synchronization
    //------------------------------------------------------------------------------------------------

    /**
     * Processes the reception of a set of NTP timestamps between this device (t0 and t3) and
     * a remote device (t1 and t2) with whom it is trying to synchronize time. Our current implementation
     * is very, very crude: we just calculate the instantaneous offset, and remember. But that's probably
     * good enough for trying to coordinate timestamps in logs.
     */
    public static void processTimeSynch(long t0, long t1, long t2, long t3) {
        if (t0 == 0 || t1 == 0 || t2 == 0 || t3 == 0) {
            return; // invalid packet data
        }

        // https://en.wikipedia.org/wiki/Network_Time_Protocol
        // offset is how much the time source is ahead of us (ie: not behind)
        double offset = ((t1 - t0) + (t2 - t3)) / 2.0;
        setMsTimeOffset(offset);
    }

    // Records the time difference between this device and a device with whom we are synchronizing our time
    public static void setMsTimeOffset(double offset) {
        msTimeOffset = offset;
    }

    //------------------------------------------------------------------------------------------------
    // Logging API
    //------------------------------------------------------------------------------------------------

    public static void a(String format, Object... args) {
        v(String.format(format, args));
    }

    public static void a(String message) {
        internalLog(Log.ASSERT, TAG, message);
    }

    public static void aa(String tag, String format, Object... args) {
        vv(tag, String.format(format, args));
    }

    public static void aa(String tag, String message) {
        internalLog(Log.ASSERT, tag, message);
    }

    public static void aa(String tag, Throwable throwable, String format, Object... args) {
        vv(tag, throwable, String.format(format, args));
    }

    public static void aa(String tag, Throwable throwable, String message) {
        internalLog(Log.ASSERT, tag, throwable, message);
    }

    public static void v(String format, Object... args) {
        v(String.format(format, args));
    }

    public static void v(String message) {
        internalLog(Log.VERBOSE, TAG, message);
    }

    public static void vv(String tag, String format, Object... args) {
        vv(tag, String.format(format, args));
    }

    public static void vv(String tag, String message) {
        internalLog(Log.VERBOSE, tag, message);
    }

    public static void vv(String tag, Throwable throwable, String format, Object... args) {
        vv(tag, throwable, String.format(format, args));
    }

    public static void vv(String tag, Throwable throwable, String message) {
        internalLog(Log.VERBOSE, tag, throwable, message);
    }

    public static void d(String format, Object... args) {
        d(String.format(format, args));
    }

    public static void d(String message) {
        internalLog(Log.DEBUG, TAG, message);
    }

    public static void dd(String tag, String format, Object... args) {
        dd(tag, String.format(format, args));
    }

    public static void dd(String tag, String message) {
        internalLog(Log.DEBUG, tag, message);
    }

    public static void dd(String tag, Throwable throwable, String format, Object... args) {
        dd(tag, throwable, String.format(format, args));
    }

    public static void dd(String tag, Throwable throwable, String message) {
        internalLog(Log.DEBUG, tag, throwable, message);
    }

    public static void i(String format, Object... args) {
        i(String.format(format, args));
    }

    public static void i(String message) {
        internalLog(Log.INFO, TAG, message);
    }

    public static void ii(String tag, String format, Object... args) {
        ii(tag, String.format(format, args));
    }

    public static void ii(String tag, String message) {
        internalLog(Log.INFO, tag, message);
    }

    public static void ii(String tag, Throwable throwable, String format, Object... args) {
        ii(tag, throwable, String.format(format, args));
    }

    public static void ii(String tag, Throwable throwable, String message) {
        internalLog(Log.INFO, tag, throwable, message);
    }

    public static void w(String format, Object... args) {
        w(String.format(format, args));
    }

    public static void w(String message) {
        internalLog(Log.WARN, TAG, message);
    }

    public static void ww(String tag, String format, Object... args) {
        ww(tag, String.format(format, args));
    }

    public static void ww(String tag, String message) {
        internalLog(Log.WARN, tag, message);
    }

    public static void ww(String tag, Throwable throwable, String format, Object... args) {
        ww(tag, throwable, String.format(format, args));
    }

    public static void ww(String tag, Throwable throwable, String message) {
        internalLog(Log.WARN, tag, throwable, message);
    }

    public static void e(String format, Object... args) {
        e(String.format(format, args));
    }

    public static void e(String message) {
        internalLog(Log.ERROR, TAG, message);
    }

    public static void ee(String tag, String format, Object... args) {
        ee(tag, String.format(format, args));
    }

    public static void ee(String tag, String message) {
        internalLog(Log.ERROR, tag, message);
    }

    public static void ee(String tag, Throwable throwable, String format, Object... args) {
        ee(tag, throwable, String.format(format, args));
    }

    public static void ee(String tag, Throwable throwable, String message) {
        internalLog(Log.ERROR, tag, throwable, message);
    }

    public static void internalLog(int priority, String tag, String message) {
        if (msTimeOffset == 0) {
            android.util.Log.println(priority, tag, message);
        } else {
            double offset = msTimeOffset;
            long now = (long) (Heartbeat.getMsTimeSyncTime() + offset + 0.5);
            GregorianCalendar tNow = new GregorianCalendar();
            tNow.setTimeInMillis(now);
            android.util.Log.println(priority, tag, String.format("{%5d %2d.%03d} %s", (int) (msTimeOffset + 0.5), tNow.get(GregorianCalendar.SECOND), tNow.get(GregorianCalendar.MILLISECOND), message));
        }
    }

    public static void internalLog(int priority, String tag, Throwable throwable, String message) {
        internalLog(priority, tag, message);
        logStackTrace(tag, throwable);
    }

    public static void logExceptionHeader(Exception e, String format, Object... args) {
        String message = String.format(format, args);
        RobotLog.e("exception %s(%s): %s [%s]", e.getClass().getSimpleName(), e.getMessage(), message, getStackTop(e));
    }

    public static void logExceptionHeader(String tag, Exception e, String format, Object... args) {
        String message = String.format(format, args);
        RobotLog.ee(tag, "exception %s(%s): %s [%s]", e.getClass().getSimpleName(), e.getMessage(), message, getStackTop(e));
    }

    private static StackTraceElement getStackTop(Exception e) {
        StackTraceElement[] frames = e.getStackTrace();
        return frames.length > 0 ? frames[0] : null;
    }

    /**
     * @deprecated obsolete capitalization
     */
    @Deprecated
    public static void logStacktrace(Throwable e) {
        logStackTrace(e);
    }

    public static void logStackTrace(Throwable e) {
        logStackTrace(TAG, e);
    }

    public static void logStackTrace(Thread thread, String format, Object... args) {
        String message = String.format(format, args);
        RobotLog.e("thread id=%d tid=%d name=\"%s\" %s", thread.getId(), ThreadPool.getTID(thread), thread.getName(), message);
        logStackFrames(thread.getStackTrace());
    }

    public static void logStackTrace(Thread thread, StackTraceElement[] stackTrace) {
        RobotLog.e("thread id=%d tid=%d name=\"%s\"", thread.getId(), ThreadPool.getTID(thread), thread.getName());
        logStackFrames(stackTrace);
    }

    public static void logStackTrace(String tag, Throwable e) {
        e.printStackTrace(LogOutputStream.printStream(tag));
    }

    private static void logStackFrames(StackTraceElement[] stackTrace) {
        for (StackTraceElement frame : stackTrace) {
            RobotLog.e("    at %s", frame.toString());
        }
    }

    public static void logAndThrow(String errMsg) throws RobotCoreException {
        w(errMsg);
        throw new RobotCoreException(errMsg);
    }

    //------------------------------------------------------------------------------------------------
    // Error and Warning Messages
    //------------------------------------------------------------------------------------------------

    /**
     * Set a global error message
     * <p>
     * This message stays set until clearGlobalErrorMsg is called. Additional
     * calls to set the global error message will be silently ignored until the
     * current error message is cleared.
     * <p>
     * This is so that if multiple global error messages are raised, the first
     * error message is captured.
     * <p>
     * Presently, the global error is cleared only when the robot is restarted.
     *
     * @param message error message
     */
    public static boolean setGlobalErrorMsg(String message) {
        // don't allow a new error message to overwrite the old error message
        if (globalErrorMessage.isEmpty()) {
            globalErrorMessage += message; // using += to force a null pointer exception if message is null
            return true;
        }
        return false;
    }

    public static void setGlobalErrorMsg(String format, Object... args) {
        setGlobalErrorMsg(String.format(format, args));
    }


    /**
     * Sets the global warning message.
     * <p>
     * This stays set until clearGlobalWarningMsg is called.
     *
     * @param message the warning message to set
     */
    public static void setGlobalWarningMessage(String message) {
        synchronized (globalWarningLock) {
            if (globalWarningMessage.isEmpty()) {
                globalWarningMessage += message;
            }
        }
    }

    public static void setGlobalWarningMessage(String format, Object... args) {
        setGlobalWarningMessage(String.format(format, args));
    }

    /**
     * Adds (if not already present) a new source that can contribute the generation of warnings
     * on the robot controller and driver station displays (if the source is already registered,
     * the call has no effect). The source will periodically be polled for its contribution to
     * the overall warning message; if the source has no warning to contribute, it should return
     * an empty string. Note that weak references are used in this registration: the act of adding
     * a global warning source will not of its own accord keep that source from being reclaimed by
     * the system garbage collector.
     *
     * @param globalWarningSource the warning source to add
     */
    public static void registerGlobalWarningSource(GlobalWarningSource globalWarningSource) {
        synchronized (globalWarningLock) {
            globalWarningSources.put(globalWarningSource, 1);
        }
    }

    /**
     * Removes (if present) a source from the list of warning sources contributing to the
     * overall system warning message (if the indicated source is not currently registered, this
     * call has no effect). Note that explicit unregistration of a warning source is not required:
     * due to the internal use of weak references, warning sources will be automatically
     * unregistered if they are reclaimed by the garbage collector. However, explicit unregistration
     * may be useful to the source itself so that it will stop being polled for its warning
     * contribution.
     *
     * @param globalWarningSource the source to remove as a global warning source
     */
    public static void unregisterGlobalWarningSource(GlobalWarningSource globalWarningSource) {
        synchronized (globalWarningLock) {
            globalWarningSources.remove(globalWarningSource);
        }
    }

    public static void setGlobalWarningMsg(RobotCoreException e, String message) {
        setGlobalWarningMessage(message + ": " + e.getMessage());
    }

    public static void setGlobalErrorMsg(RobotCoreException e, String message) {
        setGlobalErrorMsg(message + ": " + e.getMessage());
    }

    public static void setGlobalErrorMsgAndThrow(RobotCoreException e, String message) throws RobotCoreException {
        setGlobalErrorMsg(e, message);
        throw e;
    }

    public static void setGlobalErrorMsg(RuntimeException e, String message) {
        setGlobalErrorMsg(String.format("%s: %s: %s", message, e.getClass().getSimpleName(), e.getMessage()));
    }

    public static void setGlobalErrorMsgAndThrow(RuntimeException e, String message) throws RobotCoreException {
        setGlobalErrorMsg(e, message);
        throw e;
    }

    /**
     * Get the current global error message
     *
     * @return error message
     */
    public static String getGlobalErrorMsg() {
        return globalErrorMessage;
    }

    /**
     * Returns the current global warning, or "" if there is none
     *
     * @return the current global warning
     */
    public static String getGlobalWarningMessage() {
        List<String> warnings = new ArrayList<String>();
        synchronized (globalWarningLock) {
            warnings.add(globalWarningMessage);
            for (GlobalWarningSource source : globalWarningSources.keySet()) {
                warnings.add(source.getGlobalWarning());
            }
        }
        return combineGlobalWarnings(warnings);
    }

    /**
     * Combines possibly multiple warnings together using an appropriate delimiter. If
     * there is no actual warning in effect, then "" is returned.
     */
    public static String combineGlobalWarnings(List<String> warnings) {
        StringBuilder result = new StringBuilder();
        for (String warning : warnings) {
            if (warning != null && !warning.isEmpty()) {
                if (result.length() > 0) {
                    result.append("; ");
                }
                result.append(warning);
            }
        }
        return result.toString();
    }

    /**
     * Returns true if a global error message is set
     *
     * @return true if there is an error message
     */
    public static boolean hasGlobalErrorMsg() {
        return !getGlobalErrorMsg().isEmpty();
    }

    /**
     * Returns whether a global warning currently exists
     *
     * @return whether a global warning currently exists
     */
    public static boolean hasGlobalWarningMsg() {
        return !getGlobalWarningMessage().isEmpty();
    }

    /**
     * Clears the current global error message.
     */
    public static void clearGlobalErrorMsg() {
        globalErrorMessage = "";
    }

    /**
     * Clears the current global warning message.
     */
    public static void clearGlobalWarningMsg() {
        synchronized (globalWarningLock) {
            globalWarningMessage = "";
        }
    }

    //------------------------------------------------------------------------------------------------
    // Disk Management
    //------------------------------------------------------------------------------------------------

    /**
     * Write logcat logs to disk. Log data will continue to be written to disk until
     * {@link #cancelWriteLogcatToDisk()} is called. {@link #onApplicationStart()} is
     * idempotent: additional calls to this method will be a NOOP.
     */
    public static void onApplicationStart() {
        // Diskify the log in quanta (all but the last of these will get GZIP'd)
        writeLogcatToDisk(AppUtil.getDefContext(), kbLogcatQuantum);
    }

    // Synchronized so we can't start and stop logging at the same time
    protected static synchronized void writeLogcatToDisk(final Context context, final int kbFileSize) {
        // If we're already logging, then swell: we're done
        if (loggingThread != null) {
            return;
        }

        // Make a thread that will hang around so long as logging is active
        loggingThread = new Thread("Logging Thread") {
            @SuppressLint("DefaultLocale")
            @Override
            public void run() {
                try {
                    /**
                     * logcat is documented <a href="https://developer.android.com/studio/command-line/logcat.html">here</a>.
                     * A brief summary:
                     *
                     * logcat [option] ... [filter-spec] ...
                     *
                     *    -c            Clears (flushes) the entire log and exits.
                     *    -f filename   Writes log message output to filename
                     *    -r kbytes     Rotates the log file every kbytes of output. The default value is 16. Requires the -f option.
                     *    -n count      Sets the maximum number of rotated logs to count. The default value is 4. Requires the -r option.
                     *    -v format     Sets the output format for log messages. The default is brief format.
                     *
                     * output formats are documented <a href="https://developer.android.com/studio/command-line/logcat.html#outputFormat">here</a>.
                     *    brief, process, tag, raw, time, threadtime, long
                     */
                    final File file = getLogFile(context);
                    final String filename = file.getAbsolutePath();

                    final String commandLine = String.format("%s -f %s -r%d -n%d -v %s %s", logcatCommand, filename, kbFileSize, logcatRotatedLogsMax, logcatFormat, logcatFilter);

                    RobotLog.v("saving logcat to " + filename);
                    RobotLog.v("logging command line: " + commandLine);
                    final RunShellCommand shell = new RunShellCommand();

                    // Paranoia: kill any existing logger we spawned previously
                    RunShellCommand.killSpawnedProcess(logcatCommand, context.getPackageName(), shell);

                    // (Don't) empty the log: while this would avoid the multi-copies-of-log-entries
                    // problem, not clearing the log gives us a chance to capture any logcat entries that might
                    // be still lingering from an immediately preceding crash, entries that might be especially
                    // valuable to capture.
                    // shell.run(String.format("%s -c", logcatCommand));

                    // Dribble to disk until we're cancelled. Note that this call to run()
                    // doesn't return until such cancellation happens.
                    shell.run(commandLine);

                } catch (RobotCoreException e) {
                    RobotLog.v("Error while initializing RobotLog to disk: " + e.toString());
                } finally {
                    loggingThread = null;
                }
            }
        };
        // Start that thread a-going
        loggingThread.start();
    }

    public static String getLogFilename() {
        return getLogFilename(AppUtil.getDefContext());
    }

    public static String getLogFilename(Context context) {
        File directory = AppUtil.LOG_FOLDER;
        //noinspection ResultOfMethodCallIgnored
        directory.mkdirs(); // paranoia, though we *might* have actually seen this needed, hard to tell

        String name;
        if (AppUtil.getInstance().isRobotController()) {
            name = "robotControllerLog.txt";
        } else if (AppUtil.getInstance().isDriverStation()) {
            name = "driverStationLog.txt";
        } else {
            name = context.getPackageName() + "Log.txt";
        }
        File file = new File(directory, name);
        return file.getAbsolutePath();
    }

    private static File getLogFile(Context context) {
        return new File(getLogFilename(context));
    }

    // Returns the extant log files, which include the rollovers, eg:
    // com.qualcomm.ftcrobotcontroller.logcat.1.gz
    public static List<File> getExtantLogFiles(Context context) {
        List<File> result = new ArrayList<>();
        File root = getLogFile(context);
        result.add(root);
        for (int i = 1; true; i++) {
            File compressed = new File(root.getParentFile(), root.getName() + "." + i + ".gz");
            if (compressed.exists()) {
                result.add(compressed);
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * Cancels any logcat writing to disk that might currently be going on
     */
    public static synchronized void cancelWriteLogcatToDisk() {
        // Synchronized so we can't start and top logging at the same time
        // If we're not logging, then we've got nothing to do
        if (loggingThread == null) {
            return;
        }

        Context context = AppUtil.getDefContext();
        final String packageName = context.getPackageName();
        final String filename = getLogFile(context).getAbsolutePath();

        // let last few log messages out before we stop logging
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // just continue
        }

        // Kill off the logging process. That will let the shell.run() in the logging thread return
        try {
            RobotLog.v("Killing logcat process.");
            RunShellCommand shell = new RunShellCommand();
            RunShellCommand.killSpawnedProcess(logcatCommand, packageName, shell);
        } catch (RobotCoreException e) {
            RobotLog.e("Unable to cancel writing log file to disk: " + e.toString());
            return;
        }

        RobotLog.v("Waiting for the logcat process to die.");
        ElapsedTime timeoutTimer = new ElapsedTime();
        // wait until the log thread terminates
        while (loggingThread != null) {
            if (timeoutTimer.milliseconds() > 1000.0) {
                loggingThread.interrupt();
            }
            Thread.yield();
        }
    }

    public static void logBuildConfig(Class buildConfig) {
        String moduleName = getStringStatic(buildConfig, "APPLICATION_ID");
        int versionCode = getIntStatic(buildConfig, "VERSION_CODE");
        String versionName = getStringStatic(buildConfig, "VERSION_NAME");
        RobotLog.v("BuildConfig: versionCode=%d versionName=%s module=%s", versionCode, versionName, moduleName);
    }

    protected static String getStringStatic(Class clazz, String name) {
        try {
            return (String) (clazz.getField(name).get(null));
        } catch (Exception ignored) {
            return "";
        }
    }

    protected static int getIntStatic(Class clazz, String name) {
        try {
            return (clazz.getField(name).getInt(null));
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static void logBytes(String tag, String caption, byte[] data, int cb) {
        logBytes(tag, caption, data, 0, cb);
    }

    public static void logBytes(String tag, String caption, byte[] data, int ibStart, int cb) {
        int cbLine = 16;
        char separator = ':';
        for (int ibFirst = ibStart; ibFirst < cb; ibFirst += cbLine) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < cbLine; i++) {
                int ib = i + ibFirst;
                if (ib >= cb) {
                    break;
                }
                line.append(String.format("%02x ", data[ib]));
            }
            vv(tag, "%s%c %s", caption, separator, line.toString());
            separator = '|';
        }
    }
}
