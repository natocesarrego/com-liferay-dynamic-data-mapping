/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
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

package com.liferay.dynamic.data.mapping.internal.util;

import com.liferay.dynamic.data.mapping.form.field.type.DDMFormFieldTypeServicesTracker;
import com.liferay.dynamic.data.mapping.internal.test.util.DDMFixture;
import com.liferay.dynamic.data.mapping.io.DDMFormJSONSerializer;
import com.liferay.dynamic.data.mapping.io.internal.DDMFormJSONSerializerImpl;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMFormField;
import com.liferay.dynamic.data.mapping.model.DDMFormFieldType;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.LocalizedValue;
import com.liferay.dynamic.data.mapping.model.impl.DDMStructureImpl;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalServiceUtil;
import com.liferay.dynamic.data.mapping.storage.DDMFormFieldValue;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.test.util.DDMFormTestUtil;
import com.liferay.dynamic.data.mapping.test.util.DDMFormValuesTestUtil;
import com.liferay.dynamic.data.mapping.util.DDMIndexer;
import com.liferay.portal.json.JSONArrayImpl;
import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.json.JSONObjectImpl;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.DateFormatFactory;
import com.liferay.portal.kernel.util.DateFormatFactoryUtil;
import com.liferay.portal.kernel.util.Html;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.search.test.util.FieldValuesAssert;
import com.liferay.portal.search.test.util.indexing.DocumentFixture;
import com.liferay.portal.util.HtmlImpl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author Lino Alves
 * @author André de Oliveira
*/
@PrepareOnlyThisForTest(
	{DDMStructureLocalServiceUtil.class, ResourceBundleUtil.class}
)
@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor(
	{
		"com.liferay.dynamic.data.mapping.model.impl.DDMStructureModelImpl",
		"com.liferay.dynamic.data.mapping.service.DDMStructureLocalServiceUtil"
	}
)
public class DDMIndexerImplTest {

	@BeforeClass
	public static void setUpClass() throws JSONException {
		setUpDateFormatFactoryUtil();
		setUpHtmlUtil();
		setUpJSONFactoryUtil();
	}

	@AfterClass
	public static void tearDownClass() {
		tearDownDateFormatFactoryUtil();
		tearDownHtmlUtil();
		tearDownJSONFactoryUtil();
	}

	@Before
	public void setUp() throws Exception {
		ddmFixture.setUp();
		documentFixture.setUp();
	}

	@After
	public void tearDown() throws Exception {
		ddmFixture.tearDown();
		documentFixture.tearDown();
	}

	@Test
	public void testFormWithOneAvailableLocaleSameAsDefaultLocale() {
		Locale defaultLocale = LocaleUtil.JAPAN;
		Locale translationLocale = LocaleUtil.JAPAN;

		Set<Locale> availableLocales = Collections.singleton(defaultLocale);

		DDMForm ddmForm = DDMFormTestUtil.createDDMForm(
			availableLocales, defaultLocale);

		String fieldName = "text1";
		String indexType = "text";

		ddmForm.addDDMFormField(createDDMFormField(fieldName, indexType));

		String fieldValue = "新規作成";

		Map<Locale, String> fieldValues = new HashMap<>();

		fieldValues.put(translationLocale, fieldValue);

		DDMFormFieldValue ddmFormFieldValue = createDDMFormFieldValue(
			fieldName, fieldValues, defaultLocale);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		Map<String, String> map = _withDefaultSortableValues(
			Collections.singletonMap(
				"ddm__text__NNNNN__text1_ja_JP", fieldValue));

		FieldValuesAssert.assertFieldValues(
			_replaceKeys(
				"NNNNN", String.valueOf(ddmStructure.getStructureId()), map),
			"ddm__text", document, fieldValue);
	}

	@Test
	public void testFormWithTwoAvailableLocalesAndFieldWithNonDefaultLocale() {
		Locale defaultLocale = LocaleUtil.US;
		Locale translationLocale = LocaleUtil.JAPAN;

		Set<Locale> availableLocales = new HashSet<>(
			Arrays.asList(defaultLocale, translationLocale));

		DDMForm ddmForm = DDMFormTestUtil.createDDMForm(
			availableLocales, defaultLocale);

		String fieldName = "text1";
		String indexType = "text";

		ddmForm.addDDMFormField(createDDMFormField(fieldName, indexType));

		String fieldValue = "新規作成";

		Map<Locale, String> fieldValues = new HashMap<>();

		fieldValues.put(translationLocale, fieldValue);

		DDMFormFieldValue ddmFormFieldValue = createDDMFormFieldValue(
			fieldName, fieldValues, defaultLocale);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		Map<String, String> map = _withDefaultSortableValues(
			Collections.singletonMap(
				"ddm__text__NNNNN__text1_ja_JP", fieldValue));

		FieldValuesAssert.assertFieldValues(
			_replaceKeys(
				"NNNNN", String.valueOf(ddmStructure.getStructureId()), map),
			"ddm__text", document, fieldValue);
	}

	@Test
	public void testFormWithTwoAvailableLocalesAndFieldWithTwoLocales() {
		Locale defaultLocale = LocaleUtil.JAPAN;
		Locale translationLocale = LocaleUtil.US;

		Set<Locale> availableLocales = new HashSet<>(
			Arrays.asList(defaultLocale, translationLocale));

		DDMForm ddmForm = DDMFormTestUtil.createDDMForm(
			availableLocales, defaultLocale);

		String fieldName = "text1";
		String indexType = "text";

		DDMFormField ddmFormField = createDDMFormField(fieldName, indexType);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldValueJP = "新規作成";
		String fieldValueUS = "Create New";

		Map<Locale, String> fieldValues = new HashMap<>();

		fieldValues.put(defaultLocale, fieldValueJP);
		fieldValues.put(translationLocale, fieldValueUS);

		DDMFormFieldValue ddmFormFieldValue = createDDMFormFieldValue(
			fieldName, fieldValues, defaultLocale);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		Map<String, String> map = _withDefaultSortableValues(
			new HashMap<String, String>() {
				{
					put("ddm__text__NNNNN__text1_ja_JP", fieldValueJP);
					put("ddm__text__NNNNN__text1_en_US", fieldValueUS);
				}
			});

		FieldValuesAssert.assertFieldValues(
			_replaceKeys(
				"NNNNN", String.valueOf(ddmStructure.getStructureId()), map),
			"ddm__text", document, fieldValueJP);
	}

	@Test
	public void testIndexCheckboxFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.CHECKBOX);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.CHECKBOX);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(
			DDMFormFieldType.CHECKBOX);

		Map<String, String> indexedFields = _withDefaultSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexCheckboxMultipleFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.CHECKBOX_MULTIPLE);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.CHECKBOX_MULTIPLE);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(
			DDMFormFieldType.CHECKBOX_MULTIPLE);

		Map<String, String> indexedFields = _withDefaultSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexDateFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.DATE);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.DATE);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedSortableFieldName = indexedFieldName + "_Number_sortable";

		String indexedFieldValue = getIndexedFieldValue(DDMFormFieldType.DATE);

		String indexedSortableFieldValue = getIndexedSortableDateFieldValue();

		Map<String, String> indexedFields = new HashMap<>();

		indexedFields.put(indexedFieldName, indexedFieldValue);
		indexedFields.put(indexedSortableFieldName, indexedSortableFieldValue);

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexDecimalFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.DECIMAL);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.DECIMAL);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(
			DDMFormFieldType.DECIMAL);

		Map<String, String> indexedFields = _withNumberSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexDocumentLibraryFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.DOCUMENT_LIBRARY);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.DOCUMENT_LIBRARY);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(
			DDMFormFieldType.DOCUMENT_LIBRARY);

		Map<String, String> indexedFields = _withDefaultSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexGeolocationFieldWithUSLocale() throws JSONException {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.GEOLOCATION);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.GEOLOCATION);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US_geolocation");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(
			DDMFormFieldType.GEOLOCATION);

		Map<String, String> indexedFields = Collections.singletonMap(
			indexedFieldName, indexedFieldValue);

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexGridFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType("grid");

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(fieldName, "grid");

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue("grid");

		Map<String, String> indexedFields = _withDefaultSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexImageFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.IMAGE);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.IMAGE);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(DDMFormFieldType.IMAGE);

		Map<String, String> indexedFields = _withDefaultSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexIntegerFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.INTEGER);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.INTEGER);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(
			DDMFormFieldType.INTEGER);

		Map<String, String> indexedFields = _withNumberSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexJournalArticleFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.JOURNAL_ARTICLE);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.JOURNAL_ARTICLE);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(
			DDMFormFieldType.JOURNAL_ARTICLE);

		Map<String, String> indexedFields = _withDefaultSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexLinkToPageFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.LINK_TO_PAGE);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.LINK_TO_PAGE);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(
			DDMFormFieldType.LINK_TO_PAGE);

		Map<String, String> indexedFields = _withDefaultSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexNumberFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.NUMBER);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.NUMBER);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(
			DDMFormFieldType.NUMBER);

		Map<String, String> indexedFields = _withDefaultSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexNumericFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.NUMERIC);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.NUMERIC);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(
			DDMFormFieldType.NUMERIC);

		Map<String, String> indexedFields = _withDefaultSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexRadioFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.RADIO);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.RADIO);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(DDMFormFieldType.RADIO);

		Map<String, String> indexedFields = _withDefaultSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexSelectFieldWithUSLocale() throws JSONException {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.SELECT);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.SELECT);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(
			DDMFormFieldType.SELECT);

		Map<String, String> indexedFields = _withDefaultSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexTextAreaFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.TEXT_AREA);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.TEXT_AREA);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(
			DDMFormFieldType.TEXT_AREA);

		Map<String, String> indexedFields = _withDefaultSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexTextFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.TEXT);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.TEXT);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(DDMFormFieldType.TEXT);

		Map<String, String> indexedFields = _withDefaultSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	@Test
	public void testIndexTextHtmlFieldWithUSLocale() {
		DDMForm ddmForm = DDMFormTestUtil.createDDMFormWithUSLocale();

		DDMFormField ddmFormField = createDDMFormFieldByType(
			DDMFormFieldType.TEXT_HTML);

		ddmForm.addDDMFormField(ddmFormField);

		String fieldName = ddmFormField.getName();

		DDMFormFieldValue ddmFormFieldValue =
			createDDMFormFieldValueByTypeWithUSLocale(
				fieldName, DDMFormFieldType.TEXT_HTML);

		DDMFormValues ddmFormValues = createDDMFormValues(
			ddmForm, ddmFormFieldValue);

		Document document = createDocument();

		DDMStructure ddmStructure = createDDMStructure(ddmForm);

		ddmIndexer.addAttributes(document, ddmStructure, ddmFormValues);

		StringBundler sb = new StringBundler();

		sb.append("ddm__");
		sb.append(ddmFormField.getIndexType());
		sb.append("__NNNNN__");
		sb.append(fieldName);
		sb.append("_en_US");

		String indexedFieldName = sb.toString();

		String indexedFieldValue = getIndexedFieldValue(
			DDMFormFieldType.TEXT_HTML);

		Map<String, String> indexedFields = _withDefaultSortableValues(
			Collections.singletonMap(indexedFieldName, indexedFieldValue));

		indexedFields = _replaceKeys(
			"NNNNN", String.valueOf(ddmStructure.getStructureId()),
			indexedFields);

		String indexedFieldNamePrefix = indexedFieldName.substring(0, 2);

		FieldValuesAssert.assertFieldValues(
			indexedFields, indexedFieldNamePrefix, document, fieldName);
	}

	protected static String createJSONObjectString(
		Map<String, String> fieldValueProperties) {

		JSONObject jsonObject = new JSONObjectImpl(fieldValueProperties);

		return jsonObject.toJSONString();
	}

	protected static String executeExtractText(String textHtmlFieldValue) {
		String extractedText = textHtmlFieldValue.replace(
			"<p>", StringPool.BLANK);

		return extractedText.replace("</p>", StringPool.BLANK);
	}

	protected static String getFieldValue(String type) {
		String fieldValue = "field_value";

		if (type.equals(DDMFormFieldType.CHECKBOX)) {
			fieldValue = "true";
		}
		else if (type.equals(DDMFormFieldType.CHECKBOX_MULTIPLE) ||
				 type.equals(DDMFormFieldType.SELECT)) {

			fieldValue = "[\"Option1\"]";
		}
		else if (type.equals(DDMFormFieldType.DATE)) {
			fieldValue = "2017-07-05";
		}
		else if (type.equals(DDMFormFieldType.DECIMAL)) {
			fieldValue = "10.0";
		}
		else if (type.equals(DDMFormFieldType.DOCUMENT_LIBRARY)) {
			Map<String, String> fieldValueProperties = new HashMap<>();

			fieldValueProperties.put("groupId", GROUP_ID);
			fieldValueProperties.put("title", "document.doc");
			fieldValueProperties.put("type", "document");
			fieldValueProperties.put("uuid", UUID);

			fieldValue = createJSONObjectString(fieldValueProperties);
		}
		else if (type.equals(DDMFormFieldType.GEOLOCATION)) {
			Map<String, String> fieldValueProperties = new HashMap<>();

			fieldValueProperties.put("latitude", "-8.0386948");
			fieldValueProperties.put("longitude", "-34.9127080045413");

			fieldValue = createJSONObjectString(fieldValueProperties);
		}
		else if (type.equals("grid")) {
			Map<String, String> fieldValueProperties = new HashMap<>();

			fieldValueProperties.put("Row1", "Column1");
			fieldValueProperties.put("Row1", "Column2");

			fieldValue = createJSONObjectString(fieldValueProperties);
		}
		else if (type.equals(DDMFormFieldType.IMAGE)) {
			Map<String, String> fieldValueProperties = new HashMap<>();

			fieldValueProperties.put("alt", "Green Button");
			fieldValueProperties.put("fileEntryId", "30656");
			fieldValueProperties.put("groupId", GROUP_ID);
			fieldValueProperties.put("name", "Green button.png");
			fieldValueProperties.put("resourcePrimKey", "30651");
			fieldValueProperties.put("title", "Green button.png");
			fieldValueProperties.put("type", "journal");
			fieldValueProperties.put("uuid", UUID);

			fieldValue = createJSONObjectString(fieldValueProperties);
		}
		else if (type.equals(DDMFormFieldType.INTEGER) ||
				 type.equals(DDMFormFieldType.NUMBER) ||
				 type.equals(DDMFormFieldType.NUMERIC)) {

			fieldValue = "10";
		}
		else if (type.equals(DDMFormFieldType.JOURNAL_ARTICLE)) {
			Map<String, String> fieldValueProperties = new HashMap<>();

			fieldValueProperties.put(
				"className", "com.liferay.journal.model.JournalArticle");
			fieldValueProperties.put("classPK", "30638");

			fieldValue = createJSONObjectString(fieldValueProperties);
		}
		else if (type.equals(DDMFormFieldType.LINK_TO_PAGE)) {
			String layoutId = "1";
			String layoutType = "public";
			String layoutGroupId = GROUP_ID;

			fieldValue =
				layoutId + StringPool.AT + layoutType + StringPool.AT +
					layoutGroupId;
		}
		else if (type.equals(DDMFormFieldType.TEXT_HTML)) {
			fieldValue = "<p>" + fieldValue + "</p>";
		}

		return fieldValue;
	}

	protected static void setUpDateFormatFactoryUtil() {
		dateFormatFactory = DateFormatFactoryUtil.getDateFormatFactory();

		DateFormatFactoryUtil dateFormatFactoryUtil =
			new DateFormatFactoryUtil();

		DateFormatFactory dateFormatFactory = Mockito.mock(
			DateFormatFactory.class);

		Mockito.when(
			dateFormatFactory.getSimpleDateFormat("yyyy-MM-dd")
		).thenReturn(
			new SimpleDateFormat("yyyy-MM-dd")
		);

		dateFormatFactoryUtil.setDateFormatFactory(dateFormatFactory);
	}

	protected static void setUpHtmlUtil() {
		html = HtmlUtil.getHtml();

		HtmlUtil htmlUtil = new HtmlUtil();

		Html html = Mockito.mock(Html.class);

		String textHtmlFieldValue = getFieldValue(DDMFormFieldType.TEXT_HTML);

		Mockito.when(
			html.extractText(textHtmlFieldValue)
		).thenReturn(
			executeExtractText(textHtmlFieldValue)
		);

		htmlUtil.setHtml(html);
	}

	protected static void setUpJSONFactoryUtil() throws JSONException {
		jsonFactory = JSONFactoryUtil.getJSONFactory();

		JSONFactoryUtil jsonFactoryUtil = new JSONFactoryUtil();

		JSONFactory jsonFactory = Mockito.mock(JSONFactory.class);

		String geolocationFieldValue = getFieldValue(
			DDMFormFieldType.GEOLOCATION);

		Mockito.when(
			jsonFactory.createJSONObject(geolocationFieldValue)
		).thenReturn(
			new JSONObjectImpl(geolocationFieldValue)
		);

		String selectFieldValue = getFieldValue(DDMFormFieldType.SELECT);

		Mockito.when(
			jsonFactory.createJSONArray(selectFieldValue)
		).thenReturn(
			new JSONArrayImpl(selectFieldValue)
		);

		jsonFactoryUtil.setJSONFactory(jsonFactory);
	}

	protected static void tearDownDateFormatFactoryUtil() {
		DateFormatFactoryUtil dateFormatFactoryUtil =
			new DateFormatFactoryUtil();

		dateFormatFactoryUtil.setDateFormatFactory(dateFormatFactory);

		dateFormatFactory = null;
	}

	protected static void tearDownHtmlUtil() {
		HtmlUtil htmlUtil = new HtmlUtil();

		htmlUtil.setHtml(html);

		html = null;
	}

	protected static void tearDownJSONFactoryUtil() {
		JSONFactoryUtil jsonFactoryUtil = new JSONFactoryUtil();

		jsonFactoryUtil.setJSONFactory(jsonFactory);

		jsonFactory = null;
	}

	protected DDMFormField createDDMFormField(
		String fieldName, String indexType) {

		DDMFormField ddmFormField = DDMFormTestUtil.createTextDDMFormField(
			fieldName, false, false, true);

		ddmFormField.setIndexType(indexType);

		return ddmFormField;
	}

	protected DDMFormField createDDMFormFieldByType(String type) {
		String fieldName = type + "_field";

		String dataType = getDataType(type);

		DDMFormField ddmFormField = DDMFormTestUtil.createDDMFormField(
			fieldName, fieldName, type, dataType, false, false, true);

		ddmFormField.setIndexType(getIndexType(type));

		return ddmFormField;
	}

	protected DDMFormFieldValue createDDMFormFieldValue(
		String name, Map<Locale, String> values, Locale defaultLocale) {

		LocalizedValue localizedValue = new LocalizedValue(defaultLocale);

		Map<Locale, String> actualValues = localizedValue.getValues();

		actualValues.putAll(values);

		return DDMFormValuesTestUtil.createDDMFormFieldValue(
			name, localizedValue);
	}

	protected DDMFormFieldValue createDDMFormFieldValueByTypeWithUSLocale(
		String name, String type) {

		Locale locale = LocaleUtil.US;

		Map<Locale, String> values = new HashMap<>();

		values.put(locale, getFieldValue(type));

		return createDDMFormFieldValue(name, values, locale);
	}

	protected DDMFormJSONSerializer createDDMFormJSONSerializer() {
		return new DDMFormJSONSerializerImpl() {
			{
				setDDMFormFieldTypeServicesTracker(
					Mockito.mock(DDMFormFieldTypeServicesTracker.class));

				setJSONFactory(new JSONFactoryImpl());
			}
		};
	}

	protected DDMFormValues createDDMFormValues(
		DDMForm ddmForm, DDMFormFieldValue... ddmFormFieldValues) {

		DDMFormValues ddmFormValues = DDMFormValuesTestUtil.createDDMFormValues(
			ddmForm);

		for (DDMFormFieldValue ddmFormFieldValue : ddmFormFieldValues) {
			ddmFormValues.addDDMFormFieldValue(ddmFormFieldValue);
		}

		return ddmFormValues;
	}

	protected DDMStructure createDDMStructure(DDMForm ddmForm) {
		DDMStructure ddmStructure = new DDMStructureImpl();

		ddmStructure.setDefinition(ddmFormJSONSerializer.serialize(ddmForm));

		ddmStructure.setDDMForm(ddmForm);

		ddmStructure.setName(RandomTestUtil.randomString());
		ddmStructure.setStructureId(RandomTestUtil.randomLong());

		ddmFixture.whenDDMStructureLocalServiceFetchStructure(ddmStructure);

		return ddmStructure;
	}

	protected Document createDocument() {
		return DocumentFixture.newDocument(
			RandomTestUtil.randomLong(), RandomTestUtil.randomLong(),
			DDMForm.class.getName());
	}

	protected String getDataType(String type) {
		String dataType = "string";

		if (type.equals(DDMFormFieldType.CHECKBOX)) {
			dataType = "boolean";
		}
		else if (type.equals(DDMFormFieldType.DATE)) {
			dataType = "date";
		}
		else if (type.equals(DDMFormFieldType.DECIMAL)) {
			dataType = "double";
		}
		else if (type.equals(DDMFormFieldType.DOCUMENT_LIBRARY)) {
			dataType = "ddm-documentlibrary";
		}
		else if (type.equals(DDMFormFieldType.GEOLOCATION)) {
			dataType = "geolocation";
		}
		else if (type.equals(DDMFormFieldType.IMAGE)) {
			dataType = "image";
		}
		else if (type.equals(DDMFormFieldType.INTEGER)) {
			dataType = "integer";
		}
		else if (type.equals(DDMFormFieldType.JOURNAL_ARTICLE)) {
			dataType = "journal-article";
		}
		else if (type.equals(DDMFormFieldType.LINK_TO_PAGE)) {
			dataType = "link-to-page";
		}
		else if (type.equals(DDMFormFieldType.NUMBER)) {
			dataType = "number";
		}
		else if (type.equals(DDMFormFieldType.TEXT_HTML)) {
			dataType = "html";
		}

		return dataType;
	}

	protected String getIndexedFieldValue(String type) {
		String indexedFieldValue = "field_value";

		if (type.equals(DDMFormFieldType.CHECKBOX)) {
			indexedFieldValue = "true";
		}
		else if (type.equals(DDMFormFieldType.CHECKBOX_MULTIPLE)) {
			indexedFieldValue = "[\"Option1\"]";
		}
		else if (type.equals(DDMFormFieldType.DATE)) {
			Calendar calendar = new GregorianCalendar();

			calendar.setTimeInMillis(0);

			calendar.set(Calendar.YEAR, 2017);
			calendar.set(Calendar.MONTH, Calendar.JULY);
			calendar.set(Calendar.DAY_OF_MONTH, 5);

			Date date = calendar.getTime();

			DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

			indexedFieldValue = dateFormat.format(date);
		}
		else if (type.equals(DDMFormFieldType.DECIMAL)) {
			indexedFieldValue = "10.0";
		}
		else if (type.equals(DDMFormFieldType.DOCUMENT_LIBRARY)) {
			Map<String, String> indexedFieldValueProperties = new HashMap<>();

			indexedFieldValueProperties.put("groupId", GROUP_ID);
			indexedFieldValueProperties.put("title", "document.doc");
			indexedFieldValueProperties.put("type", "document");
			indexedFieldValueProperties.put("uuid", UUID);

			indexedFieldValue = createJSONObjectString(
				indexedFieldValueProperties);
		}
		else if (type.equals(DDMFormFieldType.GEOLOCATION)) {
			indexedFieldValue = "lat: -8.0386948, lon: -34.9127080045413";
		}
		else if (type.equals("grid")) {
			Map<String, String> indexedFieldValueProperties = new HashMap<>();

			indexedFieldValueProperties.put("Row1", "Column1");
			indexedFieldValueProperties.put("Row1", "Column2");

			indexedFieldValue = createJSONObjectString(
				indexedFieldValueProperties);
		}
		else if (type.equals(DDMFormFieldType.IMAGE)) {
			Map<String, String> indexedFieldValueProperties = new HashMap<>();

			indexedFieldValueProperties.put("alt", "Green Button");
			indexedFieldValueProperties.put("fileEntryId", "30656");
			indexedFieldValueProperties.put("groupId", GROUP_ID);
			indexedFieldValueProperties.put("name", "Green button.png");
			indexedFieldValueProperties.put("resourcePrimKey", "30651");
			indexedFieldValueProperties.put("title", "Green button.png");
			indexedFieldValueProperties.put("type", "journal");
			indexedFieldValueProperties.put("uuid", UUID);

			indexedFieldValue = createJSONObjectString(
				indexedFieldValueProperties);
		}
		else if (type.equals(DDMFormFieldType.INTEGER) ||
				 type.equals(DDMFormFieldType.NUMBER) ||
				 type.equals(DDMFormFieldType.NUMERIC)) {

			indexedFieldValue = "10";
		}
		else if (type.equals(DDMFormFieldType.JOURNAL_ARTICLE)) {
			Map<String, String> fieldValueProperties = new HashMap<>();

			fieldValueProperties.put(
				"className", "com.liferay.journal.model.JournalArticle");
			fieldValueProperties.put("classPK", "30638");

			indexedFieldValue = createJSONObjectString(fieldValueProperties);
		}
		else if (type.equals(DDMFormFieldType.LINK_TO_PAGE)) {
			String layoutId = "1";
			String layoutType = "public";
			String layoutGroupId = GROUP_ID;

			indexedFieldValue =
				layoutId + StringPool.AT + layoutType + StringPool.AT +
					layoutGroupId;
		}
		else if (type.equals(DDMFormFieldType.SELECT)) {
			indexedFieldValue = "Option1";
		}

		return indexedFieldValue;
	}

	protected String getIndexedSortableDateFieldValue() {
		Calendar calendar = new GregorianCalendar();

		calendar.setTimeInMillis(0);

		calendar.set(Calendar.YEAR, 2017);
		calendar.set(Calendar.MONTH, Calendar.JULY);
		calendar.set(Calendar.DAY_OF_MONTH, 5);

		return String.valueOf(calendar.getTimeInMillis());
	}

	protected String getIndexType(String type) {
		String indexType = "keyword";

		if (type.equals(DDMFormFieldType.IMAGE) ||
			type.equals(DDMFormFieldType.TEXT_AREA) ||
			type.equals(DDMFormFieldType.TEXT_HTML)) {

			indexType = "text";
		}

		return indexType;
	}

	protected static final String GROUP_ID = "20128";

	protected static final String UUID = "6c9137db-e338-a8bf-b5f2-3366f7381479";

	protected static DateFormatFactory dateFormatFactory;
	protected static Html html = new HtmlImpl();
	protected static JSONFactory jsonFactory = new JSONFactoryImpl();

	protected final DDMFixture ddmFixture = new DDMFixture();
	protected final DDMFormJSONSerializer ddmFormJSONSerializer =
		createDDMFormJSONSerializer();
	protected final DDMIndexer ddmIndexer = new DDMIndexerImpl();
	protected final DocumentFixture documentFixture = new DocumentFixture();

	private static Map<String, String> _replaceKeys(
		String oldSub, String newSub, Map<String, String> map) {

		Set<Entry<String, String>> entrySet = map.entrySet();

		Stream<Entry<String, String>> entries = entrySet.stream();

		return entries.collect(
			Collectors.toMap(
				entry -> StringUtil.replace(entry.getKey(), oldSub, newSub),
				Map.Entry::getValue));
	}

	private static Map<String, String> _withDefaultSortableValues(
		Map<String, String> map) {

		return _withSortableValues(map, "_sortable");
	}

	private static Map<String, String> _withNumberSortableValues(
		Map<String, String> map) {

		return _withSortableValues(map, "_Number_sortable");
	}

	private static Map<String, String> _withSortableValues(
		Map<String, String> map, String suffix) {

		Set<Entry<String, String>> entrySet = map.entrySet();

		Stream<Entry<String, String>> entries = entrySet.stream();

		Map<String, String> map2 = entries.collect(
			Collectors.toMap(
				entry -> entry.getKey() + suffix,
				entry -> StringUtil.toLowerCase(entry.getValue())));

		map2.putAll(map);

		return map2;
	}

}