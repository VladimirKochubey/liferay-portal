/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portalweb.portal.session;

import com.liferay.portalweb.portal.BaseTestCase;
import com.liferay.portalweb.portal.util.RuntimeVariables;

/**
 * @author Brian Wing Shun Chan
 */
public class ConfirmNoSessionExpireTest extends BaseTestCase {
	public void testConfirmNoSessionExpire() throws Exception {
		selenium.selectWindow("null");
		selenium.selectFrame("relative=top");
		Thread.sleep(30000);
		Thread.sleep(30000);
		selenium.waitForElementNotPresent("//input[@value='Extend']");
		Thread.sleep(30000);
		Thread.sleep(30000);
		assertTrue(selenium.isElementNotPresent("//input[@value='Extend']"));
		selenium.waitForElementPresent("link=Session Expiration Test Page");
		selenium.click(RuntimeVariables.replace(
				"link=Session Expiration Test Page"));
		selenium.waitForPageToLoad("30000");
	}
}