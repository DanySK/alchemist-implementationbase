/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.utils;

/**
 * @author Danilo Pianini
 * 
 */
public enum SubNaN {
	/**
	 * 
	 */
	Eliminate(Double.NaN), Zero(0), One(1), Infinity(Double.POSITIVE_INFINITY), MinusInfinity(Double.NEGATIVE_INFINITY);

	private final double val;

	private SubNaN(final double d) {
		val = d;
	}

	/**
	 * @return the value to use to replace {@link Double}.NaN
	 */
	public double getSubstitutionValue() {
		return val;
	}
}
