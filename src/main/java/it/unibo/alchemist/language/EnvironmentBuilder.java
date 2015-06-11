/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.language;

import static it.unibo.alchemist.utils.L.debug;
import static it.unibo.alchemist.utils.L.error;
import static it.unibo.alchemist.utils.L.warn;
import it.unibo.alchemist.external.cern.jet.random.engine.RandomEngine;
import it.unibo.alchemist.model.implementations.times.DoubleTime;
import it.unibo.alchemist.model.interfaces.IAction;
import it.unibo.alchemist.model.interfaces.IConcentration;
import it.unibo.alchemist.model.interfaces.ICondition;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.ILinkingRule;
import it.unibo.alchemist.model.interfaces.IMolecule;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IPosition;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;
import it.unibo.alchemist.model.interfaces.TimeDistribution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.danilopianini.lang.Pair;
import org.danilopianini.lang.PrimitiveUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Danilo Pianini
 * @author Enrico Galassi
 * 
 * @param <T>
 *            concentration type
 */
public class EnvironmentBuilder<T> {

	private static final String DEFAULT_PACKAGE = "it.unibo.alchemist.";
	private static final String LINKINGRULES_DEFAULT_PACKAGE = DEFAULT_PACKAGE + "model.implementations.linkingrules.";
	private static final String NAME = "name";
	private static final String REACTIONS_DEFAULT_PACKAGE = DEFAULT_PACKAGE + "model.implementations.reactions.";
	private static final String TEXT = "#text";
	private static final String TYPE = "type";
	private static final Class<?>[] TYPES = new Class<?>[] { List.class, Integer.TYPE, Double.TYPE, Boolean.TYPE, Character.TYPE, Byte.TYPE, Short.TYPE, Long.TYPE, Float.TYPE };
	private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();
	private static final List<Pair<Class<?>[], Function<Number, ?>>> NUMBER_CASTER = new LinkedList<>();
	
	private Class<?> concentrationClass;
	private final Random internalRandom = new Random();
	private final Semaphore mutex = new Semaphore(1);
	private final Semaphore envMutex = new Semaphore(0);
	private final Semaphore randMutex = new Semaphore(0);
	private Class<?> positionClass;
	private RandomEngine random;
	private IEnvironment<T> result;
	private final InputStream xmlFile;

	static {
		NUMBER_CASTER.add(new Pair<>(new Class<?>[] {Byte.class,  Byte.TYPE}, Number::byteValue));
		NUMBER_CASTER.add(new Pair<>(new Class<?>[] {Short.class,  Short.TYPE}, Number::shortValue));
		NUMBER_CASTER.add(new Pair<>(new Class<?>[] {Integer.class,  Integer.TYPE}, Number::intValue));
		NUMBER_CASTER.add(new Pair<>(new Class<?>[] {Long.class,  Long.TYPE}, Number::longValue));
		NUMBER_CASTER.add(new Pair<>(new Class<?>[] {Float.class,  Float.TYPE}, Number::floatValue));
		NUMBER_CASTER.add(new Pair<>(new Class<?>[] {Double.class,  Double.TYPE}, Number::doubleValue));
	}

	/**
	 * Builds a new XML interpreter.
	 * 
	 * @param xmlFilePath
	 *            the file to interpret
	 * @throws FileNotFoundException 
	 */
	public EnvironmentBuilder(final String xmlFilePath) throws FileNotFoundException {
		this(new File(xmlFilePath));
	}

	/**
	 * Builds a new XML interpreter.
	 * 
	 * @param xml
	 *            the file to interpret
	 * @throws FileNotFoundException 
	 */
	public EnvironmentBuilder(final File xml) throws FileNotFoundException {
		this(new FileInputStream(xml));
	}

	/**
	 * Builds a new XML interpreter.
	 * 
	 * @param xmlStream
	 *            the input stream to interpret
	 */
	public EnvironmentBuilder(final InputStream xmlStream) {
		xmlFile = xmlStream;
	}

	private IAction<T> buildAction(final Node son, final Map<String, Object> env) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		return buildK(son, env, "it.unibo.alchemist.model.implementations.actions.");
	}

	private T buildConcentration(final Node son, final Map<String, Object> subenv) throws InstantiationException, IllegalAccessException, InvocationTargetException {
		if (concentrationClass != null) {
			final String args = son.getNodeValue();
			final ArrayList<String> arguments = new ArrayList<String>(1);
			if (!args.isEmpty()) {
				arguments.add(args);
			}
			final List<Constructor<IConcentration<T>>> list = unsafeExtractConstructors(concentrationClass);
			return tryToBuild(list, arguments, subenv, random).getContent();
		}
		error("concentration class not yet defined");
		return null;
	}

	private ICondition<T> buildCondition(final Node son, final Map<String, Object> env) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		return buildK(son, env, "it.unibo.alchemist.model.implementations.conditions.");
	}

	private TimeDistribution<T> buildTimeDist(final Node son, final Map<String, Object> env) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		return buildK(son, env, "it.unibo.alchemist.model.implementations.probabilitydistributions.");
	}

	private <K> K buildK(final Node son, final Map<String, Object> env, final String defaultPackage) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		final NamedNodeMap attributes = son.getAttributes();
		final Node typeNode = attributes.getNamedItem(TYPE);
		String type;
		if (typeNode == null) {
			type = "";
		} else {
			type = typeNode.getNodeValue();
			type = type.contains(".") ? type : defaultPackage + type;
		}
		return coreOperations(env, son, type, random);
	}

	/**
	 * Actually builds the environment given the AST built in the constructor.
	 * 
	 * @throws InstantiationException
	 *             malformed XML
	 * @throws IllegalAccessException
	 *             malformed XML
	 * @throws InvocationTargetException
	 *             malformed XML
	 * @throws ClassNotFoundException
	 *             your classpath does not include all the classes you are using
	 * @throws IOException
	 *             if there is an error reading the file
	 * @throws SAXException
	 *             if the XML is not correctly formatted
	 * @throws ParserConfigurationException
	 *             should not happen.
	 */
	public void buildEnvironment() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, SAXException, IOException, ParserConfigurationException {
		mutex.acquireUninterruptibly();
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder builder = factory.newDocumentBuilder();
		final Document doc = builder.parse(xmlFile);
		debug("Starting processing");
		random = null;
		final Node root = doc.getFirstChild();
		if (root.getNodeName().equals("environment") && doc.getChildNodes().getLength() == 1) {
			final NamedNodeMap atts = root.getAttributes();
			String type = atts.getNamedItem(TYPE).getNodeValue();
			type = type.contains(".") ? type : "it.unibo.alchemist.model.implementations.environments." + type;
			result = coreOperations(new ConcurrentHashMap<String, Object>(), root, type, null);
			synchronized (result) {
				envMutex.drainPermits();
				envMutex.release();
				final Node nameNode = atts.getNamedItem(NAME);
				final String name = nameNode == null ? "" : nameNode.getNodeValue();
				final Map<String, Object> env = new ConcurrentHashMap<String, Object>();
				env.put("ENV", result);
				if (!name.equals("")) {
					env.put(name, result);
				}
				final NodeList children = root.getChildNodes();
				for (int i = 0; i < children.getLength(); i++) {
					final Node son = children.item(i);
					final String kind = son.getNodeName();
					debug(kind);
					if (!kind.equals(TEXT)) {
						final Node sonNameAttr = son.getAttributes().getNamedItem(NAME);
						final String sonName = sonNameAttr == null ? "" : sonNameAttr.getNodeValue();
						Object sonInstance = null;
						if (kind.equals("molecule")) {
							sonInstance = buildMolecule(son, env);
						} else if (kind.equals("concentration")) {
							if (concentrationClass == null) {
								setConcentration(son);
							}
						} else if (kind.equals("position")) {
							if (positionClass == null) {
								setPosition(son);
							}
						} else if (kind.equals("random")) {
							setRandom(son, env);
						} else if (kind.equals("linkingrule")) {
							result.setLinkingRule(buildLinkingRule(son, env));
						} else if (kind.equals("condition")) {
							sonInstance = buildCondition(son, env);
						} else if (kind.equals("action")) {
							sonInstance = buildAction(son, env);
						} else if (kind.equals("reaction")) {
							sonInstance = buildReaction(son, env);
						} else if (kind.equals("node")) {
							final INode<T> node = buildNode(son, env);
							final IPosition pos = buildPosition(son, env);
							sonInstance = node;
							result.addNode(node, pos);
						} else if (kind.equals("time")) {
							sonInstance = buildTime(son, env);
						}
						if (sonInstance != null) {
							env.put(sonName, sonInstance);
						}
					}
				}
				/*
				 * This operation forces a reset to the random generator. It
				 * ensures that if the user reloads the same random seed she
				 * passed in the specification, the simulation will still be
				 * reproducible.
				 */
				random.setSeed(random.getSeed());
			}
		} else {
			error("XML does not contain one and one only environment.");
		}
		mutex.release();
	}

	private ILinkingRule<T> buildLinkingRule(final Node rootLinkingRule, final Map<String, Object> env) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		final NamedNodeMap attributes = rootLinkingRule.getAttributes();
		// final Node nameNode = attributes.getNamedItem(NAME);
		final String name = "LINKINGRULE";
		String type = attributes.getNamedItem(TYPE).getNodeValue();
		type = type.contains(".") ? type : LINKINGRULES_DEFAULT_PACKAGE + type;
		final ILinkingRule<T> res = coreOperations(env, rootLinkingRule, type, random);
		env.put(name, res);
		return res;
	}

	private IMolecule buildMolecule(final Node son, final Map<String, Object> env) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		final NamedNodeMap attributes = son.getAttributes();
		String type = attributes.getNamedItem(TYPE).getNodeValue();
		type = type.contains(".") ? type : "it.unibo.alchemist.model.implementations.molecules." + type;
		return coreOperations(env, son, type, random);
	}

	private INode<T> buildNode(final Node rootNode, final Map<String, Object> env) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		final NamedNodeMap attributes = rootNode.getAttributes();
		final Node nameNode = attributes.getNamedItem(NAME);
		final String name = nameNode == null ? "" : nameNode.getNodeValue();
		String type = attributes.getNamedItem(TYPE).getNodeValue();
		type = type.contains(".") ? type : "it.unibo.alchemist.model.implementations.nodes." + type;
		final INode<T> res = coreOperations(env, rootNode, type, random);
		if (res == null) {
			error("Failed to build " + type);
		}
		env.put(name, res);
		env.put("NODE", res);
		final NodeList children = rootNode.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			final Node son = children.item(i);
			final String kind = son.getNodeName();
			if (!kind.equals(TEXT)) {
				final Node sonNameAttr = son.getAttributes().getNamedItem(NAME);
				final String sonName = sonNameAttr == null ? "" : sonNameAttr.getNodeValue();
				String objType = null;
				Object sonInstance = null;
				if (kind.equals("condition")) {
					final ICondition<T> cond = buildCondition(son, env);
					sonInstance = cond;
					objType = "CONDITION";
				} else if (kind.equals("action")) {
					final IAction<T> act = buildAction(son, env);
					sonInstance = act;
					objType = "ACTION";
				} else if (kind.equals("content")) {
					final NamedNodeMap moleculesMap = son.getAttributes();
					for (int j = 0; j < moleculesMap.getLength(); j++) {
						final Node molNode = moleculesMap.item(j);
						final String molName = molNode.getNodeName();
						debug("checking molecule " + molName);
						if (env.containsKey(molName)) {
							debug(molName + " found");
							final Object molObj = env.get(molName);
							if (molObj instanceof IMolecule) {
								debug(molName + " matches in environment");
								final IMolecule mol = (IMolecule) molObj;
								final T conc = buildConcentration(molNode, env);
								debug(molName + " concentration: " + conc);
								sonInstance = conc;
								res.setConcentration(mol, conc);
							} else {
								warn(molObj + "(class " + molObj.getClass().getCanonicalName() + " is not subclass of IMolecule!");
							}
						} else {
							warn("molecule " + molName + " is not yet defined.");
						}
					}
				} else if (kind.equals("reaction")) {
					final IReaction<T> reaction = buildReaction(son, env);
					res.addReaction(reaction);
					sonInstance = reaction;
					objType = "REACTION";
				} else if (kind.equals("time")) {
					sonInstance = buildTime(son, env);
				} else if (kind.equals("timedistribution")) {
					sonInstance = buildTimeDist(son, env);
					objType = "TIMEDIST";
				}
				updateEnv(sonName, objType, sonInstance, env);
			}
		}
		env.remove(name);
		env.remove("NODE");
		return res;
	}

	private void updateEnv(final String name, final String curElem, final Object elem, final Map<String, Object> env) {
		if (elem != null) {
			if (name != null) {
				env.put(name, elem);
			}
			if (curElem != null) {
				env.put(curElem, elem);
			}
		}
	}

	private IPosition buildPosition(final Node son, final Map<String, Object> env) throws InstantiationException, IllegalAccessException, InvocationTargetException {
		if (positionClass == null) {
			error("position class not yet defined.");
		} else {
			final NamedNodeMap attributes = son.getAttributes();
			final Node posNode = attributes.getNamedItem("position");
			if (posNode == null) {
				warn("a node has no position!");
			} else {
				final String args = posNode.getNodeValue();
				final StringTokenizer tk = new StringTokenizer(args, " ,;");
				final ArrayList<String> arguments = new ArrayList<String>();
				while (tk.hasMoreElements()) {
					arguments.add(tk.nextToken());
				}
				arguments.trimToSize();
				final List<Constructor<IPosition>> list = unsafeExtractConstructors(positionClass);
				return tryToBuild(list, arguments, env, random);
			}
		}
		return null;
	}

	private IReaction<T> buildReaction(final Node rootReact, final Map<String, Object> env) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		final NamedNodeMap attributes = rootReact.getAttributes();
		final Node nameNode = attributes.getNamedItem(NAME);
		final String name = nameNode == null ? "" : nameNode.getNodeValue();
		String type = attributes.getNamedItem(TYPE).getNodeValue();
		type = type.contains(".") ? type : REACTIONS_DEFAULT_PACKAGE + type;
		final IReaction<T> res = coreOperations(env, rootReact, type, random);
		if (!name.equals("")) {
			env.put(name, res);
		}
		env.put("REACTION", res);
		populateReaction(env, res, rootReact);
		return res;
	}

	private ITime buildTime(final Node son, final Map<String, Object> env) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		return buildTime(son, env, random);
	}

	/**
	 * Warning: the environment may not be completely initialized yet.
	 * 
	 * @return the environment
	 */
	public IEnvironment<T> getEnvironment() {
		envMutex.acquireUninterruptibly();
		final IEnvironment<T> res = result;
		envMutex.release();
		return res;
	}

	/**
	 * @return the random engine. It is consequently possible to force a new
	 *         seed after the environment creation.
	 */
	public RandomEngine getRandomEngine() {
		randMutex.acquireUninterruptibly();
		final RandomEngine res = random;
		randMutex.release();
		return res;
	}

	private void populateReaction(final Map<String, Object> subenv, final IReaction<T> res, final Node rootReact) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		final NodeList children = rootReact.getChildNodes();
		final ArrayList<ICondition<T>> conditions = new ArrayList<ICondition<T>>();
		final ArrayList<IAction<T>> actions = new ArrayList<IAction<T>>();
		for (int i = 0; i < children.getLength(); i++) {
			final Node son = children.item(i);
			final String kind = son.getNodeName();
			if (!kind.equals(TEXT)) {
				final Node sonNameAttr = son.getAttributes().getNamedItem(NAME);
				final String sonName = sonNameAttr == null ? "" : sonNameAttr.getNodeValue();
				Object sonInstance = null;
				if (kind.equals("condition")) {
					final ICondition<T> cond = buildCondition(son, subenv);
					conditions.add(cond);
					sonInstance = cond;
				} else if (kind.equals("action")) {
					final IAction<T> act = buildAction(son, subenv);
					actions.add(act);
					sonInstance = act;
				}
				if (sonInstance != null) {
					subenv.put(sonName, sonInstance);
				}
			}
		}
		conditions.trimToSize();
		actions.trimToSize();
		if (!conditions.isEmpty()) {
			res.setConditions(conditions);
		}
		if (!actions.isEmpty()) {
			res.setActions(actions);
		}
	}

	private void setConcentration(final Node son) throws ClassNotFoundException {
		final NamedNodeMap attributes = son.getAttributes();
		String type = attributes.getNamedItem(TYPE).getNodeValue();
		type = type.contains(".") ? type : "it.unibo.alchemist.model.implementations.concentrations." + type;
		concentrationClass = Class.forName(type);
		debug("Concentration type set to " + concentrationClass);
	}

	private void setPosition(final Node son) throws ClassNotFoundException {
		final NamedNodeMap attributes = son.getAttributes();
		String type = attributes.getNamedItem(TYPE).getNodeValue();
		if (!type.contains(".")) {
			type = "it.unibo.alchemist.model.implementations.positions." + type;
		}
		positionClass = Class.forName(type);
		debug("Position type set to " + positionClass);
	}

	private void setRandom(final Node son, final Map<String, Object> env) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		final NamedNodeMap attributes = son.getAttributes();
		String type = attributes.getNamedItem(TYPE).getNodeValue();
		String seed = attributes.getNamedItem("seed").getNodeValue();
		/*
		 * This workaround ensures compatibility with pre-PVeStA integration
		 * XMLs generated with the SAPERE DSL.
		 */
		if (type.equals("cern.jet.random.engine.MersenneTwister")) {
			type = "it.unibo.alchemist.external.cern.jet.random.engine.MersenneTwister";
		}
		type = type.contains(".") ? type : "it.unibo.alchemist.external.cern.jet.random.engine." + type;
		seed = seed.equalsIgnoreCase("RANDOM") ? Integer.toString(internalRandom.nextInt()) : seed;
		final List<String> params = new ArrayList<>(1);
		params.add(seed);
		final Class<?> randomEngineClass = Class.forName(type);
		final List<Constructor<RandomEngine>> consList = unsafeExtractConstructors(randomEngineClass);
		random = tryToBuild(consList, params, env, null);
		randMutex.drainPermits();
		randMutex.release();

	}

	/**
	 * Sets a new seed for the random engine. Thread unsafe. Handle with care.
	 * 
	 * @param seed
	 *            the new random engine seed
	 */
	public void setRandomEngineSeed(final int seed) {
		random.setSeed(seed);
	}


	private static ITime buildTime(final Node son, final Map<String, Object> env, final RandomEngine random) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		final NamedNodeMap attributes = son.getAttributes();
		String type = attributes.getNamedItem(TYPE).getNodeValue();
		type = type.contains(".") ? type : "it.unibo.alchemist.model.implementations.times." + type;
		return (ITime) coreOperations(env, son, type, random);
	}

	@SuppressWarnings("unchecked")
	private static <E> E coreOperations(final Map<String, Object> environment, final Node root, final String type, final RandomEngine random) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
		final NamedNodeMap atts = root.getAttributes();
		final Node nameNode = atts.getNamedItem(NAME);
		final String name = nameNode == null ? "" : nameNode.getNodeValue();
		if (!name.equals("") && atts.getLength() == 1 && environment.containsKey(name)) {
			return (E) environment.get(name);
		}
		final Class<?> objClass = (Class<?>) Class.forName(type);
		final List<Constructor<E>> consList = unsafeExtractConstructors(objClass);
		final ArrayList<String> params = new ArrayList<String>();
		int index = 0;
		for (Node param = atts.getNamedItem("p0"); param != null; param = atts.getNamedItem("p" + (++index))) {
			params.add(param.getNodeValue());
		}
		params.trimToSize();
		final E res = tryToBuild(consList, params, environment, random);
		environment.put(name, res);
		return res;
	}

	private static Set<Class<?>> getWrapperTypes() {
		final HashSet<Class<?>> ret = new HashSet<Class<?>>();
		ret.add(Boolean.class);
		ret.add(Character.class);
		ret.add(Byte.class);
		ret.add(Short.class);
		ret.add(Integer.class);
		ret.add(Long.class);
		ret.add(Float.class);
		ret.add(Double.class);
		ret.add(Void.class);
		return ret;
	}

	/**
	 * @param clazz
	 *            the class to test
	 * @return true if the Class is a wrapper
	 */
	public static boolean isWrapperType(final Class<?> clazz) {
		return WRAPPER_TYPES.stream().anyMatch(t -> clazz.isAssignableFrom(t));
	}

	private static Optional<Number> extractNumber(final String n) {
		long resl = 0;
		double resd = 0;
		boolean isDouble = false;
		try {
			resl = Long.parseLong(n);
		} catch (final NumberFormatException e) {
			try {
				isDouble = true;
				resd = Double.parseDouble(n);
			} catch (final NumberFormatException nested) {
				return Optional.empty();
			}
		}
		if (isDouble) {
			if (resd % 1 == 0 && resd < Long.MAX_VALUE) {
				return Optional.of((long) resd);
			}
			return Optional.of(resd);
		}
		return Optional.of(resl);
	}
	
	@SuppressWarnings("unchecked")
	private static Object parseAndCreate(final Class<?> clazz, final String val, final Map<String, Object> env, final RandomEngine random) throws InstantiationException, IllegalAccessException, InvocationTargetException {
		if (clazz.isAssignableFrom(RandomEngine.class) && val.equalsIgnoreCase("random")) {
			debug("Random detected! Class " + clazz.getSimpleName() + ", param: " + val);
			if (random == null) {
				error("Random instatiation required, but RandomEngine not yet defined.");
			}
			return random;
		}
		if (clazz.isPrimitive() || isWrapperType(clazz)) {
			debug(val + " is a primitive or a wrapper: " + clazz);
			if ((clazz.isAssignableFrom(Boolean.TYPE) || clazz.isAssignableFrom(Boolean.class)) && (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false"))) {
				return Boolean.parseBoolean(val);
			}
			/*
			 * If Number is in clazz's hierarchy
			 */
			if (PrimitiveUtils.classIsNumber(clazz)) {
				final Optional<Number> num = extractNumber(val);
				if (num.isPresent()) {
					final Optional<Number> castNum = PrimitiveUtils.castIfNeeded(clazz, num.get());
					/*
					 * If method requires Object or unsupported Number, return
					 * what was parsed.
					 */
					return castNum.orElse(num.get());
				}
			}
			if (Character.TYPE.equals(clazz) || Character.class.equals(clazz)) {
				return val.charAt(0);
			}
		}
		if (List.class.isAssignableFrom(clazz) && val.startsWith("[") && val.endsWith("]")) {
			final List<Constructor<List<?>>> l = unsafeExtractConstructors(clazz);
			@SuppressWarnings("rawtypes")
			final List list = tryToBuild(l, new ArrayList<String>(0), env, random);
			final StringTokenizer strt = new StringTokenizer(val.substring(1, val.length() - 1), ",; ");
			while (strt.hasMoreTokens()) {
				final String sub = strt.nextToken();
				final Object o = tryToParse(sub, env, random);
				if (o == null) {
					debug("WARNING: list elemnt skipped: " + sub);
				} else {
					list.add(o);
				}
			}
			return list;
		}
		debug(val + " is not a primitive: " + clazz + ". Searching it in the environment...");
		final Object o = env.get(val);
		if (o != null && clazz.isInstance(o)) {
			return o;
		}
		if (ITime.class.isAssignableFrom(clazz)) {
			return new DoubleTime(Double.parseDouble(val));
		}
		if (clazz.isAssignableFrom(String.class)) {
			debug("String detected! Passing " + val + " back.");
			return val;
		}
		debug(val + " not found or class not compatible, unable to go further.");
		return null;
	}

	private static <E> E tryToBuild(final List<Constructor<E>> consList, final List<String> params, final Map<String, Object> env, final RandomEngine random) throws InstantiationException, IllegalAccessException, InvocationTargetException {
		for (final Constructor<E> c : consList) {
			debug("Trying to build with constructor " + c);
			final Class<?>[] args = c.getParameterTypes();
			if (args.length == params.size()) {
				debug("Parameters number matches (" + args.length + ").");
				final Object[] finalArgs = new Object[args.length];
				int i = 0;
				boolean success = true;
				for (; i < args.length && success; i++) {
					final String paramVal = params.get(i);
					final Class<?> paramClass = args[i];
					finalArgs[i] = parseAndCreate(paramClass, paramVal, env, random);
					if (!paramVal.equals("null") && finalArgs[i] == null) {
						debug("Unable to use this constructor.");
						success = false;
					}
				}
				if (success && i == args.length) {
					final E result = c.newInstance(finalArgs);
					// debug("Created object " + result);
					return result;
				}
			}
		}
		throw new IllegalArgumentException("no compatible constructor find for " + params);
	}

	private static Object tryToParse(final String val, final Map<String, Object> env, final RandomEngine random) throws InstantiationException, IllegalAccessException, InvocationTargetException {
		for (final Class<?> clazz : TYPES) {
			final Object result = parseAndCreate(clazz, val, env, random);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <Klazz> List<Constructor<Klazz>> unsafeExtractConstructors(final Class<?> clazz) {
		final Constructor<?>[] constructors = clazz.getConstructors();
		final List<Constructor<Klazz>> list = new ArrayList<Constructor<Klazz>>(constructors.length);
		for (final Constructor<?> c : constructors) {
			list.add((Constructor<Klazz>) c);
		}
		return list;
	}
}
