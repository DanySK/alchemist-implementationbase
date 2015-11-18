/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.utils;

import it.unibo.alchemist.model.implementations.positions.Continuous2DEuclidean;
import it.unibo.alchemist.model.interfaces.IObstacle2D;
import it.unibo.alchemist.utils.MathUtils;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.apache.commons.math3.util.FastMath;
import org.danilopianini.concurrency.ThreadLocalIdGenerator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static it.unibo.alchemist.utils.MathUtils.fuzzyEquals;
import static it.unibo.alchemist.utils.MathUtils.fuzzyGreaterEquals;

/**
 * This class implements a rectangular obstacle, whose sides are parallel to the
 * cartesian axis.
 * 
 * 
 */
@SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
public final class RectObstacle2D extends it.unibo.alchemist.external.com.infomatiq.jsi.Rectangle implements IObstacle2D {

    private static final long serialVersionUID = -3552947311155196461L;
    private static final ThreadLocalIdGenerator SINGLETON = new ThreadLocalIdGenerator();

    private final Rectangle2D.Double awtRect;
    private final int id;

    /**
     * This code was built upon Alexander Hristov's, see
     * http://www.ahristov.com/
     * tutorial/geometry-games/intersection-segments.html
     */
    private static double[] intersection(final double x1, final double y1, final double x2, final double y2, final double x3, final double y3, final double x4, final double y4) {
        final double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (d == 0) {
            return new double[] { x2, y2 };
        }
        /*
         * Intersection point between lines
         */
        double xi = ((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
        double yi = ((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
        /*
         * If a point is on a border, reduce it to the exact border
         */
        xi = fuzzyEquals(xi, x3) ? x3 : fuzzyEquals(xi, x4) ? x4 : xi;
        yi = fuzzyEquals(yi, y3) ? y3 : fuzzyEquals(yi, y4) ? y4 : yi;
        /*
         * Check if there is actual intersection
         */
        if (intersectionOutOfRange(xi, x1, x2) || intersectionOutOfRange(xi, x3, x4) || intersectionOutOfRange(yi, y1, y2) || intersectionOutOfRange(yi, y3, y4)) {
            return new double[] { x2, y2 };
        }
        return new double[] { xi, yi };
    }

    private static boolean intersectionOutOfRange(final double xi, final double x1, final double x2) {
        final double min = Math.min(x1, x2);
        final double max = Math.max(x1, x2);
        return !fuzzyGreaterEquals(xi, min) || !fuzzyGreaterEquals(max, xi);
    }

    @Override
    public Continuous2DEuclidean next(final double startx, final double starty, final double endx, final double endy) {
        final double[] onBorders = enforceBorders(startx, starty, endx, endy);
        if (onBorders != null) {
            /*
             * The starting point was on the border.
             */
            return new Continuous2DEuclidean(onBorders);
        }
        final double[] intersection = nearestIntersection(startx, starty, endx, endy);
        /*
         * Ensure the intersection is outside the boundaries. Force it to be.
         */
        while (contains(intersection[0], intersection[1])) {
            intersection[0] = FastMath.nextAfter(intersection[0], startx);
            intersection[1] = FastMath.nextAfter(intersection[1], starty);
        }
        final double[] restricted = enforceBorders(intersection[0], intersection[1], intersection[0], intersection[1]);
        if (restricted == null) {
            return new Continuous2DEuclidean(intersection);
        }
        return new Continuous2DEuclidean(restricted);
    }

    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS")
    private double[] enforceBorders(final double startx, final double starty, final double endx, final double endy) {
        /*
         * Check if the point is somehow inside the obstacle, and reply
         * accordingly
         */
        if (fuzzyGreaterEquals(starty, minY) && fuzzyGreaterEquals(maxY, starty) && fuzzyGreaterEquals(startx, minX) && fuzzyGreaterEquals(maxX, startx)) {
            final double[] res = new double[] { endx, endy };
            if (fuzzyEquals(startx, minX) && endx >= minX) {
                /*
                 * Left border
                 */
                res[0] = FastMath.nextAfter(minX, startx);
            } else if (fuzzyEquals(startx, maxX) && endx <= maxX) {
                /*
                 * Right border
                 */
                res[0] = FastMath.nextAfter(maxX, startx);
            }
            if (fuzzyEquals(starty, minY) && endy >= minY) {
                /*
                 * Bottom border
                 */
                res[1] = FastMath.nextAfter(minY, starty);
            } else if (fuzzyEquals(starty, maxY) && endy <= maxY) {
                /*
                 * Top border
                 */
                res[1] = FastMath.nextAfter(maxY, starty);
            }
            return res;
        }
        return null;
    }

    @Override
    @SuppressFBWarnings("FE_FLOATING_POINT_EQUALITY")
    public double[] nearestIntersection(final double startx, final double starty, final double endx, final double endy) {
        final double nearx = MathUtils.nearest(startx, maxX, minX);
        final double neary = MathUtils.nearest(starty, maxY, minY);
        final double farx = nearx == maxX ? minX : maxX;
        final double fary = neary == maxY ? minY : maxY;
        final double[] intersectionSide1 = intersection(startx, starty, endx, endy, nearx, neary, nearx, fary);
        final double[] intersectionSide2 = intersection(startx, starty, endx, endy, nearx, neary, farx, neary);
        final double d1 = MathUtils.getEuclideanDistance(intersectionSide1, new double[] { startx, starty });
        final double d2 = MathUtils.getEuclideanDistance(intersectionSide2, new double[] { startx, starty });
        if (d1 < d2) {
            return intersectionSide1;
        }
        return intersectionSide2;
    }

    /**
     * Builds a new RectObstacle2D, given a point, the width and the height.
     * 
     * @param x
     *            x coordinate of the starting point
     * @param y
     *            y coordinate of the starting point
     * @param w
     *            the rectangle width. Can be negative.
     * @param h
     *            the rectangle height. Can be negative.
     */
    public RectObstacle2D(final double x, final double y, final double w, final double h) {
        super(x, y, x + w, y + h);
        awtRect = new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
        id = SINGLETON.genId();
    }

    @Override
    public boolean contains(final double x, final double y) {
        return x >= minX && y >= minY && x <= maxX && y <= maxY;
    }

    @Override
    public boolean contains(final double x, final double y, final double w, final double h) {
        return awtRect.contains(x, y, w, h);
    }

    @Override
    public boolean contains(final Point2D p) {
        return awtRect.contains(p);
    }

    @Override
    public boolean contains(final Rectangle2D r) {
        return awtRect.contains(r);
    }

    @Override
    public Rectangle getBounds() {
        return awtRect.getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        return awtRect.getBounds2D();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public PathIterator getPathIterator(final AffineTransform at) {
        return awtRect.getPathIterator(at);
    }

    @Override
    public PathIterator getPathIterator(final AffineTransform at, final double flatness) {
        return awtRect.getPathIterator(at, flatness);
    }

    @Override
    public boolean intersects(final double x, final double y, final double w, final double h) {
        return awtRect.intersects(x, y, w, h);
    }

    @Override
    public boolean intersects(final Rectangle2D r) {
        return awtRect.intersects(r);
    }

    @Override
    public String toString() {
        return "[" + minX + "," + minY + " -> " + maxX + "," + maxY + "]";
    }

}
