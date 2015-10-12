package it.unibo.alchemist.test;

import it.unibo.alchemist.language.EnvironmentBuilder;
import it.unibo.alchemist.language.EnvironmentBuilder.Result;
import it.unibo.alchemist.model.interfaces.IEnvironment;
import it.unibo.alchemist.model.interfaces.INode;
import it.unibo.alchemist.model.interfaces.IReaction;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Danilo Pianini
 *
 */
public class TestEnvironmentBuilder {

	/**
	 * @param <T> should work regardless the incarnation.
	 * 
	 * @throws InterruptedException in case of failure.
	 * @throws ExecutionException in case of failure.
	 * @throws TimeoutException in case of failure.
	 */
	@Test
	public <T> void testBuildEnvironment() throws InterruptedException, ExecutionException, TimeoutException {
		final Future<Result<T>> fenv = EnvironmentBuilder.build(TestEnvironmentBuilder.class.getResourceAsStream("/wsn-10000-dd.xml"));
		final Result<T> res = fenv.get(1, TimeUnit.MINUTES);
		Assert.assertNotNull(res.getRandomEngine());
		final IEnvironment<T> env = res.getEnvironment();
		Assert.assertNotNull(env);
		Assert.assertNotNull(env.getNodes());
		Assert.assertTrue(env.getNodesNumber() > 0);
		final INode<T> node = env.getNodeByID(0);
		Assert.assertNotNull(node);
		Assert.assertNotNull(node.getReactions());
		Assert.assertFalse(node.getReactions().isEmpty());
		final IReaction<T> r = node.getReactions().get(0);
		Assert.assertNotNull(r);
	}
	
}
