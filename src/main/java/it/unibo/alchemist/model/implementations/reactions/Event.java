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
package it.unibo.alchemist.model.implementations.reactions;

import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.ITime;
import it.unibo.alchemist.model.interfaces.TimeDistribution;

/**
 * This reaction completely ignores the propensity conditioning of the
 * conditions, and tries to run every time the {@link TimeDistribution} wants
 * to.
 * 
 * @param <T>
 */
public class Event<T> extends AReaction<T> {

    private static final long serialVersionUID = -1640973841645383193L;

    /**
     * @param node the node this {@link Event} belongs to
     * @param timedist the {@link TimeDistribution} this event should use
     */
    public Event(final INode<T> node, final TimeDistribution<T> timedist) {
        super(node, timedist);
    }

    @Override
    protected void updateInternalStatus(final ITime curTime, final boolean executed, final IEnvironment<T> env) {
    }

    @Override
    public double getRate() {
        return getTimeDistribution().getRate();
    }

    @Override
    public Event<T> cloneOnNewNode(final INode<T> n) {
        return new Event<>(n, getTimeDistribution().clone());
    }


}
