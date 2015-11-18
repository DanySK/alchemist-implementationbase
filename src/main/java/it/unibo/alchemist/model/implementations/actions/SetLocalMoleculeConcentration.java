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
package it.unibo.alchemist.model.implementations.actions;

import it.unibo.alchemist.model.interfaces.Context;
import it.unibo.alchemist.model.interfaces.IAction;
import it.unibo.alchemist.model.interfaces.IMolecule;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IReaction;

/**
 *
 * @param <T>
 */
public class SetLocalMoleculeConcentration<T> extends AbstractActionOnSingleMolecule<T> {

    private static final long serialVersionUID = -197253027556270645L;
    private final T val;

    /**
     * @param node
     *            The node to which this action belongs
     * @param target
     *            the molecule whose concentration will be modified
     * @param value
     *            the new concentration value for the molecule
     */
    public SetLocalMoleculeConcentration(final INode<T> node, final IMolecule target, final T value) {
        super(node, target);
        this.val = value;
    }

    @Override
    public IAction<T> cloneOnNewNode(final INode<T> n, final IReaction<T> r) {
        return new SetLocalMoleculeConcentration<T>(n, getMolecule(), val);
    }

    @Override
    public void execute() {
        getNode().setConcentration(getMolecule(), val);
    }

    @Override
    public Context getContext() {
        return Context.LOCAL;
    }

}
