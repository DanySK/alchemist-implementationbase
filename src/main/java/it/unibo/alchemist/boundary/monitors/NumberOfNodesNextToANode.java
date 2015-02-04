/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.monitors;

import org.danilopianini.view.ExportForGUI;

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
public class NumberOfNodesNextToANode<T> extends EnvironmentInspector<T> {

	private static final long serialVersionUID = 6973385303909686690L;

	@ExportForGUI(nameToExport = "ID of the central node")
	private String id = "0";
	@ExportForGUI(nameToExport = "Range")
	private String range = "10.0";

	@Override
	protected double[] extractValues(final IEnvironment<T> env, final IReaction<T> r, final ITime time, final long step) {
		try {
			return new double[] { env.getNodesWithinRange(env.getNodeByID(Integer.parseInt(id)), Double.parseDouble(range)).size() };
		} catch (NumberFormatException e) {
			L.warn(e);
		}
		return new double[]{Double.NaN};
	}

	/**
	 * @return id
	 */
	protected String getId() {
		return id;
	}

	/**
	 * @param idn id
	 */
	protected void setId(final String idn) {
		this.id = idn;
	}

	/**
	 * @return range
	 */
	protected String getRange() {
		return range;
	}

	/**
	 * @param rng range
	 */
	protected void setRange(final String rng) {
		this.range = rng;
	}

}
