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

import com.liferay.dynamic.data.mapping.model.DDMFormField;
import com.liferay.dynamic.data.mapping.model.DDMFormFieldType;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.storage.DDMFormFieldValue;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.storage.FieldConstants;
import com.liferay.dynamic.data.mapping.util.DDM;
import com.liferay.dynamic.data.mapping.util.DDMIndexer;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.filter.QueryFilter;
import com.liferay.portal.kernel.search.generic.BooleanQueryImpl;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.DateFormatFactoryUtil;
import com.liferay.portal.kernel.util.FastDateFormatFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.Serializable;

import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Alexander Chow
 */
@Component(immediate = true, service = DDMIndexer.class)
public class DDMIndexerImpl implements DDMIndexer {

	@Override
	public void addAttributes(
		Document document, DDMStructure ddmStructure,
		DDMFormValues ddmFormValues) {

		Set<Locale> locales = ddmFormValues.getAvailableLocales();

		Map<String, DDMFormField> ddmFormFieldsMap =
			ddmStructure.getFullHierarchyDDMFormFieldsMap(true);

		Collection<DDMFormField> ddmFormFields = ddmFormFieldsMap.values();

		Map<String, List<DDMFormFieldValue>> ddmFormFieldValuesMap =
			getAllDDMFormFieldValuesMap(ddmFormValues.getDDMFormFieldValues());

		for (DDMFormField ddmFormField : ddmFormFields) {
			try {
				String indexType = ddmStructure.getFieldProperty(
					ddmFormField.getName(), "indexType");

				if (Validator.isNull(indexType)) {
					continue;
				}

				List<DDMFormFieldValue> ddmFormFieldValues =
					ddmFormFieldValuesMap.get(ddmFormField.getName());

				if (ddmFormFieldValues.isEmpty()) {
					continue;
				}

				String dataType = ddmFormField.getDataType();

				if (Validator.isNull(dataType)) {
					continue;
				}

				for (Locale locale : locales) {
					boolean repeatable = ddmFormField.isRepeatable();

					String name = encodeName(
						ddmStructure.getStructureId(), ddmFormField.getName(),
						locale, indexType);

					List<String> valuesStrings = getValuesStrings(
						ddmFormFieldValues, locale);

					Object[] values = getValuesAsArray(
						repeatable, dataType, valuesStrings);

					if (dataType.equals(FieldConstants.BOOLEAN)) {
						if (repeatable) {
							document.addKeywordSortable(name, (String[])values);
						}
						else {
							document.addKeywordSortable(
								name, (String)values[0]);
						}
					}
					else if (dataType.equals(FieldConstants.DATE)) {
						if (repeatable) {
							document.addDateSortable(name, (Date[])values);
						}
						else {
							document.addDateSortable(name, (Date)values[0]);
						}
					}
					else if (dataType.equals(FieldConstants.DOUBLE)) {
						if (repeatable) {
							document.addNumberSortable(name, (Double[])values);
						}
						else {
							document.addNumberSortable(name, (Double)values[0]);
						}
					}
					else if (dataType.equals(FieldConstants.INTEGER)) {
						if (repeatable) {
							document.addNumberSortable(name, (Integer[])values);
						}
						else {
							document.addNumberSortable(
								name, (Integer)values[0]);
						}
					}
					else if (dataType.equals(FieldConstants.LONG)) {
						if (repeatable) {
							document.addNumberSortable(name, (Long[])values);
						}
						else {
							document.addNumberSortable(name, (Long)values[0]);
						}
					}
					else if (dataType.equals(FieldConstants.FLOAT)) {
						if (repeatable) {
							document.addNumberSortable(name, (Float[])values);
						}
						else {
							document.addNumberSortable(name, (Float)values[0]);
						}
					}
					else if (dataType.equals(FieldConstants.NUMBER) &&
							 repeatable) {

						document.addNumberSortable(name, (Double[])values);
					}
					else {
						String valueString = (String)values[0];

						String type = ddmFormField.getType();

						if (type.equals(DDMFormFieldType.GEOLOCATION)) {
							JSONObject jsonObject =
								JSONFactoryUtil.createJSONObject(valueString);

							double latitude = jsonObject.getDouble(
								"latitude", 0);
							double longitude = jsonObject.getDouble(
								"longitude", 0);

							document.addGeoLocation(
								name.concat("_geolocation"), latitude,
								longitude);
						}
						else if (type.equals(DDMImpl.TYPE_SELECT)) {
							JSONArray jsonArray =
								JSONFactoryUtil.createJSONArray(valueString);

							String[] stringArray = ArrayUtil.toStringArray(
								jsonArray);

							document.addKeywordSortable(name, stringArray);
						}
						else {
							if (type.equals(DDMImpl.TYPE_DDM_TEXT_HTML)) {
								valueString = HtmlUtil.extractText(valueString);
							}

							if (indexType.equals("keyword")) {
								document.addKeywordSortable(name, valueString);
							}
							else {
								document.addTextSortable(name, valueString);
							}
						}
					}
				}
			}
			catch (Exception e) {
				if (_log.isWarnEnabled()) {
					_log.warn(e, e);
				}
			}
		}
	}

	@Override
	public QueryFilter createFieldValueQueryFilter(
			String ddmStructureFieldName, Serializable ddmStructureFieldValue,
			Locale locale)
		throws Exception {

		BooleanQuery booleanQuery = new BooleanQueryImpl();

		String[] ddmStructureFieldNameParts = StringUtil.split(
			ddmStructureFieldName, DDMIndexer.DDM_FIELD_SEPARATOR);

		DDMStructure structure = _ddmStructureLocalService.getStructure(
			GetterUtil.getLong(ddmStructureFieldNameParts[2]));

		String fieldName = StringUtil.replaceLast(
			ddmStructureFieldNameParts[3],
			StringPool.UNDERLINE.concat(LocaleUtil.toLanguageId(locale)),
			StringPool.BLANK);

		if (structure.hasField(fieldName)) {
			ddmStructureFieldValue = _ddm.getIndexedFieldValue(
				ddmStructureFieldValue, structure.getFieldType(fieldName));
		}

		booleanQuery.addRequiredTerm(
			ddmStructureFieldName,
			StringPool.QUOTE + ddmStructureFieldValue + StringPool.QUOTE);

		return new QueryFilter(booleanQuery);
	}

	@Override
	public String encodeName(long ddmStructureId, String fieldName) {
		return encodeName(ddmStructureId, fieldName, null);
	}

	@Override
	public String encodeName(
		long ddmStructureId, String fieldName, Locale locale) {

		String indexType = StringPool.BLANK;

		if (ddmStructureId > 0) {
			DDMStructure ddmStructure =
				_ddmStructureLocalService.fetchDDMStructure(ddmStructureId);

			if (ddmStructure != null) {
				try {
					indexType = ddmStructure.getFieldProperty(
						fieldName, "indexType");
				}
				catch (PortalException pe) {
					throw new IllegalArgumentException(
						"Unable to obtain index tpe for field " + fieldName +
							" and DDM structure ID " + ddmStructureId,
						pe);
				}
			}
		}

		return encodeName(ddmStructureId, fieldName, locale, indexType);
	}

	@Override
	public String extractIndexableAttributes(
		DDMStructure ddmStructure, DDMFormValues ddmFormValues, Locale locale) {

		Format dateFormat = FastDateFormatFactoryUtil.getSimpleDateFormat(
			PropsUtil.get(PropsKeys.INDEX_DATE_FORMAT_PATTERN));

		StringBundler sb = new StringBundler();

		Map<String, DDMFormField> ddmFormFieldsMap =
			ddmStructure.getFullHierarchyDDMFormFieldsMap(true);

		Collection<DDMFormField> ddmFormFields = ddmFormFieldsMap.values();

		Map<String, List<DDMFormFieldValue>> ddmFormFieldValuesMap =
			getAllDDMFormFieldValuesMap(ddmFormValues.getDDMFormFieldValues());

		for (DDMFormField ddmFormField : ddmFormFields) {
			try {
				String indexType = ddmStructure.getFieldProperty(
					ddmFormField.getName(), "indexType");

				if (Validator.isNull(indexType)) {
					continue;
				}

				List<DDMFormFieldValue> ddmFormFieldValues =
					ddmFormFieldValuesMap.get(ddmFormField.getName());

				if (ddmFormFieldValues.isEmpty()) {
					continue;
				}

				String dataType = ddmFormField.getDataType();

				List<String> valuesStrings = getValuesStrings(
					ddmFormFieldValues, locale);

				if (dataType.equals(FieldConstants.BOOLEAN)) {
					Boolean value = GetterUtil.getBoolean(valuesStrings.get(0));

					sb.append(value);

					sb.append(StringPool.SPACE);
				}
				else if (dataType.equals(FieldConstants.NUMBER)) {
					Number value = GetterUtil.getNumber(valuesStrings.get(0));

					sb.append(value);

					sb.append(StringPool.SPACE);
				}
				else if (dataType.equals(FieldConstants.DATE)) {
					boolean repeatable = ddmFormField.isRepeatable();

					Date[] values = (Date[])getValuesAsArray(
						repeatable, dataType, valuesStrings);

					if (repeatable) {
						for (Date value : values) {
							sb.append(dateFormat.format(value));
							sb.append(StringPool.SPACE);
						}
					}
					else {
						sb.append(dateFormat.format(values[0]));
						sb.append(StringPool.SPACE);
					}
				}
				else {
					String valueString = valuesStrings.get(0);

					String type = ddmFormField.getType();

					if (type.equals(DDMImpl.TYPE_SELECT)) {
						JSONArray jsonArray = JSONFactoryUtil.createJSONArray(
							valueString);

						String[] stringArray = ArrayUtil.toStringArray(
							jsonArray);

						sb.append(stringArray);

						sb.append(StringPool.SPACE);
					}
					else {
						if (type.equals(DDMImpl.TYPE_DDM_TEXT_HTML)) {
							valueString = HtmlUtil.extractText(valueString);
						}

						sb.append(valueString);
						sb.append(StringPool.SPACE);
					}
				}
			}
			catch (Exception e) {
				if (_log.isWarnEnabled()) {
					_log.warn(e, e);
				}
			}
		}

		return sb.toString();
	}

	protected String encodeName(
		long ddmStructureId, String fieldName, Locale locale,
		String indexType) {

		StringBundler sb = new StringBundler(8);

		sb.append(DDM_FIELD_PREFIX);

		if (Validator.isNotNull(indexType)) {
			sb.append(indexType);
			sb.append(DDM_FIELD_SEPARATOR);
		}

		sb.append(ddmStructureId);
		sb.append(DDM_FIELD_SEPARATOR);
		sb.append(fieldName);

		if (locale != null) {
			sb.append(StringPool.UNDERLINE);
			sb.append(LocaleUtil.toLanguageId(locale));
		}

		return sb.toString();
	}

	protected Map<String, List<DDMFormFieldValue>> getAllDDMFormFieldValuesMap(
		List<DDMFormFieldValue> ddmFormFieldValues) {

		Map<String, List<DDMFormFieldValue>> ddmFormFieldValuesMap =
			new LinkedHashMap<>();

		for (DDMFormFieldValue ddmFormFieldValue : ddmFormFieldValues) {
			List<DDMFormFieldValue> listDDMFormFieldValues =
				ddmFormFieldValuesMap.get(ddmFormFieldValue.getName());

			if (listDDMFormFieldValues == null) {
				listDDMFormFieldValues = new ArrayList<>();

				ddmFormFieldValuesMap.put(
					ddmFormFieldValue.getName(), listDDMFormFieldValues);
			}

			listDDMFormFieldValues.add(ddmFormFieldValue);

			if (!ddmFormFieldValue.getNestedDDMFormFieldValues().isEmpty()) {
				Map<String, List<DDMFormFieldValue>>
					nestedDDMFormFieldValuesMap = getAllDDMFormFieldValuesMap(
						ddmFormFieldValue.getNestedDDMFormFieldValues());

				ddmFormFieldValuesMap.putAll(nestedDDMFormFieldValuesMap);
			}
		}

		return ddmFormFieldValuesMap;
	}

	protected Object[] getValuesAsArray(
			boolean repeatable, String valueType, List<String> valuesStrings)
		throws ParseException {

		String[] valuesStringsArray = valuesStrings.toArray(
			new String[valuesStrings.size()]);

		if (valueType.equals(FieldConstants.DATE)) {
			Date[] dateValues = new Date[valuesStringsArray.length];

			DateFormat dateFormat = DateFormatFactoryUtil.getSimpleDateFormat(
				"yyyy-MM-dd");

			dateValues = GetterUtil.getDateValues(
				valuesStringsArray, dateFormat);

			return dateValues;
		}
		else if (valueType.equals(FieldConstants.INTEGER)) {
			Integer[] integerValues = new Integer[valuesStringsArray.length];

			for (int i = 0; i < valuesStringsArray.length; i++) {
				integerValues[i] = GetterUtil.getInteger(valuesStringsArray[i]);
			}

			return integerValues;
		}
		else if (valueType.equals(FieldConstants.LONG)) {
			Long[] longValues = new Long[valuesStringsArray.length];

			for (int i = 0; i < valuesStringsArray.length; i++) {
				longValues[i] = GetterUtil.getLong(valuesStringsArray[i]);
			}

			return longValues;
		}
		else if (valueType.equals(FieldConstants.FLOAT)) {
			Float[] floatValues = new Float[valuesStringsArray.length];

			for (int i = 0; i < valuesStringsArray.length; i++) {
				floatValues[i] = GetterUtil.getFloat(valuesStringsArray[i]);
			}

			return floatValues;
		}
		else if (valueType.equals(FieldConstants.DOUBLE) ||
				 (valueType.equals(FieldConstants.NUMBER) && repeatable)) {

			Double[] doubleValues = new Double[valuesStringsArray.length];

			for (int i = 0; i < valuesStringsArray.length; i++) {
				doubleValues[i] = GetterUtil.getDouble(valuesStringsArray[i]);
			}

			return doubleValues;
		}
		else {
			return valuesStringsArray;
		}
	}

	protected List<String> getValuesStrings(
		List<DDMFormFieldValue> ddmFormFieldValues, Locale locale) {

		List<String> values = new ArrayList<>();

		for (DDMFormFieldValue ddmFormFieldValue : ddmFormFieldValues) {
			if (Validator.isNotNull(ddmFormFieldValue.getValue())) {
				values.add(ddmFormFieldValue.getValue().getString(locale));
			}
		}

		return values;
	}

	@Reference(unbind = "-")
	protected void setDDM(DDM ddm) {
		_ddm = ddm;
	}

	@Reference(unbind = "-")
	protected void setDDMStructureLocalService(
		DDMStructureLocalService ddmStructureLocalService) {

		_ddmStructureLocalService = ddmStructureLocalService;
	}

	private static final Log _log = LogFactoryUtil.getLog(DDMIndexerImpl.class);

	private DDM _ddm;
	private DDMStructureLocalService _ddmStructureLocalService;

}