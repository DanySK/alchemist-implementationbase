/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.monitors;

import gnu.trove.TDoubleCollection;
import gnu.trove.list.array.TDoubleArrayList;
import it.unibo.alchemist.model.implementations.utils.Aggregator;
import it.unibo.alchemist.model.implementations.utils.SubNaN;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;

import java.util.ArrayList;
import java.util.List;

import org.danilopianini.view.ExportForGUI;

/**
 * @author Danilo Pianini
 *
 * @param <S>
 * @param <T>
 */
public abstract class EnvironmentSampler<S, T> extends EnvironmentInspector<T> {

	private static final long serialVersionUID = 4933331976793542L;
	@ExportForGUI(nameToExport = "Data aggregator")
	private Aggregator aggregator = Aggregator.MEAN;
	@ExportForGUI(nameToExport = "Filter NaN with")
	private SubNaN subnan = SubNaN.Eliminate;

	@Override
	protected final double[] extractValues(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step) {
		final List<TDoubleCollection> vpn = new ArrayList<>();
		for (final S sample : computeSamples(env, r, time, step)) {
			final double[] prop = getProperties(env, sample, r, time, step);
			for (int i = 0; i < prop.length; i++) {
				if (Double.isNaN(prop[i])) {
					if (!subnan.equals(SubNaN.Eliminate)) {
						expandList(vpn, i);
						vpn.get(i).add(subnan.getSubstitutionValue());
					}
				} else {
					expandList(vpn, i);
					vpn.get(i).add(prop[i]);
				}
			}
		}
		return aggregator.aggregate(vpn);
	}

	/**
	 * Given a sample on this environment, compute the related properties.
	 * 
	 * @param env environment
	 * @param sample sample
	 * @param r reaction
	 * @param time current time
	 * @param step current step
	 * @return an array of properties of interest
	 */
	protected abstract double[] getProperties(IEnvironment<T> env, S sample, IReaction<T> r, ITime time, long step);

	/**
	 * Given the current status of the simulated world, extract a list of samples to extract properties.
	 * 
	 * @param env environment
	 * @param r reaction
	 * @param time current time
	 * @param step current step
	 * @return an {@link Iterable} along samples
	 */
	protected abstract Iterable<S> computeSamples(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step);

	private static void expandList(final List<TDoubleCollection> vpn, final int i) {
		if (i >= vpn.size()) {
			vpn.add(new TDoubleArrayList());
		}
	}

	/**
	 * @return aggregator
	 */
	protected Aggregator getAggregator() {
		return aggregator;
	}

	/**
	 * @param a
	 *            aggregator
	 */
	protected void setAggregator(final Aggregator a) {
		this.aggregator = a;
	}

	/**
	 * @return SubNaN
	 */
	protected SubNaN getSubnan() {
		return subnan;
	}

	/**
	 * @param snan
	 *            subnan
	 */
	protected void setSubnan(final SubNaN snan) {
		this.subnan = snan;
	}

}
