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
package it.unibo.alchemist.model.implementations.molecules;

import org.danilopianini.lang.util.FasterString;

import it.unibo.alchemist.model.interfaces.IMolecule;


/**
 *         Simple implementation of Molecule. Ids are generated through a simple
 *         Singleton Pattern, no thread safeness is provided.
 * 
 */
public class Molecule implements IMolecule {

    private static final long serialVersionUID = 2727376723102146271L;

    private final FasterString n;

    /**
     * @param name
     *            the molecule name
     */
    public Molecule(final String name) {
        this(new FasterString(name));
    }

    /**
     * @param name
     *            the molecule name
     */
    public Molecule(final FasterString name) {
        this.n = name;
    }

    @Override
    public boolean dependsOn(final IMolecule mol) {
        return equals(mol);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Molecule) {
            return ((Molecule) obj).n.equals(n);
        }
        return false;
    }

    @Override
    public long getId() {
        return n.hash64();
    }

    @Override
    public int hashCode() {
        return n.hashCode();
    }

    @Override
    public String toString() {
        return n + "[ID: " + n.hashToString() + "]";
    }

    /**
     * @return a {@link FasterString} version of this {@link Molecule}
     */
    public final FasterString toFasterString() {
        return n;
    }

}
