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
package it.unibo.alchemist.model.implementations.neighborhoods;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import it.unibo.alchemist.exceptions.UncomparableDistancesException;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.INeighborhood;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IPosition;
import it.unibo.alchemist.utils.L;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A simple implementation of a neighborhood.
 * 
 * @author Danilo Pianini
 * 
 * @param <T>
 *            The type which describes the concentration of a molecule
 */
public final class Neighborhood<T> implements INeighborhood<T> {

	private static final long serialVersionUID = 2810393824506583972L;
	private final INode<T> c;
	private final IEnvironment<T> env;
	private final TIntList kCache;
	private final List<INode<T>> k;

	/**
	 * Builds a new neighborhood given a central node, its neighbors and the
	 * environment.
	 * 
	 * @param center
	 *            the central node
	 * @param nodes
	 *            the neighbors of the central node
	 * @param environment
	 *            the environment
	 */
	public Neighborhood(final INode<T> center, final Collection<? extends INode<T>> nodes, final IEnvironment<T> environment) {
		this.c = center;
		this.env = environment;
		kCache = new TIntArrayList(nodes.size());
		k = new ArrayList<>(nodes.size());
		for (final INode<T> n : nodes) {
			kCache.add(n.getId());
			k.add(n);
		}
		Collections.sort(k);
		kCache.sort();
	}

	/**
	 * Builds a new neighborhood given a central node, its neighbors and the
	 * environment.
	 * 
	 * @param center
	 *            the central node
	 * @param nodes
	 *            the neighbors of the central node
	 * @param environment
	 *            the environment
	 */
	private Neighborhood(final INode<T> center, final TIntList map, final List<INode<T>> l, final IEnvironment<T> environment) {
		this.c = center;
		this.env = environment;
		kCache = map;
		k = l;
	}

	@Override
	public void addNeighbor(final INode<T> neigh) {
		if (!contains(neigh)) {
			int low = 0;
			int high = kCache.size() - 1;
			final int value = neigh.getId();
			while (low <= high) {
				final int mid = (low + high) >>> 1;
				final int midVal = kCache.get(mid);
				if (midVal < value) {
					low = mid + 1;
				} else if (midVal > value) {
					high = mid - 1;
				} else {
					break;
				}
			}
			kCache.insert(low, value);
			k.add(low, neigh);
		}
	}

	@Override
	public Neighborhood<T> clone() throws CloneNotSupportedException {
		return new Neighborhood<T>(c, new TIntArrayList(kCache), new ArrayList<>(k), env);
	}

	@Override
	public boolean contains(final INode<T> n) {
		return contains(n.getId());
	}

	@Override
	public boolean contains(final int n) {
		return kCache.binarySearch(n) >= 0;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof INeighborhood<?>) {
			final INeighborhood<?> n = (INeighborhood<?>) obj;
			return c.equals(n.getCenter()) && getNeighbors().equals(n.getNeighbors());
		}
		return false;
	}

	@Override
	public Set<INode<T>> getBetweenRange(final double min, final double max) {
		final Set<INode<T>> res = new LinkedHashSet<>(k.size() + 1, 1f);
		final IPosition centerposition = env.getPosition(c);
		for (final INode<T> n : k) {
			try {
				final double dist = centerposition.getDistanceTo(env.getPosition(n));
				if (dist < max && dist > min) {
					res.add(n);
				}
			} catch (UncomparableDistancesException e) {
				L.error(e);
			}
		}
		return res;
	}

	@Override
	public INode<T> getCenter() {
		return c;
	}

	@Override
	public INode<T> getNeighborById(final int id) {
		return k.get(kCache.binarySearch((id)));
	}

	@Override
	public INode<T> getNeighborByNumber(final int num) {
		return k.get(num % size());
	}

	@Override
	public List<? extends INode<T>> getNeighbors() {
		return Collections.unmodifiableList(k);
	}

	@Override
	public int hashCode() {
		return c.hashCode() ^ k.hashCode();
	}

	@Override
	public boolean isEmpty() {
		return k.isEmpty();
	}

	@Override
	public Iterator<INode<T>> iterator() {
		return k.iterator();
	}

	@Override
	public void removeNeighbor(final INode<T> neighbor) {
		kCache.remove(neighbor.getId());
		k.remove(neighbor);
	}

	@Override
	public int size() {
		return k.size();
	}

	@Override
	public String toString() {
		return c + " links " + kCache;
	}

	@Override
	public void forEach(final Consumer<? super INode<T>> arg0) {
		k.forEach(arg0);
	}

	@Override
	public Spliterator<INode<T>> spliterator() {
		return k.spliterator();
	}

}
