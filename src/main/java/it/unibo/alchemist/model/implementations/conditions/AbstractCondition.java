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
package it.unibo.alchemist.model.implementations.conditions;

import it.unibo.alchemist.model.interfaces.ICondition;
import it.unibo.alchemist.model.interfaces.IMolecule;
import it.unibo.alchemist.model.interfaces.INode;

import java.util.ArrayList;
import java.util.List;


/**
 *
 * @param <T>
 */
public abstract class AbstractCondition<T> implements ICondition<T> {

    private static final long serialVersionUID = -1610947908159507754L;
    private final List<IMolecule> influencing = new ArrayList<IMolecule>(1);
    private final INode<T> n;

    /**
     * @param node the node this Condition belongs to
     */
    public AbstractCondition(final INode<T> node) {
        this.n = node;
    }

    @Override
    public List<? extends IMolecule> getInfluencingMolecules() {
        return influencing;
    }

    @Override
    public INode<T> getNode() {
        return n;
    }

    /**
     * @param m the molecule to add
     */
    protected void addReadMolecule(final IMolecule m) {
        influencing.add(m);
    }
}
