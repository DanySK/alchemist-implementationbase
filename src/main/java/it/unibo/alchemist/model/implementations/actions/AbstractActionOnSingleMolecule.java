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

import it.unibo.alchemist.model.interfaces.IMolecule;
import it.unibo.alchemist.model.interfaces.INode;

/**
 * This class offers the basic structures to provide operations with numeric
 * concentrations on a single molecule.
 * 
 * @author Danilo Pianini
 * @param <T>
 */
public abstract class AbstractActionOnSingleMolecule<T> extends AbstractAction<T> {

	private static final long serialVersionUID = 5506733553861927362L;
	private final IMolecule mol;

	/**
	 * Call this constructor in the subclasses in order to automatically
	 * instance the node, the molecules and the dependency managing facilities.
	 * 
	 * @param node
	 *            the node this action belongs to
	 * @param molecule
	 *            the molecule which whose concentration will be modified y the
	 *            execution of this action
	 */
	protected AbstractActionOnSingleMolecule(final INode<T> node,
			final IMolecule molecule) {
		super(node);
		this.mol = molecule;
		addModifiedMolecule(molecule);
	}

	/**
	 * @return the molecule which whose concentration will be modified y the
	 *         execution of this action
	 */
	public IMolecule getMolecule() {
		return mol;
	}

}
