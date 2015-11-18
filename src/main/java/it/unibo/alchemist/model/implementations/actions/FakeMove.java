/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.actions;

import it.unibo.alchemist.model.interfaces.IAction;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IPosition;
import it.unibo.alchemist.model.interfaces.IReaction;

/**
 * Fake movement class, used only to trigger the neighborhood update.
 * 
 * @param <T>
 */
public class FakeMove<T> extends AbstractMoveNode<T> {

    private static final long serialVersionUID = 1774989279335172458L;

    /**
     * @param environment
     *            the environment
     * @param node
     *            the node
     */
    public FakeMove(final IEnvironment<T> environment, final INode<T> node) {
        super(environment, node, true);
    }

    @Override
    public IAction<T> cloneOnNewNode(final INode<T> n, final IReaction<T> r) {
        return new FakeMove<>(getEnvironment(), n);
    }

    @Override
    public IPosition getNextPosition() {
        return getEnvironment().getPosition(getNode());
    }

}
