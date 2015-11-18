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
package it.unibo.alchemist.model.implementations.nodes;

import it.unibo.alchemist.model.interfaces.IMolecule;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IReaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.danilopianini.concurrency.ThreadLocalIdGenerator;


/**
 * This class realizes an abstract node. You may extend it to realize your own
 * nodes.
 * 
 * @param <T>
 */
public abstract class GenericNode<T> implements INode<T> {

    private static final int CENTER = 0;
    private static final int MAX = 1073741824;
    private static final int MIN = -MAX;
    private static final ThreadLocal<Integer> ODD = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 1;
        }
    };
    private static final ThreadLocal<Boolean> POSITIVE = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return true;
        }
    };
    private static final ThreadLocal<Integer> POW = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 1;
        }
    };
    private static final long serialVersionUID = 2496775909028222278L;
    private static final ThreadLocalIdGenerator SINGLETON = new ThreadLocalIdGenerator();
    private static final AtomicInteger THREAD_UNSAFE = new AtomicInteger();
    private final int hash;
    private final int id;
    private final List<IReaction<T>> reactions = new ArrayList<>();
    private final Map<IMolecule, T> molecules = new ConcurrentHashMap<>();

    /**
     * Basically, builds the node and just caches the hash code.
     * 
     * @param threadLocal
     *            true if the id should be local to the current thread. In order
     *            to keep the node ids along multiple simulations, pass true and
     *            use different threads to instance the nodes.
     */
    protected GenericNode(final boolean threadLocal) {
        if (threadLocal) {
            id = SINGLETON.genId();
        } else {
            id = THREAD_UNSAFE.getAndIncrement();
        }
        if (id == 0) {
            hash = CENTER;
        } else {
            final boolean positive = POSITIVE.get();
            final int val = positive ? MAX : MIN;
            final int pow = POW.get();
            final int odd = ODD.get();
            hash = val / pow * odd;
            if (!positive) {
                if (odd + 2 > pow) {
                    POW.set(pow * 2);
                    ODD.set(1);
                } else {
                    ODD.set(odd + 2);
                }
            }
            POSITIVE.set(!positive);
        }
    }

    @Override
    public void addReaction(final IReaction<T> r) {
        reactions.add(r);
    }

    @Override
    public int compareTo(final INode<T> o) {
        if (o instanceof GenericNode<?>) {
            if (id > ((GenericNode<?>) o).id) {
                return 1;
            }
            if (id < ((GenericNode<?>) o).id) {
                return -1;
            }
        }
        return 0;
    }

    @Override
    public boolean contains(final IMolecule m) {
        return molecules.containsKey(m);
    }

    /**
     * @return an empty concentration
     */
    protected abstract T createT();

    @Override
    public boolean equals(final Object o) {
        if (o instanceof GenericNode<?>) {
            return ((GenericNode<?>) o).id == id;
        }
        return false;
    }

    @Override
    public int getChemicalSpecies() {
        return molecules.size();
    }

    @Override
    public T getConcentration(final IMolecule mol) {
        final T res = molecules.get(mol);
        if (res == null) {
            return createT();
        }
        return res;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public List<IReaction<T>> getReactions() {
        return reactions;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public Iterator<IReaction<T>> iterator() {
        return reactions.iterator();
    }

    @Override
    public void removeReaction(final IReaction<T> r) {
        reactions.remove(r);
    }

    @Override
    public void setConcentration(final IMolecule mol, final T c) {
        molecules.put(mol, c);
    }

    @Override
    public void removeConcentration(final IMolecule mol) {
        molecules.remove(mol);
    }

    @Override
    public String toString() {
        return molecules.toString();
    }

    @Override
    public void forEach(final Consumer<? super IReaction<T>> action) {
        reactions.forEach(action);
    }

    @Override
    public Spliterator<IReaction<T>> spliterator() {
        return reactions.spliterator();
    }

    @Override
    public Map<IMolecule, T> getContents() {
        return Collections.unmodifiableMap(molecules);
    }

}
