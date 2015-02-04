/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.linkingrules;

import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IEnvironment2DWithObstacles;
import it.unibo.alchemist.model.interfaces.INeighborhood;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IPosition;

/**
 * Similar to {@link EuclideanDistance}, but if the environment has obstacles,
 * the links are removed.
 * 
 * @author Danilo Pianini
 * 
 * @param <T>
 */
public class ObstaclesBreakConnection<T> extends EuclideanDistance<T> {

	private static final long serialVersionUID = -3279202906910960340L;

	/**
	 * @param radius
	 *            connection range
	 */
	public ObstaclesBreakConnection(final Double radius) {
		super(radius);
	}

	@Override
	public INeighborhood<T> computeNeighborhood(final INode<T> center, final IEnvironment<T> env) {
		final INeighborhood<T> normal = super.computeNeighborhood(center, env);
		if (!normal.isEmpty() && env instanceof IEnvironment2DWithObstacles) {
			final IPosition cp = env.getPosition(center);
			@SuppressWarnings("unchecked")
			final IEnvironment2DWithObstacles<?, T> environment = (IEnvironment2DWithObstacles<?, T>) env;
			for (int i = 0; i < normal.size(); i++) {
				final INode<T> node = normal.getNeighborByNumber(i);
				final IPosition np = environment.getPosition(node);
				if (environment.intersectsObstacle(cp, np)) {
					normal.removeNeighbor(node);
					i--;
				}
			}
		}
		return normal;
	}

}
