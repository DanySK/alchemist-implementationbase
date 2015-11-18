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
package it.unibo.alchemist.model.implementations.reactions;

import it.unibo.alchemist.model.interfaces.Context;
import it.unibo.alchemist.model.interfaces.IAction;
import it.unibo.alchemist.model.interfaces.ICondition;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IMolecule;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;
import it.unibo.alchemist.model.interfaces.TimeDistribution;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The type which describes the concentration of a molecule
 * 
 * This class offers a partial implementation of IReaction. In particular, it
 * allows to write new reaction specifying only which distribution time to adopt
 * 
 * @param <T>
 */
public abstract class AReaction<T> implements IReaction<T> {

    private static final int CENTER = 0;
    private static final AtomicInteger ID_GEN = new AtomicInteger();
    /**
     * How bigger should be the StringBuffer with respect to the previous
     * interaction
     */
    private static final byte MARGIN = 20;
    private static final int MAX = 1073741824;
    private static final int MIN = -MAX;
    /**
     * Separators for toString.
     */
    protected static final String NEXT = "next scheduled @", SEP0 = " :: ", SEP1 = " -", SEP2 = "-> ";
    private static final AtomicInteger ODD = new AtomicInteger(1);
    private static final AtomicBoolean POSITIVE = new AtomicBoolean(true);
    private static final AtomicInteger POW = new AtomicInteger(1);
    private static final long serialVersionUID = 6454665278161217867L;

    private List<? extends IAction<T>> actions = new ArrayList<IAction<T>>(0);
    private List<? extends ICondition<T>> conditions = new ArrayList<ICondition<T>>(0);
    private List<IMolecule> influencing = new ArrayList<IMolecule>(), influenced = new ArrayList<IMolecule>();

    private final int hash;
    private Context incontext = Context.LOCAL, outcontext = Context.LOCAL;
    private int stringLength = Byte.MAX_VALUE;
    private final TimeDistribution<T> dist;
    private final INode<T> node;

    /**
     * This method provides facility to clone reactions. Given a new reaction
     * where to clone and two sets of actions and conditions, it initializes the
     * given reaction. The current time of occurrence and the target node are
     * asserted by retrieving them from the passed reaction, so initialize it
     * carefully. It is useful in the implementation of the cloneOnNewNode
     * method.
     * 
     * @param conditions
     *            the list of conditions to clone on the passed reaction
     * @param actions
     *            the list of actions to clone on the passed reaction
     * @param res
     *            the target reaction
     * @param <T>
     *            The type which describes the concentration of a molecule
     */
    protected static <T> void cloneConditionsAndActions(final List<? extends ICondition<T>> conditions, final List<? extends IAction<T>> actions, final IReaction<T> res) {
        final INode<T> n = res.getNode();
        final ArrayList<ICondition<T>> c = new ArrayList<ICondition<T>>(conditions.size());
        for (final ICondition<T> cond : conditions) {
            c.add(cond.cloneOnNewNode(n));
        }
        final ArrayList<IAction<T>> a = new ArrayList<IAction<T>>(actions.size());
        for (final IAction<T> act : actions) {
            a.add(act.cloneOnNewNode(n, res));
        }
        res.setActions(a);
        res.setConditions(c);
    }

    /**
     * Builds a new reaction, starting at time t.
     * 
     * @param n
     *            the node this reaction belongs to
     * @param pd
     *            the time distribution this reaction should follow
     */
    public AReaction(final INode<T> n, final TimeDistribution<T> pd) {
        final int id = ID_GEN.getAndIncrement();
        if (id == 0) {
            hash = CENTER;
        } else {
            final boolean positive = POSITIVE.get();
            final int val = positive ? MAX : MIN;
            final int pow = POW.get();
            final int odd = ODD.get();
            hash = val / pow * odd;
            if (!positive) {
                if (odd + 2 > pow) {
                    POW.set(pow * 2);
                    ODD.set(1);
                } else {
                    ODD.set(odd + 2);
                }
            }
            POSITIVE.set(!positive);
        }
        dist = pd;
        node = n;
    }

    @Override
    public int compareTo(final IReaction<T> o) {
        return getTau().compareTo(o.getTau());
    }

    @Override
    public final boolean equals(final Object o) {
        if (o instanceof AReaction) {
            return ((AReaction<?>) o).hash == hash;
        }
        return false;
    }

    @Override
    public Context getInputContext() {
        return incontext;
    }

    @Override
    public Context getOutputContext() {
        return outcontext;
    }

    @Override
    public ITime getTau() {
        return dist.getNextOccurence();
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    /**
     * Used by sublcasses to set their input context.
     * 
     * @param c
     *            the new input context
     */
    protected void setInputContext(final Context c) {
        incontext = c;
    }

    /**
     * Used by sublcasses to set their output context.
     * 
     * @param c
     *            the new input context
     */
    protected void setOutputContext(final Context c) {
        outcontext = c;
    }

    @Override
    public String toString() {
        final StringBuilder tot = new StringBuilder(stringLength + MARGIN);
        tot.append(getReactionName());
        tot.append(SEP0);
        tot.append(NEXT);
        tot.append(getTau());
        tot.append('\n');
        for (final ICondition<T> c : getConditions()) {
            tot.append(c);
            tot.append(' ');
        }
        tot.append(SEP1);
        tot.append(getRateAsString());
        tot.append(SEP2);
        for (final IAction<T> a : getActions()) {
            tot.append(a);
            tot.append(' ');
        }
        stringLength = tot.length();
        return tot.toString();
    }

    /**
     * This method is used to provide a reaction name in toString().
     * 
     * @return the name for this reaction.
     */
    protected String getReactionName() {
        return getClass().getSimpleName();
    }

    @Override
    public final void update(final ITime curTime, final boolean executed, final IEnvironment<T> env) {
        updateInternalStatus(curTime, executed, env);
        dist.update(curTime, executed, getRate(), env);
    }

    @Override
    public final TimeDistribution<T> getTimeDistribution() {
        return dist;
    }

    /**
     * Allows subclasses to add influenced molecules.
     * 
     * @param m
     *            the influenced molecule
     */
    protected void addInfluencedMolecule(final IMolecule m) {
        influenced.add(m);
    }

    /**
     * This method gets called as soon as
     * {@link #update(ITime, boolean, IEnvironment)} is called. It is useful to
     * update the internal status of the reaction.
     * 
     * @param curTime
     *            the current simulation time
     * @param executed
     *            true if this reaction has just been executed, false if the
     *            update has been triggered due to a dependency
     * @param env
     *            the current environment
     */
    protected abstract void updateInternalStatus(ITime curTime, boolean executed, IEnvironment<T> env);

    /**
     * Allows subclasses to add influencing molecules.
     * 
     * @param m
     *            the molecule to add
     */
    protected void addInfluencingMolecule(final IMolecule m) {
        influencing.add(m);
    }

    @Override
    public boolean canExecute() {
        if (conditions == null) {
            return true;
        }
        int i = 0;
        while (i < conditions.size() && conditions.get(i).isValid()) {
            i++;
        }
        return i == conditions.size();
    }

    @Override
    public void execute() {
        for (final IAction<T> a : actions) {
            a.execute();
        }
    }

    @Override
    public List<? extends IAction<T>> getActions() {
        return actions;
    }

    @Override
    public List<? extends ICondition<T>> getConditions() {
        return conditions;
    }

    @Override
    public List<? extends IMolecule> getInfluencedMolecules() {
        return influenced;
    }

    @Override
    public List<? extends IMolecule> getInfluencingMolecules() {
        return influencing;
    }

    @Override
    public INode<T> getNode() {
        return node;
    }

    /**
     * @return a {@link String} representation of the rate
     */
    protected String getRateAsString() {
        return Double.toString(dist.getRate());
    }

    @Override
    public void setActions(final List<? extends IAction<T>> a) {
        actions = a;
        Context lessStrict = Context.LOCAL;
        influenced = new ArrayList<IMolecule>();
        for (final IAction<T> act : actions) {
            final Context condcontext = act.getContext();
            lessStrict = lessStrict.isMoreStrict(condcontext) ? condcontext : lessStrict;
            final List<? extends IMolecule> mod = act.getModifiedMolecules();
            /*
             * This check is needed because of the meaning of a null list of
             * modified molecules: it means that the reaction will influence
             * every other reaction. This must be managed directly by the
             * dependency graph, and consequently the whole reaction must have a
             * null list of modified molecules.
             */
            if (mod != null) {
                influenced.addAll(mod);
            } else {
                influenced = null;
                break;
            }
        }
        setOutputContext(lessStrict);
    }

    @Override
    public void setConditions(final List<? extends ICondition<T>> c) {
        conditions = c;
        Context lessStrict = Context.LOCAL;
        influencing = new ArrayList<IMolecule>();
        for (final ICondition<T> cond : conditions) {
            final Context condcontext = cond.getContext();
            lessStrict = lessStrict.isMoreStrict(condcontext) ? condcontext : lessStrict;
            final List<? extends IMolecule> mod = cond.getInfluencingMolecules();
            /*
             * This check is needed because of the meaning of a null list of
             * modified molecules: it means that the reaction will influence
             * every other reaction. This must be managed directly by the
             * dependency graph, and consequently the whole reaction must have a
             * null list of modified molecules.
             */
            if (mod != null) {
                influencing.addAll(mod);
            } else {
                influencing = null;
                break;
            }
        }
        setInputContext(lessStrict);
    }

}
