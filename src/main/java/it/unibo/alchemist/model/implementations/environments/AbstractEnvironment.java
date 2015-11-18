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
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IPosition;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.danilopianini.lang.SpatialIndex;

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
     * The default monitor that will be loaded. If null, the GUI must default to
     * a compatible monitor.
     */
    protected static final String DEFAULT_MONITOR = null;
    private final TIntObjectHashMap<IPosition> nodeToPos = new TIntObjectHashMap<>();
    private final TIntObjectHashMap<INode<T>> nodes = new TIntObjectHashMap<INode<T>>();
    private String separator = System.getProperty("line.separator");
    private final SpatialIndex<INode<T>> spatialIndex;

    /**
     * @param internalIndex
     *            the {@link SpatialIndex} to use in order to efficiently
     *            retrieve nodes.
     */
    protected AbstractEnvironment(final SpatialIndex<INode<T>> internalIndex) {
        assert internalIndex != null;
        spatialIndex = internalIndex;
    }

    /**
     * Adds or changes a position entry in the position map.
     * 
     * @param n
     *            the node
     * @param p
     *            its new position
     */
    protected final void setPosition(final INode<T> n, final IPosition p) {
        final IPosition pos = nodeToPos.put(n.getId(), p);
        if (pos != null && !spatialIndex.move(n, pos.getCartesianCoordinates(), p.getCartesianCoordinates())) {
            throw new IllegalArgumentException("Tried to move a node not previously present in the environment: \n"
                    + "Node: " + n + "\n" + "Requested position" + p);
        }
    }

    /**
     * This method gets called once that the basic operations for a node
     * addition have been performed by {@link AbstractEnvironment}.
     * 
     * @param node
     *            the node to add
     * @param p
     *            the position
     */
    protected abstract void nodeAdded(final INode<T> node, final IPosition p);

    /**
     * Allows subclasses to determine wether or not a {@link INode} should
     * actually get added to this environment.
     * 
     * @param node
     *            the node
     * @param p
     *            the original (requested) position
     * @return true if the node should be added to this environment, false
     *         otherwise
     */
    protected abstract boolean nodeShouldBeAdded(final INode<T> node, final IPosition p);

    /**
     * Allows subclasses to tune the actual position of a node, applying spatial
     * constrains at node addition.
     * 
     * @param node
     *            the node
     * @param p
     *            the original (requested) position
     * @return the actual position where the node should be located
     */
    protected abstract IPosition computeActualInsertionPosition(final INode<T> node, final IPosition p);

    @Override
    public final void addNode(final INode<T> node, final IPosition p) {
        if (nodeShouldBeAdded(node, p)) {
            final IPosition actualPosition = computeActualInsertionPosition(node, p);
            setPosition(node, actualPosition);
            nodes.put(node.getId(), node);
            spatialIndex.insert(node, actualPosition.getCartesianCoordinates());
            nodeAdded(node, p);
        }
    }

    /**
     * Deletes a position from the map.
     * 
     * @param node
     *            the node whose position will be removed
     * @return the position removed
     */
    protected final IPosition getAndDeletePosition(final INode<T> node) {
        return nodeToPos.remove(node.getId());
    }

    @Override
    public final IPosition getPosition(final INode<T> node) {
        return nodeToPos.get(node.getId());
    }

    /**
     * This method gets called once that the basic operations for a node removal
     * have been performed by {@link AbstractEnvironment}.
     * 
     * @param node
     *            the node to add
     * @param pos
     *            the position
     */
    protected abstract void nodeRemoved(INode<T> node, IPosition pos);

    @Override
    public final void removeNode(final INode<T> node) {
        nodes.remove(node.getId());
        final IPosition pos = nodeToPos.remove(node.getId());
        spatialIndex.remove(node, pos.getCartesianCoordinates());
        nodeRemoved(node, pos);
    }

    @Override
    public double getDistanceBetweenNodes(final INode<T> n1, final INode<T> n2) {
        final IPosition p1 = getPosition(n1);
        final IPosition p2 = getPosition(n2);
        return p1.getDistanceTo(p2);
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
        return getAllNodesInRange(getPosition(center), range).filter((n) -> !n.equals(center))
                .collect(Collectors.toList());
    }

    private Stream<INode<T>> getAllNodesInRange(final IPosition center, final double range) {
        final List<IPosition> boundingBox = center.buildBoundingBox(range);
        assert boundingBox.size() == getDimensions();
        final double[][] queryArea = new double[getDimensions()][];
        IntStream.range(0, getDimensions()).parallel()
                .forEach(i -> queryArea[i] = boundingBox.get(i).getCartesianCoordinates());
        return spatialIndex.query(queryArea).parallelStream()
                .filter((n) -> getPosition(n).getDistanceTo(center) <= range);
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
