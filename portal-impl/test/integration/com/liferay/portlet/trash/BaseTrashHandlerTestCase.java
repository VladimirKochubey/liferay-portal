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

package com.liferay.portlet.trash;

import com.liferay.portal.kernel.dao.orm.FinderCacheUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.transaction.Transactional;
import com.liferay.portal.kernel.trash.TrashHandler;
import com.liferay.portal.kernel.trash.TrashHandlerRegistryUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.WorkflowedModel;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceTestUtil;
import com.liferay.portal.util.TestPropsValues;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.service.AssetEntryLocalServiceUtil;
import com.liferay.portlet.trash.model.TrashEntry;
import com.liferay.portlet.trash.service.TrashEntryLocalServiceUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Brian Wing Shun Chan
 */
public abstract class BaseTrashHandlerTestCase {

	@Before
	public void setUp() {
		FinderCacheUtil.clearCache();
	}

	@Test
	@Transactional
	public void testTrashAndDelete() throws Exception {
		trashModel(false, true);
	}

	@Test
	@Transactional
	public void testTrashAndRestoreApproved() throws Exception {
		trashModel(true, false);
	}

	@Test
	@Transactional
	public void testTrashAndRestoreDraft() throws Exception {
		trashModel(false, false);
	}

	protected abstract BaseModel<?> addBaseModel(
			BaseModel<?> parentBaseModel, boolean approved,
			ServiceContext serviceContext)
		throws Exception;

	protected AssetEntry fetchAssetEntry(Class<?> clazz, long classPK)
		throws Exception {

		return AssetEntryLocalServiceUtil.fetchEntry(clazz.getName(), classPK);
	}

	protected AssetEntry fetchAssetEntry(ClassedModel classedModel)
		throws Exception {

		return fetchAssetEntry(
			classedModel.getModelClass(),
			(Long)classedModel.getPrimaryKeyObj());
	}

	protected abstract BaseModel<?> getBaseModel(long primaryKey)
		throws Exception;

	protected abstract Class<?> getBaseModelClass();

	protected String getBaseModelClassName() {
		Class<?> clazz = getBaseModelClass();

		return clazz.getName();
	}

	protected abstract int getBaseModelsNotInTrashCount(
			BaseModel<?> parentBaseModel)
		throws Exception;

	protected BaseModel<?> getParentBaseModel(
			Group group, ServiceContext serviceContext)
		throws Exception {

		return group;
	}

	protected abstract String getSearchKeywords();

	protected int getTrashEntriesCount(long groupId) throws Exception {
		return TrashEntryLocalServiceUtil.getEntriesCount(groupId);
	}

	protected boolean isAssetEntryVisible(ClassedModel classedModel)
		throws Exception {

		AssetEntry assetEntry = AssetEntryLocalServiceUtil.getEntry(
			classedModel.getModelClassName(),
			(Long)classedModel.getPrimaryKeyObj());

		return assetEntry.isVisible();
	}

	protected abstract void moveBaseModelToTrash(long primaryKey)
		throws Exception;

	protected int searchBaseModelsCount(Class<?> clazz, long groupId)
		throws Exception {

		Thread.sleep(1000 * TestPropsValues.JUNIT_DELAY_FACTOR);

		Indexer indexer = IndexerRegistryUtil.getIndexer(clazz);

		SearchContext searchContext = ServiceTestUtil.getSearchContext();

		searchContext.setGroupIds(new long[] {groupId});

		Hits results = indexer.search(searchContext);

		return results.getLength();
	}

	protected int searchTrashEntriesCount(
			String keywords, ServiceContext serviceContext)
		throws Exception {

		Thread.sleep(1000 * TestPropsValues.JUNIT_DELAY_FACTOR);

		Hits results = TrashEntryLocalServiceUtil.search(
			serviceContext.getCompanyId(), serviceContext.getScopeGroupId(),
			serviceContext.getUserId(), keywords, QueryUtil.ALL_POS,
			QueryUtil.ALL_POS, null);

		return results.getLength();
	}

	protected void trashModel(boolean approved, boolean delete)
		throws Exception {

		Group group = ServiceTestUtil.addGroup();

		ServiceContext serviceContext = ServiceTestUtil.getServiceContext();

		serviceContext.setScopeGroupId(group.getGroupId());

		BaseModel<?> parentBaseModel = getParentBaseModel(
			group, serviceContext);

		int initialBaseModelsCount = getBaseModelsNotInTrashCount(
			parentBaseModel);
		int initialBaseModelsSearchCount = searchBaseModelsCount(
			getBaseModelClass(), group.getGroupId());
		int initialTrashEntriesCount = getTrashEntriesCount(group.getGroupId());
		int initialTrashEntriesSearchCount = searchTrashEntriesCount(
			getSearchKeywords(), serviceContext);

		BaseModel<?> baseModel = addBaseModel(group, approved, serviceContext);

		WorkflowedModel workflowedModel = (WorkflowedModel)baseModel;

		int oldStatus = workflowedModel.getStatus();

		Assert.assertEquals(approved, isAssetEntryVisible(baseModel));

		Assert.assertEquals(
			initialBaseModelsCount + 1,
			getBaseModelsNotInTrashCount(parentBaseModel));

		if (approved) {
			Assert.assertEquals(
				initialBaseModelsSearchCount + 1,
				searchBaseModelsCount(getBaseModelClass(), group.getGroupId()));
		}
		else {
			Assert.assertEquals(
				initialBaseModelsSearchCount,
				searchBaseModelsCount(getBaseModelClass(), group.getGroupId()));
		}

		Assert.assertEquals(
			initialTrashEntriesCount, getTrashEntriesCount(group.getGroupId()));
		Assert.assertEquals(
			initialTrashEntriesSearchCount,
			searchTrashEntriesCount(getSearchKeywords(), serviceContext));

		moveBaseModelToTrash((Long)baseModel.getPrimaryKeyObj());

		TrashEntry trashEntry = TrashEntryLocalServiceUtil.getEntry(
			getBaseModelClassName(), (Long)baseModel.getPrimaryKeyObj());

		Assert.assertEquals(
			baseModel.getPrimaryKeyObj(),
			Long.valueOf(trashEntry.getClassPK()));
		Assert.assertEquals(
			WorkflowConstants.STATUS_IN_TRASH, workflowedModel.getStatus());
		Assert.assertFalse(isAssetEntryVisible(baseModel));

		Assert.assertEquals(
			initialBaseModelsCount,
			getBaseModelsNotInTrashCount(parentBaseModel));
		Assert.assertEquals(
			initialBaseModelsSearchCount,
			searchBaseModelsCount(getBaseModelClass(), group.getGroupId()));
		Assert.assertEquals(
			initialTrashEntriesCount + 1,
			getTrashEntriesCount(group.getGroupId()));
		Assert.assertEquals(
			initialTrashEntriesSearchCount + 1,
			searchTrashEntriesCount(getSearchKeywords(), serviceContext));

		TrashHandler trashHandler = TrashHandlerRegistryUtil.getTrashHandler(
			getBaseModelClassName());

		if (delete) {
			trashHandler.deleteTrashEntry((Long)baseModel.getPrimaryKeyObj());

			Assert.assertNull(fetchAssetEntry(baseModel));

			Assert.assertEquals(
				initialBaseModelsCount,
				getBaseModelsNotInTrashCount(parentBaseModel));
			Assert.assertEquals(
				initialBaseModelsSearchCount,
				searchBaseModelsCount(getBaseModelClass(), group.getGroupId()));
			Assert.assertEquals(
				initialTrashEntriesSearchCount,
				searchTrashEntriesCount(getSearchKeywords(), serviceContext));
		}
		else {
			trashHandler.restoreTrashEntry((Long)baseModel.getPrimaryKeyObj());

			baseModel = getBaseModel((Long)baseModel.getPrimaryKeyObj());

			workflowedModel = (WorkflowedModel)baseModel;

			Assert.assertEquals(oldStatus, workflowedModel.getStatus());
			Assert.assertEquals(approved, isAssetEntryVisible(baseModel));

			Assert.assertEquals(
				initialBaseModelsCount + 1,
				getBaseModelsNotInTrashCount(parentBaseModel));

			if (approved) {
				Assert.assertEquals(
					initialBaseModelsSearchCount + 1,
					searchBaseModelsCount(
						getBaseModelClass(), group.getGroupId()));
			}
			else {
				Assert.assertEquals(
					initialBaseModelsSearchCount,
					searchBaseModelsCount(
						getBaseModelClass(), group.getGroupId()));
			}

			Assert.assertEquals(
				initialTrashEntriesSearchCount,
				searchTrashEntriesCount(getSearchKeywords(), serviceContext));

			trashHandler.deleteTrashEntry((Long)baseModel.getPrimaryKeyObj());
		}
	}

}