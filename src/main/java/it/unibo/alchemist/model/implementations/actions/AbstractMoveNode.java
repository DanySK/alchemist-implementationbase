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
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IPosition;

/**
 * This action moves a node inside a given environment.
 * 
 * @author Danilo Pianini
 * 
 * @param <T>
 */
public abstract class AbstractMoveNode<T> extends AbstractAction<T> {

	private static final long serialVersionUID = -5867654295577425307L;
	private final IEnvironment<T> env;
	private final boolean isAbs;

	/**
	 * Builds a new move node action. By default the movements are relative.
	 * 
	 * @param environment
	 *            The environment where to move
	 * @param node
	 *            The node to which this action belongs
	 */
	protected AbstractMoveNode(final IEnvironment<T> environment, final INode<T> node) {
		this(environment, node, false);
	}

	/**
	 * @param environment
	 *            The environment where to move
	 * @param node
	 *            The node to which this action belongs
	 * @param isAbsolute
	 *            if set to true, the environment expects the movement to be
	 *            expressed in absolute coordinates. It means that, if a node in
	 *            (1,1) wants to move to (2,3), its getNextPosition() must
	 *            return (2,3). If false, a relative coordinate is expected, and
	 *            the method for the same effect must return (1,2).
	 */
	protected AbstractMoveNode(final IEnvironment<T> environment, final INode<T> node, final boolean isAbsolute) {
		super(node);
		this.env = environment;
		this.isAbs = isAbsolute;
	}

	@Override
	public void execute() {
		if (isAbs) {
			env.moveNodeToPosition(getNode(), getNextPosition());
		} else {
			env.moveNode(getNode(), getNextPosition());
		}
	}

	@Override
	public Context getContext() {
		return Context.LOCAL;
	}

	/**
	 * @return the current environment
	 */
	public IEnvironment<T> getEnvironment() {
		return env;
	}
	
	/**
	 * @return the position of the local node
	 */
	protected final IPosition getCurrentPosition() {
		return getNodePosition(getNode());
	}
	
	/**
	 * @return The next position where to move, in relative coordinates with
	 *         respect to the current node position.
	 */
	public abstract IPosition getNextPosition();

	/**
	 * Given a node, computes its position.
	 * 
	 * @param n the node
	 * @return the position of the node
	 */
	protected final IPosition getNodePosition(final INode<T> n) {
		return env.getPosition(n);
	}
	
}
