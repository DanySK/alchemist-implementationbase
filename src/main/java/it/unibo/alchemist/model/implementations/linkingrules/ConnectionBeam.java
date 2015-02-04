/*
 * Copyright (C) 2010-2014, Danilo Pianini and contributors
 * listed in the project's pom.xml file.
 * 
 * This file is part of Alchemist, and is distributed under the terms of
 * the GNU General Public License, with a linking exception, as described
 * in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.implementations.linkingrules;

import static org.apache.commons.math3.util.FastMath.PI;
import static org.apache.commons.math3.util.FastMath.atan2;
import static org.apache.commons.math3.util.FastMath.cos;
import static org.apache.commons.math3.util.FastMath.nextAfter;
import static org.apache.commons.math3.util.FastMath.nextUp;
import static org.apache.commons.math3.util.FastMath.sin;
import it.unibo.alchemist.model.implementations.neighborhoods.Neighborhood;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.IEnvironment2DWithObstacles;
import it.unibo.alchemist.model.interfaces.INeighborhood;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IObstacle2D;
import it.unibo.alchemist.model.interfaces.IPosition;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.danilopianini.lang.HashUtils;

public class ConnectionBeam<T> extends EuclideanDistance<T> {

	private static final long serialVersionUID = -6303232843110524434L;
	private final double range;
	private IEnvironment2DWithObstacles<IObstacle2D, T> oenv;
	private final Area obstacles = new Area();

	public ConnectionBeam(final double radius, final double beamSize) {
		super(radius);
		range = beamSize;
	}

	@SuppressWarnings("unchecked")
	@Override
	public INeighborhood<T> computeNeighborhood(final INode<T> center, final IEnvironment<T> env) {
		final INeighborhood<T> normal = super.computeNeighborhood(center, env);
		if(!HashUtils.pointerEquals(env, oenv)) {
			if(! (env instanceof IEnvironment2DWithObstacles<?, ?>)) {
				return normal;
			}
			oenv = (IEnvironment2DWithObstacles<IObstacle2D, T>) env;
			obstacles.reset();
			oenv.getObstacles().forEach((obs) -> {
				/*
				 * Doubles are prone to approximation errors. Use nextAfter to get rid of them
				 */
				final Rectangle2D bounds = obs.getBounds2D();
				final double mx = nextAfter(bounds.getMinX(), java.lang.Double.NEGATIVE_INFINITY);
				final double my = nextAfter(bounds.getMinY(), java.lang.Double.NEGATIVE_INFINITY);
				final double ex = nextUp(bounds.getMaxX());
				final double ey = nextUp(bounds.getMaxY());
				obstacles.add(new Area(new Rectangle2D.Double(mx, my, ex-mx, ey-my)));
			});
		}
		if (!normal.isEmpty()) {
			final IPosition cp = env.getPosition(center);
			final List<INode<T>> neighs = normal.getNeighbors().parallelStream().filter((neigh) -> {
				final IPosition np = env.getPosition(neigh);
				return !oenv.intersectsObstacle(cp, np) || projectedBeamOvercomesObstacle(cp, np);
			}).collect(Collectors.toList());
			return new Neighborhood<>(center, neighs, env);
		}
		return normal;
	}

	protected boolean projectedBeamOvercomesObstacle(final IPosition pos1, final IPosition pos2) {
		final double p1x = pos1.getCoordinate(0);
		final double p1y = pos1.getCoordinate(1);
		final double p2x = pos2.getCoordinate(0);
		final double p2y = pos2.getCoordinate(1);
		final double x = p2x - p1x;
		final double y = p2y - p1y;
		/*
		 * Compute the angle
		 */
		final double angle = atan2(y, x);
		/*
		 * Deduce surrounding beam vertices
		 */
		final double dx = range * cos(PI / 2 + angle);
		final double dy = range * sin(PI / 2 + angle);
		/*
		 * Enlarge the beam
		 */
		final double cx = range * cos(angle);
		final double cy = range * sin(angle);
		/*
		 * Create the beam
		 */
		final Path2D.Double beamShape = new Path2D.Double();
		beamShape.moveTo(p1x+dx-cx, p1y+dy-cy);
		beamShape.lineTo(p1x-dx-cx, p1y-dy-cy);
		beamShape.lineTo(p2x-dx+cx, p2y-dy+cy);
		beamShape.lineTo(p2x+dx+cx, p2y+dy+cy);
		beamShape.closePath();
		final Area beam = new Area(beamShape);
		/*
		 * Perform subtraction
		 */
		beam.subtract(obstacles);
		/*
		 * Rebuild single areas
		 */
		final List<Path2D.Double> subareas = new ArrayList<>();
		Path2D.Double curpath = new Path2D.Double();
		final PathIterator pi = beam.getPathIterator(null);
		final double[] coords = new double[6];
		while(!pi.isDone()) {
			switch(pi.currentSegment(coords)) {
			case PathIterator.SEG_MOVETO :
				curpath = new Path2D.Double();
				curpath.moveTo(coords[0], coords[1]);
				break;
			case PathIterator.SEG_LINETO :
				curpath.lineTo(coords[0], coords[1]);
				break;
			case PathIterator.SEG_CLOSE :
				curpath.closePath();
				subareas.add(curpath);
				break;
			default : throw new IllegalArgumentException();
			}
			pi.next();
		}
		/*
		 * At least one area must contain both points
		 */
		for(final Path2D.Double p: subareas) {
			if(p.contains(p1x, p1y) && p.contains(p2x, p2y)) {
				return true;
			}
		}
		return false;
	}
	
}
