/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.reactions;

import it.unibo.alchemist.model.interfaces.ICondition;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.ITime;
import it.unibo.alchemist.model.interfaces.TimeDistribution;

/**
 * 
 * 
 * @param <T>
 */
public class ChemicalReaction<T> extends AReaction<T> {

    private static final long serialVersionUID = -5260452049415003046L;
    private double currentRate;

    /**
     * @param n
     *            node
     * @param pd
     *            time distribution
     */
    public ChemicalReaction(final INode<T> n, final TimeDistribution<T> pd) {
        super(n, pd);
    }

    @Override
    public ChemicalReaction<T> cloneOnNewNode(final INode<T> n) {
        return new ChemicalReaction<>(n, getTimeDistribution().clone());
    }

    @Override
    protected void updateInternalStatus(final ITime curTime, final boolean executed, final IEnvironment<T> env) {
        currentRate = getTimeDistribution().getRate();
        for (final ICondition<T> cond : getConditions()) {
            final double v = cond.getPropensityConditioning();
            if (v == 0) {
                currentRate = 0;
                break;
            }
            if (v < 0) {
                throw new IllegalStateException("Condition " + cond + " returned a negative propensity conditioning value");
            }
            currentRate *= cond.getPropensityConditioning();
        }
    }

    @Override
    public double getRate() {
        return currentRate;
    }

}
