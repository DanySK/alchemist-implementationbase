/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.environments;

import gnu.trove.map.hash.TIntObjectHashMap;
import it.unibo.alchemist.exceptions.UncomparableDistancesException;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IPosition;
import it.unibo.alchemist.utils.L;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;
import org.danilopianini.lang.QuadTree;

/**
 * Very generic and basic implementation for an environment. Basically, only
 * manages an internal set of nodes and their position.
 * 
 * @author Danilo Pianini
 * 
 * @param <T>
 */
public abstract class AbstractEnvironment<T> implements IEnvironment<T> {

	private static final long serialVersionUID = 2704085518489753349L;
	/**
	 * The default monitor that will be loaded. If null, the GUI must default to a compatible monitor.
	 */
	protected static final String DEFAULT_MONITOR = null;
	private final TIntObjectHashMap<IPosition> nodeToPos = new TIntObjectHashMap<>();
	private final TIntObjectHashMap<INode<T>> nodes = new TIntObjectHashMap<INode<T>>();
	private String separator = System.getProperty("line.separator");
	private QuadTree<INode<T>> spatialIndex = new QuadTree<>(0, 0, 0, 0, 10);

	/**
	 * Adds or changes a position entry in the position map.
	 * 
	 * @param n
	 *            the node
	 * @param p
	 *            its new position
	 */
	protected void setPosition(final INode<T> n, final IPosition p) {
		final IPosition pos = nodeToPos.put(n.getId(), p);
		if (pos != null && !spatialIndex.move(n, pos.getCoordinate(0), pos.getCoordinate(1), p.getCoordinate(0), p.getCoordinate(1))) {
			resetSpatialIndex();
		}
	}

	/**
	 * Updates position and other metadata for a new node. Subclasses should
	 * call this method prior to implement their own addNode()
	 * 
	 * @param node
	 *            the node to add
	 * @param p
	 *            the position
	 */
	protected void addNodeInternally(final INode<T> node, final IPosition p) {
		setPosition(node, p);
		nodes.put(node.getId(), node);
		if (!spatialIndex.insert(node, p.getCoordinate(0), p.getCoordinate(1))) {
			resetSpatialIndex();
		}
	}

	private void resetSpatialIndex() {
		final double[] offset = getOffset();
		final double[] size = getSize();
		final double minX = offset[0];
		final double minY = offset[1];
		final double maxX = FastMath.nextUp(minX + size[0]);
		final double maxY = FastMath.nextUp(minY + size[1]);
		spatialIndex = new QuadTree<>(minX, minY, maxX, maxY, 10);
		for (final INode<T> n : nodes.valueCollection()) {
			final IPosition p = getPosition(n);
			if (!spatialIndex.insert(n, p.getCoordinate(0), p.getCoordinate(1))) {
				L.warn("Environment size computation is broken.");
			}
		}
	}

	/**
	 * Deletes a position from the map.
	 * 
	 * @param node
	 *            the node whose position will be removed
	 * @return the position removed
	 */
	protected IPosition getAndDeletePosition(final INode<T> node) {
		return nodeToPos.remove(node.getId());
	}

	@Override
	public IPosition getPosition(final INode<T> node) {
		return nodeToPos.get(node.getId());
	}

	@Override
	public void removeNode(final INode<T> node) {
		nodes.remove(node.getId());
		final IPosition pos = nodeToPos.remove(node.getId());
		spatialIndex.delete(node, pos.getCoordinate(0), pos.getCoordinate(1));
	}

	@Override
	public double getDistanceBetweenNodes(final INode<T> n1, final INode<T> n2) {
		final IPosition p1 = getPosition(n1);
		final IPosition p2 = getPosition(n2);
		if (p1 != null && p2 != null) {
			try {
				return p1.getDistanceTo(p2);
			} catch (UncomparableDistancesException e) {
				L.warn("Uncomparable distances. NaN will be returned, see the stacktrace below to fix.");
				L.warn(e);
			}
		}
		L.warn("One or both nodes are not in the environment. NaN will be returned.");
		return Double.NaN;
	}

	@Override
	public int getNodesNumber() {
		return nodes.size();
	}

	@Override
	public Collection<INode<T>> getNodes() {
		return nodes.valueCollection();
	}

	@Override
	public INode<T> getNodeByID(final int id) {
		return nodes.get(id);
	}

	@Override
	public Iterator<INode<T>> iterator() {
		return nodes.valueCollection().iterator();
	}

	@Override
	public List<INode<T>> getNodesWithinRange(final INode<T> center, final double range) {
		/*
		 * Remove the center node
		 */
		return getAllNodesInRange(getPosition(center), range).filter((n) -> !n.equals(center)).collect(Collectors.toList());
	}

	private Stream<INode<T>> getAllNodesInRange(final IPosition center, final double range) {
		final Pair<IPosition, IPosition> boundingBox = center.buildBoundingBox(range);
		final double[] bl = boundingBox.getFirst().getCartesianCoordinates();
		final IPosition ul = boundingBox.getSecond();
		final double rx = ul.getCoordinate(0) - bl[0];
		final double ry = ul.getCoordinate(1) - bl[1];
		return spatialIndex.query(new Rectangle2D.Double(bl[0], bl[1], rx, ry)).parallelStream().filter((n) -> getPosition(n).getDistanceTo(center) <= range);
	}

	@Override
	public List<INode<T>> getNodesWithinRange(final IPosition center, final double range) {
		/*
		 * Collect every node in range
		 */
		return getAllNodesInRange(center, range).collect(Collectors.toList());
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final INode<T> n : this) {
			sb.append(n + separator);
		}
		return sb.toString();
	}

	/**
	 * @return the separator used in toString()
	 */
	public String getSeparator() {
		return separator;
	}

	/**
	 * @param s
	 *            the new separator
	 */
	public void setSeparator(final String s) {
		separator = s;
	}

	@Override
	public String getPreferredMonitor() {
		return DEFAULT_MONITOR;
	}
	
	@Override
	public void forEach(final Consumer<? super INode<T>> action) {
		getNodes().forEach(action);
	}
	
	@Override
	public Spliterator<INode<T>> spliterator() {
		return getNodes().spliterator();
	}
}
