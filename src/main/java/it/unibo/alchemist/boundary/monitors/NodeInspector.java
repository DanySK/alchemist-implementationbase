/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.monitors;

import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IMolecule;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IPosition;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;
import it.unibo.alchemist.model.interfaces.Incarnation;
import it.unibo.alchemist.utils.L;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.ArrayUtils;
import org.danilopianini.lang.CollectionWithCurrentElement;
import org.danilopianini.lang.HashUtils;
import org.danilopianini.lang.ImmutableCollectionWithCurrentElement;
import org.danilopianini.view.ExportForGUI;
import org.reflections.Reflections;

/**
 * @author Danilo Pianini
 *
 * @param <T>
 */
@ExportInspector
public class NodeInspector<T> extends AbstractNodeInspector<T> {
	
	private static final long serialVersionUID = 6602681089557080486L;

	private static final List<Incarnation> INCARNATIONS = new LinkedList<>();
	
	@ExportForGUI(nameToExport = "Incarnation")
	private transient CollectionWithCurrentElement<Incarnation> incarnation = new ImmutableCollectionWithCurrentElement<>(INCARNATIONS, INCARNATIONS.get(0));
	@ExportForGUI(nameToExport = "Track position")
	private boolean trackPos;
	@ExportForGUI(nameToExport = "Molecule")
	private String molecule = "";
	@ExportForGUI(nameToExport = "Properties")
	private String property = "";
	@ExportForGUI(nameToExport = "Property separators")
	private String propertySeparators = " ;,:";

	private String propertyCache;
	private String lsaCache;
	private transient IMolecule mol;
	private final List<String> properties = new LinkedList<>();

	static {
		final Reflections reflections = new Reflections("it.unibo.alchemist");
		for (final Class<? extends Incarnation> clazz : reflections.getSubTypesOf(Incarnation.class)) {
			try {
				INCARNATIONS.add(clazz.newInstance());
			} catch (InstantiationException e) {
				L.warn(e);
			} catch (IllegalAccessException e) {
				L.warn(e);
			}
		}
	}
	
	@Override
	protected double[] getProperties(final IEnvironment<T> env, final INode<T> sample, final IReaction<T> r, final ITime time, final long step) {
		if (!HashUtils.pointerEquals(propertyCache, property)) {
			propertyCache = property;
			properties.clear();
			final StringTokenizer tk = new StringTokenizer(propertyCache, propertySeparators);
			while (tk.hasMoreElements()) {
				properties.add(tk.nextToken());
			}
		}
		if (!HashUtils.pointerEquals(lsaCache, molecule)) {
			lsaCache = molecule;
			mol = incarnation.getCurrent().createMolecule(lsaCache);
		}
		final double[] base;
		if (trackPos) {
			final IPosition pos = env.getPosition(sample);
			base = pos.getCartesianCoordinates();
		} else {
			base = ArrayUtils.EMPTY_DOUBLE_ARRAY;
		}
		final double[] res = Arrays.copyOf(base, base.length + properties.size());
		int i = base.length;
		for (final String prop : properties) {
			res[i++] = incarnation.getCurrent().getProperty(sample, mol, prop);
		}
		return res;
	}

	/**
	 * @return the incarnation
	 */
	protected CollectionWithCurrentElement<Incarnation> getIncarnation() {
		return incarnation;
	}

	/**
	 * @param inc
	 *            the incarnation
	 */
	protected void setIncarnation(final CollectionWithCurrentElement<Incarnation> inc) {
		this.incarnation = inc;
	}

	/**
	 * @return the molecule
	 */
	protected String getMolecule() {
		return molecule;
	}

	/**
	 * @param m
	 *            the molecule
	 */
	protected void setMolecule(final String m) {
		this.molecule = m;
	}

	/**
	 * @return the property
	 */
	protected String getProperty() {
		return property;
	}

	/**
	 * @param prop the property
	 */
	protected void setProperty(final String prop) {
		this.property = prop;
	}

	/**
	 * @return the property separators
	 */ 
	protected String getPropertySeparators() {
		return propertySeparators;
	}

	/**
	 * @param pSep the property separators
	 */
	protected void setPropertySeparators(final String pSep) {
		this.propertySeparators = pSep;
	}
	
	private void readObject(final ObjectInputStream s) throws ClassNotFoundException, IOException {
		s.defaultReadObject();
		incarnation = new ImmutableCollectionWithCurrentElement<Incarnation>(INCARNATIONS, INCARNATIONS.get(0));
	}

}
