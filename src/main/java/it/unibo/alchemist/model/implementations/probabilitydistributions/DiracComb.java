/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.probabilitydistributions;

import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.ITime;

/**
 * A DiracComb is a sequence of events that happen every fixed time interval.
 * 
 * @author Danilo Pianini
 * @param <T>
 * 
 */
public class DiracComb<T> extends AbstractDistribution<T> {

	private static final long serialVersionUID = -5382454244629122722L;

	private final double timeInterval;

	/**
	 * @param start
	 *            initial time
	 * @param rate
	 *            how many events should happen per time unit
	 */
	public DiracComb(final ITime start, final double rate) {
		super(start);
		timeInterval = 1 / rate;
	}

	/**
	 * @param rate
	 *            how many events should happen per time unit
	 */
	public DiracComb(final double rate) {
		this(new DoubleTime(), rate);
	}

	@Override
	public double getRate() {
		return 1 / timeInterval;
	}

	@Override
	protected void updateStatus(final ITime curTime, final boolean executed, final double param, final IEnvironment<T> env) {
		if (executed) {
			setTau(new DoubleTime(curTime.toDouble() + timeInterval));
		}
	}

	@Override
	public DiracComb<T> clone() {
		return new DiracComb<>(getNextOccurence(), 1 / timeInterval);
	}

}
