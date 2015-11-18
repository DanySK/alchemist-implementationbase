/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.actions;

import it.unibo.alchemist.external.cern.jet.random.engine.RandomEngine;
import it.unibo.alchemist.model.implementations.positions.Continuous2DEuclidean;
import it.unibo.alchemist.model.interfaces.IAction;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IPosition;
import it.unibo.alchemist.model.interfaces.IReaction;

/**
 * Moves the node randomly.
 * 
 * @param <T>
 */
public class BrownianMove<T> extends AbstractMoveNode<T> {

    private static final long serialVersionUID = -904100978119782403L;
    private final double r;
    private final RandomEngine rng;

    /**
     * @param environment
     *            the environment
     * @param node
     *            the node
     * @param rand
     *            the simulation {@link RandomEngine}.
     * @param range
     *            the maximum distance the node may walk in a single step for
     *            each dimension
     */
    public BrownianMove(final IEnvironment<T> environment, final INode<T> node, final RandomEngine rand, final double range) {
        super(environment, node);
        r = range;
        rng = rand;
    }

    @Override
    public IAction<T> cloneOnNewNode(final INode<T> n, final IReaction<T> reaction) {
        return new BrownianMove<>(getEnvironment(), n, rng, r);
    }

    @Override
    public IPosition getNextPosition() {
        return new Continuous2DEuclidean(genRandom() * r, genRandom() * r);
    }

    private double genRandom() {
        return rng.nextFloat() - 0.5;
    }

    /**
     * @return the movement radius
     */
    protected double getRadius() {
        return r;
    }

    /**
     * @return the {@link RandomEngine}
     */
    protected RandomEngine getRandomEngine() {
        return rng;
    }

}
