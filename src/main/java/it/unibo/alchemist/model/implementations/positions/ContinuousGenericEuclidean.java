/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
/**
 * 
 */
package it.unibo.alchemist.model.implementations.positions;

import java.util.Arrays;

import org.danilopianini.lang.Couple;
import org.danilopianini.lang.HashUtils;

import it.unibo.alchemist.exceptions.UncomparableDistancesException;
import it.unibo.alchemist.model.interfaces.IPosition;
import it.unibo.alchemist.utils.MathUtils;

/**
 * @author Danilo Pianini
 * @version 20110129
 */
public class ContinuousGenericEuclidean implements IPosition {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2993200108153260352L;
	private final double[] c;
	private int hash;

	private String stringCache;

	/**
	 * Faster constructor for bidimensional positions.
	 * 
	 * @param x
	 *            x position
	 * @param y
	 *            y position
	 */
	protected ContinuousGenericEuclidean(final double x, final double y) {
		c = new double[] { x, y };
		org.apache.commons.math3.util.MathUtils.checkFinite(c);
	}

	/**
	 * Faster constructor for bidimensional positions.
	 * 
	 * @param x
	 *            x position
	 * @param y
	 *            y position
	 * @param z
	 *            z position
	 */
	protected ContinuousGenericEuclidean(final double x, final double y, final double z) {
		c = new double[] { x, y, z };
		org.apache.commons.math3.util.MathUtils.checkFinite(c);
	}

	/**
	 * Slower than other constructors. Prefer others for 2D and 3D positions.
	 * 
	 * @param coord
	 *            the coordinates
	 */
	public ContinuousGenericEuclidean(final double[] coord) {
		c = Arrays.copyOf(coord, coord.length);
		org.apache.commons.math3.util.MathUtils.checkFinite(c);
	}

	@Override
	public Couple<IPosition> buildBoundingBox(final double range) {
		final double[] bl = Arrays.copyOf(c, c.length);
		final double[] ur = Arrays.copyOf(c, c.length);
		for (int i = 0; i < c.length; i++) {
			bl[i] -= range;
			ur[i] += range;
		}
		return new Couple<IPosition>(new ContinuousGenericEuclidean(bl), new ContinuousGenericEuclidean(ur));
	}

	@Override
	public int compareTo(final IPosition o) {
		if (c.length < o.getDimensions()) {
			return -1;
		}
		if (c.length > o.getDimensions()) {
			return 1;
		}
		final double[] pos = o.getCartesianCoordinates();
		for (int i = 0; i < c.length; i++) {
			if (c[i] < pos[i]) {
				return -1;
			}
			if (c[i] > pos[i]) {
				return 1;
			}
		}
		return 0;
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof IPosition) {
			return samePosition((IPosition) o);
		} else {
			return false;
		}
	}

	@Override
	public double[] getCartesianCoordinates() {
		return Arrays.copyOf(c, c.length);
	}

	@Override
	public double getCoordinate(final int dim) {
		if (dim < 0 || dim >= c.length) {
			throw new IllegalArgumentException(dim + "is not an allowed dimension, only values between 0 and " + c.length + "are allowed.");
		}
		return c[dim];
	}

	@Override
	public int getDimensions() {
		return c.length;
	}

	@Override
	public double getDistanceTo(final IPosition p) {
		final double[] coord = p.getCartesianCoordinates();
		if (c.length == coord.length) {
			return MathUtils.getEuclideanDistance(c, coord);
		} else {
			throw new UncomparableDistancesException(this, p);
		}
	}

	@Override
	public int hashCode() {
		if (hash == 0) {
			hash = HashUtils.djb2int32(c);
		}
		return hash;
	}

	/**
	 * @param o
	 *            the position to compare with
	 * @return true if the two positions are the the same
	 */
	public boolean samePosition(final IPosition o) {
		final double[] p = o.getCartesianCoordinates();
		if (c.length == p.length) {
			for (int i = 0; i < c.length; i++) {
				if (p[i] != c[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		if (stringCache == null) {
			stringCache = Arrays.toString(c);
		}
		return stringCache;
	}

}
