/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.monitors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import org.apache.commons.math3.util.FastMath;
import org.danilopianini.lang.RangedInteger;
import org.danilopianini.view.ExportForGUI;

import it.unibo.alchemist.boundary.interfaces.OutputMonitor;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;
import it.unibo.alchemist.utils.L;

/**
 * @author Danilo Pianini
 * 
 * @param <T>
 */
@ExportInspector
public abstract class EnvironmentInspector<T> implements OutputMonitor<T> {

	private static final long serialVersionUID = -6609357608585315L;
	private static final int OOM_RANGE = 24;

	private static enum Mode {
		TIME, STEP
	}

	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault());
	private PrintStream writer;
	private String fpCache;
	private double lastUpdate = Double.NEGATIVE_INFINITY;
	private long lastStep = Long.MIN_VALUE;
	private final Semaphore mutex = new Semaphore(1);

	@ExportForGUI(nameToExport = "File path")
	private String filePath = System.getProperty("user.home") + System.getProperty("file.separator") + sdf.format(new Date()) + "-alchemist_report.log";
	@ExportForGUI(nameToExport = "Value separator")
	private String separator = " ";
	@ExportForGUI(nameToExport = "Report time")
	private boolean logTime = true;
	@ExportForGUI(nameToExport = "Report step")
	private boolean logStep = true;
	@ExportForGUI(nameToExport = "Sampling mode")
	private Mode mode = Mode.TIME;
	@ExportForGUI(nameToExport = "Sample order of magnitude")
	private RangedInteger intervaloom = new RangedInteger(-OOM_RANGE, OOM_RANGE, 0);
	@ExportForGUI(nameToExport = "Sample space")
	private RangedInteger interval = new RangedInteger(1, 100, 1);

	@Override
	public void finished(final IEnvironment<T> env, final ITime time, final long step) {
		if (writer != null) {
			writer.close();
		}
		writer = null;
		lastUpdate = Double.NEGATIVE_INFINITY;
		lastStep = Long.MIN_VALUE;
		fpCache = null;
	}

	@Override
	public void initialized(final IEnvironment<T> env) {
		stepDone(env, null, new DoubleTime(), 0);
	}

	@Override
	public void stepDone(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step) {
		mutex.acquireUninterruptibly();
		if (System.identityHashCode(fpCache) != System.identityHashCode(filePath)) {
			fpCache = filePath;
			if (writer != null) {
				writer.close();
			}
			try {
				writer = new PrintStream(new File(fpCache));
			} catch (FileNotFoundException e) {
				L.error(e);
			}
		}
		final double sample = interval.getVal() * FastMath.pow(10, intervaloom.getVal());
		final boolean log = mode.equals(Mode.TIME) ?  time.toDouble() - lastUpdate >= sample : step - lastStep >= sample;
		if (log) {
			lastUpdate = time.toDouble();
			lastStep = step;
			writeData(env, r, time, step);
		}
		mutex.release();
	}

	private void writeData(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step) {
		if (writer == null) {
			throw new IllegalStateException("Error initializing the file writer in " + getClass().getCanonicalName());
		}
		if (logTime) {
			writer.print(time.toDouble());
			writeSeparator();
		}
		if (logStep) {
			writer.print(step);
			writeSeparator();
		}
		for (final double d : extractValues(env, r, time, step)) {
			writer.print(d);
			writeSeparator();
		}
		writer.println();
	}

	private void writeSeparator() {
		writer.print(separator);
	}

	/**
	 * @return file path
	 */
	protected String getFilePath() {
		return filePath;
	}

	/**
	 * @param fp file path
	 */
	protected void setFilePath(final String fp) {
		this.filePath = fp;
	}

	/**
	 * @return separator
	 */
	protected String getSeparator() {
		return separator;
	}

	/**
	 * @param s separator
	 */
	protected void setSeparator(final String s) {
		this.separator = s;
	}

	/**
	 * @return true if the {@link EnvironmentInspector} is logging time
	 */
	protected boolean isLoggingTime() {
		return logTime;
	}

	/**
	 * @param lt true if you want to log time
	 */
	protected void setLogTime(final boolean lt) {
		this.logTime = lt;
	}

	/**
	 * @return true if the {@link EnvironmentInspector} is logging steps
	 */
	protected boolean isLoggingStep() {
		return logStep;
	}

	/**
	 * @param ls true if you want to log steps
	 */
	protected void setLogStep(final boolean ls) {
		this.logStep = ls;
	}

	/**
	 * @return current mode
	 */
	protected Mode getMode() {
		return mode;
	}

	/**
	 * @param m the mode
	 */
	protected void setMode(final Mode m) {
		this.mode = m;
	}

	/**
	 * @return order of magnitude of the sapling interval
	 */
	protected RangedInteger getIntervalOrderOfMagnitude() {
		return intervaloom;
	}

	/**
	 * @param ioom order of magnitude of the sapling interval
	 */
	protected void setIntervaloom(final RangedInteger ioom) {
		this.intervaloom = ioom;
	}

	/**
	 * @return sampling interval
	 */
	protected RangedInteger getInterval() {
		return interval;
	}

	/**
	 * @param i sampling interval
	 */
	protected void setInterval(final RangedInteger i) {
		this.interval = i;
	}

	/**
	 * This method extracts data values from an environment snapshot.
	 * 
	 * @param env
	 *            environment
	 * @param r
	 *            reaction executed
	 * @param time
	 *            time
	 * @param step
	 *            step
	 * @return an array of data values
	 */
	protected abstract double[] extractValues(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step);

}
