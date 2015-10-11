package it.unibo.alchemist.test;

import static org.junit.Assert.assertArrayEquals;
import it.unibo.alchemist.model.implementations.positions.ContinuousGenericEuclidean;
import it.unibo.alchemist.model.interfaces.IPosition;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

/**
 * @author Danilo Pianini
 *
 */
public class TestContinuousGenericEuclidean {
	
	private static final Random RNG = new Random(0);

	/**
	 * @param <T> should work regardless the incarnation.
	 * 
	 * @throws InterruptedException in case of failure.
	 * @throws ExecutionException in case of failure.
	 * @throws TimeoutException in case of failure.
	 */
	@Test
	public <T> void testSum() throws InterruptedException, ExecutionException, TimeoutException {
		final double[] a1 = new double[]{RNG.nextDouble(), RNG.nextDouble(), RNG.nextDouble()};
		final double[] a2 = new double[]{RNG.nextDouble(), RNG.nextDouble(), RNG.nextDouble()};
		final double[] res = new double[]{a1[0] + a2[0], a1[1] + a2[1], a1[2] + a2[2]};
		final IPosition p1 = new ContinuousGenericEuclidean(a1);
		final IPosition p2 = new ContinuousGenericEuclidean(a2);
		assertArrayEquals(res, p1.sum(p2).getCartesianCoordinates(), 0);
		assertArrayEquals(res, p2.sum(p1).getCartesianCoordinates(), 0);
	}
	
}
