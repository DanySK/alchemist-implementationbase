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
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IReaction;
import it.unibo.alchemist.model.interfaces.ITime;
import it.unibo.alchemist.utils.L;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.danilopianini.view.ExportForGUI;

/**
 * @param <T>
 */
public abstract class AbstractNodeInspector<T> extends EnvironmentSampler<INode<T>, T> {

    private static final long serialVersionUID = 5078169056849107817L;

    @ExportForGUI(nameToExport = "Only consider node with ID in a specific range")
    private boolean filterids;
    @ExportForGUI(nameToExport = "Range (space or minus separated)")
    private String idrange = "";
    @ExportForGUI(nameToExport = "Filter NaN values")
    private boolean filternan = true;

    private String idrangeCache;
    private int minId = Integer.MIN_VALUE;
    private int maxId = Integer.MAX_VALUE;

    @Override
    protected Iterable<INode<T>> computeSamples(final IEnvironment<T> env, final IReaction<T> r, final ITime time,
            final long step) {
        final boolean filter = filterids;
        if (filter && !idrange.equals(idrangeCache)) {
            try {
                idrangeCache = idrange;
                final StringTokenizer tk = new StringTokenizer(idrangeCache, "- ;:.,_@^?=)(/&%$!|\\");
                if (tk.hasMoreElements()) {
                    minId = Integer.parseInt(tk.nextToken());
                    if (tk.hasMoreElements()) {
                        maxId = Integer.parseInt(tk.nextToken());
                    }
                }
            } catch (NumberFormatException e) {
                L.warn(e);
            }
        }
        final int fminId = minId;
        final int fmaxId = maxId;
        return env.getNodes().stream()
                .filter(node -> fminId <= node.getId() && node.getId() <= fmaxId)
                .collect(ArrayList::new, (l, el) -> l.add(el), (l1, l2) -> l1.addAll(l2));
    }

    /**
     * @return filterids
     */
    public boolean isFilteringIDs() {
        return filterids;
    }

    /**
     * @param f
     *            filterids
     */
    public void setFilterids(final boolean f) {
        this.filterids = f;
    }

    /**
     * @return idrange
     */
    public String getIdrange() {
        return idrange;
    }

    /**
     * @param range
     *            range
     */
    public void setIdrange(final String range) {
        this.idrange = range;
    }

    /**
     * @return filter nan
     */
    public boolean isFilteringNaN() {
        return filternan;
    }

    /**
     * @param f
     *            filter nan
     */
    public void setFilternan(final boolean f) {
        this.filternan = f;
    }

}
