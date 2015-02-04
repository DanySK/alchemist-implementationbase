/**
 * 
 */
package it.unibo.alchemist.boundary.monitors;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.danilopianini.lang.CollectionWithCurrentElement;
import org.danilopianini.lang.HashUtils;
import org.danilopianini.lang.ImmutableCollectionWithCurrentElement;
import org.danilopianini.view.ExportForGUI;
import org.reflections.Reflections;

import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IMolecule;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;
import it.unibo.alchemist.model.interfaces.Incarnation;
import it.unibo.alchemist.utils.L;

/**
 * @author Danilo Pianini
 */
@ExportInspector
public class NodeInspector<T> extends AbstractNodeInspector<T> {
	
	private static final long serialVersionUID = 6602681089557080486L;

	private static final List<Incarnation> INCARNATIONS = new LinkedList<>();
	
	@ExportForGUI(nameToExport="Incarnation")
	private transient CollectionWithCurrentElement<Incarnation> incarnation = new ImmutableCollectionWithCurrentElement<>(INCARNATIONS, INCARNATIONS.get(0));
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
		final Reflections REFLECTIONS = new Reflections("it.unibo.alchemist");
		for(final Class<? extends Incarnation> clazz: REFLECTIONS.getSubTypesOf(Incarnation.class)) {
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
		if (mol != null) {
			final double[] res = new double[properties.size()];
			int i = 0;
			for (final String prop : properties) {
				res[i++] = incarnation.getCurrent().getProperty(sample, mol, prop);
			}
			return res;
		}
		return new double[0];
	}

	protected CollectionWithCurrentElement<Incarnation> getIncarnation() {
		return incarnation;
	}

	protected void setIncarnation(final CollectionWithCurrentElement<Incarnation> incarnation) {
		this.incarnation = incarnation;
	}

	protected String getLsa() {
		return molecule;
	}

	protected void setLsa(final String lsa) {
		this.molecule = lsa;
	}

	protected String getProperty() {
		return property;
	}

	protected void setProperty(final String property) {
		this.property = property;
	}

	protected String getPropertySeparators() {
		return propertySeparators;
	}

	protected void setPropertySeparators(final String propertySeparators) {
		this.propertySeparators = propertySeparators;
	}
	
	private void readObject(final ObjectInputStream s) throws ClassNotFoundException, IOException {
		s.defaultReadObject();
		incarnation = new ImmutableCollectionWithCurrentElement<Incarnation>(INCARNATIONS, INCARNATIONS.get(0));
	}

}
