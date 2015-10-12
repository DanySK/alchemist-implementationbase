/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.probabilitydistributions;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import it.unibo.alchemist.external.cern.jet.random.Exponential;
import it.unibo.alchemist.external.cern.jet.random.engine.RandomEngine;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.ITime;

/**
 * Markovian events.
 * 
 * @author Danilo Pianini
 *
 * @param <T>
 */
public class ExponentialTime<T> extends AbstractDistribution<T> {

	private static final long serialVersionUID = 5216987069271114818L;
	private double oldPropensity = -1;
	private final Exponential exp;
	private final RandomEngine rand;
	private final double rate;

	/**
	 * @param markovianRate
	 *            Markovian rate for this distribution
	 * @param random
	 *            {@link RandomEngine} used internally
	 */
	public ExponentialTime(final double markovianRate, final RandomEngine random) {
		this(markovianRate, new DoubleTime(), random);
	}
	
	/**
	 * @param markovianRate
	 *            Markovian rate for this distribution
	 * @param start
	 *            initial time
	 * @param random
	 *            {@link RandomEngine} used internally
	 */
	public ExponentialTime(final double markovianRate, final ITime start, final RandomEngine random) {
		super(start);
		rate = markovianRate;
		rand = random;
		exp = new Exponential(rand);
	}

	@Override
	public void updateStatus(
			final ITime curTime,
			final boolean executed,
			final double newpropensity,
			final IEnvironment<T> env) {
		assert !Double.isNaN(newpropensity);
		assert !Double.isNaN(oldPropensity);
		if (oldPropensity == 0 && newpropensity != 0) {
			update(newpropensity, true, curTime);
		} else if (oldPropensity != 0 && newpropensity != 0) {
			update(newpropensity, executed, curTime);
		} else if (oldPropensity != 0 && newpropensity == 0) {
			setTau(DoubleTime.INFINITE_TIME);
		}
		oldPropensity = newpropensity;
	}
	
	@SuppressFBWarnings("FE_FLOATING_POINT_EQUALITY")
	private void update(final double newpropensity, final boolean isMu, final ITime curTime) {
		assert !Double.isNaN(newpropensity);
		assert !Double.isNaN(oldPropensity);
		if (isMu) {
			final ITime dt = genTime(newpropensity);
			setTau(curTime.sum(dt));
		} else {
			if (oldPropensity != newpropensity) {
				final ITime sub = getNextOccurence().subtract(curTime);
				final ITime mul = sub.multiply(oldPropensity / newpropensity);
				setTau(mul.sum(curTime));
			}
		}
	}

	/**
	 * @param propensity
	 *            the current propensity for the reaction
	 * @return the next occurrence time for the reaction, in case this is the
	 *         reaction which have been executed.
	 */
	protected ITime genTime(final double propensity) {
		return new DoubleTime(exp.nextDouble(propensity));
	}

	@Override
	@SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
	public ExponentialTime<T> clone() {
		return new ExponentialTime<>(rate, getNextOccurence(), rand);
	}

	@Override
	public double getRate() {
		return rate;
	}
	
}
