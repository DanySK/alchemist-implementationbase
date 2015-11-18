/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.linkingrules;

import it.unibo.alchemist.model.implementations.neighborhoods.Neighborhood;
import it.unibo.alchemist.model.interfaces.ILinkingRule;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.INeighborhood;
import it.unibo.alchemist.model.interfaces.INode;

/**
 * LinkingRule which connects nodes whose euclidean distance is shorter than a
 * given radius.
 * 
 * @param <T>
 *            The type which describes the concentration of a molecule
 */
public class EuclideanDistance<T> implements ILinkingRule<T> {

    private static final long serialVersionUID = -405055780667941773L;
    private final double range;

    /**
     * @param radius
     *            connection radius
     */
    public EuclideanDistance(final double radius) {
        range = radius;
    }

    @Override
    public INeighborhood<T> computeNeighborhood(final INode<T> center, final IEnvironment<T> env) {
        return new Neighborhood<>(center, env.getNodesWithinRange(center, range), env);
    }

    /**
     * @return the range
     */
    protected final double getRange() {
        return range;
    }

}
