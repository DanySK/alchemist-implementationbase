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

import it.unibo.alchemist.model.interfaces.IAction;
import it.unibo.alchemist.model.interfaces.IMolecule;
import it.unibo.alchemist.model.interfaces.INode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * An abstract class facility with some generic methods implemented.
 * 
 * @author Danilo Pianini
 * @param <T>
 */
public abstract class AbstractAction<T> implements IAction<T> {

	private static final long serialVersionUID = 1858501940105283451L;
	private final List<IMolecule> influenced = new ArrayList<IMolecule>(1);
	private final INode<T> n;

	/**
	 * Call this constructor in the subclasses in order to automatically
	 * instance the node.
	 * 
	 * @param node
	 *            the node this action belongs to
	 */
	protected AbstractAction(final INode<T> node) {
		this.n = node;
	}

	/**
	 * @return the node this action belongs to
	 */
	public INode<T> getNode() {
		return n;
	}

	@Override
	public List<? extends IMolecule> getModifiedMolecules() {
		return influenced;
	}

	/**
	 * Allows to add an IMolecule to the set of molecules which are modified by
	 * this action. This method must be called in the constructor, and not
	 * during the execution.
	 * 
	 * @param m
	 *            the molecule which will be modified
	 */
	protected void addModifiedMolecule(final IMolecule m) {
		influenced.add(m);
	}
	
	/**
	 * @param m
	 *            the molecule
	 * @return true if the local node contains the molecule
	 */
	protected final boolean nodeContains(final IMolecule m) {
		return getNode().contains(m);
	}

	/**
	 * @param m
	 *            the molecule
	 * @return An {@link Optional} with the value of concentration, or an empty
	 *         {@link Optional} if the molecule if
	 *         {@link INode#getConcentration(IMolecule)} returns null
	 */
	protected final Optional<T> getConcentration(final IMolecule m) {
		return Optional.ofNullable(getNode().getConcentration(m));
	}

}
