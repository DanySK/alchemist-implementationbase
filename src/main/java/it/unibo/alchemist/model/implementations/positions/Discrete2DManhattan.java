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

import org.danilopianini.lang.Couple;

import it.unibo.alchemist.exceptions.UncomparableDistancesException;
import it.unibo.alchemist.model.interfaces.IPosition;

/**
 * @author Danilo Pianini
 * 
 * @version 20110107
 * 
 *          This class measures distances as integers. Suitable for
 *          bidimensional discrete environments. The distance between two nodes
 *          is computed as Manhattan distance.
 * 
 */
public class Discrete2DManhattan implements IPosition {

	private static final int MASK = 0x0000FFFF;
	private static final long serialVersionUID = 4773955346963361299L;
	private static final byte SHIFT = 16;
	private final int xCoord, yCoord, hash;

	/**
	 * @param x
	 *            X coordinate
	 * @param y
	 *            Y coordinate
	 */
	public Discrete2DManhattan(final int x, final int y) {
		this.xCoord = x;
		this.yCoord = y;
		hash = (x & MASK) << SHIFT | (y & MASK);
	}

	@Override
	public Couple<IPosition> buildBoundingBox(final double r) {
		final int range = (int) r;
		final Discrete2DManhattan bl = new Discrete2DManhattan(xCoord - range, yCoord - range);
		final Discrete2DManhattan ur = new Discrete2DManhattan(xCoord + range, yCoord + range);
		return new Couple<IPosition>(bl, ur);
	}

	@Override
	public int compareTo(final IPosition o) {
		if (o.getDimensions() > 2) {
			return -1;
		}
		if (o.getDimensions() < 2) {
			return 1;
		}
		final double[] pos = o.getCartesianCoordinates();
		if (xCoord < pos[0]) {
			return -1;
		}
		if (xCoord > pos[0]) {
			return 1;
		}
		if (yCoord < pos[1]) {
			return -1;
		}
		if (yCoord > pos[1]) {
			return 1;
		}
		return 0;
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof Discrete2DManhattan) {
			final Discrete2DManhattan d = (Discrete2DManhattan) o;
			return xCoord == d.xCoord && yCoord == d.yCoord;
		}
		return false;
	}

	@Override
	public double[] getCartesianCoordinates() {
		return new double[] { xCoord, yCoord };
	}

	@Override
	public double getCoordinate(final int dim) {
		if (dim == 0) {
			return xCoord;
		}
		if (dim == 1) {
			return yCoord;
		}
		throw new IllegalArgumentException(dim + " is not a valid dimension. Only 0 and 1 can be used.");
	}

	@Override
	public int getDimensions() {
		return 2;
	}

	@Override
	public double getDistanceTo(final IPosition p) {
		try {
			final Discrete2DManhattan d = (Discrete2DManhattan) p;
			return Math.abs(xCoord - d.xCoord) + Math.abs(yCoord - d.yCoord);
		} catch (ClassCastException e) {
			throw new UncomparableDistancesException(this, p);
		}
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public String toString() {
		return "[" + xCoord + "," + yCoord + "]";
	}

}
