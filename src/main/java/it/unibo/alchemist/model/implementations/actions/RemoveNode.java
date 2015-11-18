/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.actions;

import it.unibo.alchemist.model.interfaces.Context;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IReaction;

/**
 * @param <T> concentration type
 */
public class RemoveNode<T> extends AbstractAction<T> {

    /**
     * 
     */
    private static final long serialVersionUID = -7358217984854060148L;
    private final IEnvironment<T> env;

    /**
     * @param environment the current environment
     * @param node the node for this action
     */
    public RemoveNode(final IEnvironment<T> environment, final INode<T> node) {
        super(node);
        env = environment;
    }

    @Override
    public Context getContext() {
        return Context.LOCAL;
    }

    @Override
    public void execute() {
        env.removeNode(getNode());
    }

    @Override
    public String toString() {
        return "Remove node " + getNode().getId();
    }

    /**
     * @return the current environment
     */
    protected IEnvironment<T> getEnvironment() {
        return env;
    }

    @Override
    public RemoveNode<T> cloneOnNewNode(final INode<T> n, final IReaction<T> r) {
        return new RemoveNode<T>(getEnvironment(), n);
    }

}
