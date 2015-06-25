/*
 * Copyright (C) 2010-2015, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.monitors;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Function;

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
	@ExportForGUI(nameToExport = "Track id")
	private boolean trackId;
	@ExportForGUI(nameToExport = "Track position")
	private boolean trackPos;
	@ExportForGUI(nameToExport = "Separators")
	private String propertySeparators = " ;,:";
	@ExportForGUI(nameToExport = "Molecules")
	private String molecule = "";
	@ExportForGUI(nameToExport = "Properties")
	private String property = "";

	private String propertyCache;
	private String molCache;
	private transient List<IMolecule> mol;
	private final List<String> properties = new LinkedList<>();
	private int initSize = 1;

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
	
	private <R> void tokenize(final List<R> result, final String base, final Function<String, R> supplier) {
		final StringTokenizer tk = new StringTokenizer(base, propertySeparators);
		while (tk.hasMoreElements()) {
			result.add(supplier.apply(tk.nextToken()));
		}
	}
	
	@Override
	protected double[] getProperties(final IEnvironment<T> env, final INode<T> sample, final IReaction<T> r, final ITime time, final long step) {
		if (!HashUtils.pointerEquals(propertyCache, property)) {
			propertyCache = property;
			properties.clear();
			tokenize(properties, propertyCache, Function.identity());
		}
		if (!HashUtils.pointerEquals(molCache, molecule)) {
			molCache = molecule;
			mol = new ArrayList<>();
			tokenize(mol, molCache, s -> incarnation.getCurrent().createMolecule(s));
		}
		final TDoubleList res = new TDoubleArrayList(initSize);
		if (trackId) {
			res.add(sample.getId());
		}
		if (trackPos) {
			final IPosition pos = env.getPosition(sample);
			res.add(pos.getCartesianCoordinates());
		}
		for (final IMolecule m : mol) {
			for (final String prop : properties) {
				res.add(incarnation.getCurrent().getProperty(sample, m, prop));
			}
		}
		initSize = res.size();
		return res.toArray();
	}

	/**
	 * @return the incarnation
	 */
	public CollectionWithCurrentElement<Incarnation> getIncarnation() {
		return incarnation;
	}

	/**
	 * @param inc
	 *            the incarnation
	 */
	public void setIncarnation(final CollectionWithCurrentElement<Incarnation> inc) {
		this.incarnation = inc;
	}

	/**
	 * @return the molecule
	 */
	public String getMolecule() {
		return molecule;
	}

	/**
	 * @param m
	 *            the molecule
	 */
	public void setMolecule(final String m) {
		this.molecule = m;
	}

	/**
	 * @return the property
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * @param prop the property
	 */
	public void setProperty(final String prop) {
		this.property = prop;
	}

	/**
	 * @return the property separators
	 */ 
	public String getPropertySeparators() {
		return propertySeparators;
	}

	/**
	 * @param pSep the property separators
	 */
	public void setPropertySeparators(final String pSep) {
		this.propertySeparators = pSep;
	}
	
	private void readObject(final ObjectInputStream s) throws ClassNotFoundException, IOException {
		s.defaultReadObject();
		incarnation = new ImmutableCollectionWithCurrentElement<Incarnation>(INCARNATIONS, INCARNATIONS.get(0));
	}

}
