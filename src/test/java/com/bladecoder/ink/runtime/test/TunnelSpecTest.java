package com.bladecoder.ink.runtime.test;

import org.junit.Assert;
import org.junit.Test;

import com.bladecoder.ink.runtime.Story;

public class TunnelSpecTest {

	/**
	 * "- Test for tunnel onwards divert override"
	 */
	@Test
	public void testTunnelOnwardsDivertOverride() throws Exception {

		String json = TestUtils.getJsonString("inkfiles/tunnels/tunnel-onwards-divert-override.ink.json").replace('\uFEFF', ' ');
		Story story = new Story(json);

		Assert.assertEquals("This is A\nNow in B.\n", story.continueMaximally());
	}

}
