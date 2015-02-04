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
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IMolecule;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IPosition;
import it.unibo.alchemist.model.interfaces.IReaction;

/**
 * This action moves a node inside a given environment.
 * 
 * @author Danilo Pianini
 * 
 * @param <T>
 */
@Deprecated
public class OldMoveNode<T> extends AbstractMoveNode<T> {

	private static final long serialVersionUID = -5867654295577425307L;

	private IPosition dir;
	private final IMolecule move;

	/**
	 * Builds a new move node action.
	 * 
	 * @param environment
	 *            The environment where to move
	 * @param node
	 *            The node to which this action belongs
	 * @param direction
	 *            The direction where to move
	 * @param movetag
	 *            A signal molecule which is useful to maintain dependencies
	 *            among reactions which operate physically on the environment
	 *            and may be influenced by the move, for instance those
	 *            conditions that check the number of neighborhoods. If no
	 *            conditions of this kind are present, just pass null.
	 */
	public OldMoveNode(final IEnvironment<T> environment, final INode<T> node, final IPosition direction, final IMolecule movetag) {
		super(environment, node);
		dir = direction;
		this.move = movetag;
		addModifiedMolecule(movetag);
	}

	@Override
	public IAction<T> cloneOnNewNode(final INode<T> node, final IReaction<T> reaction) {
		return new OldMoveNode<T>(getEnvironment(), node, dir, move);
	}

	@Override
	public void execute() {
		if (dir != null) {
			super.execute();
		}
	}

	@Override
	public Context getContext() {
		return Context.LOCAL;
	}

	/**
	 * @return the set direction
	 */
	public IPosition getDirection() {
		return dir;
	}

	/**
	 * @return the signal molecule
	 */
	public IMolecule getMove() {
		return move;
	}

	/**
	 * This method allows to set the direction where to move the node. This must
	 * be in relative coordinates with respect to the current node position
	 * 
	 * @param direction
	 *            the direction where to move
	 */
	protected void setDirection(final IPosition direction) {
		this.dir = direction;
	}

	@Override
	public IPosition getNextPosition() {
		return dir;
	}

}
