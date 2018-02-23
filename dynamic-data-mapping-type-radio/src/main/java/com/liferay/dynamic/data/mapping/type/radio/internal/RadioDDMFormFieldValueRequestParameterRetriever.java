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

package com.liferay.dynamic.data.mapping.type.radio.internal;

import com.liferay.dynamic.data.mapping.form.field.type.DDMFormFieldValueRequestParameterRetriever;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Marcellus Tavares
 */
@Component(immediate = true, property = "ddm.form.field.type.name=radio")
public class RadioDDMFormFieldValueRequestParameterRetriever
	implements DDMFormFieldValueRequestParameterRetriever {

	@Override
	public String get(
		HttpServletRequest httpServletRequest, String ddmFormFieldParameterName,
		String defaultDDMFormFieldParameterValue) {

		String[] defaultDDMFormFieldParameterValues;
		String[] parameterValues;

		if (Validator.isNull(defaultDDMFormFieldParameterValue)) {
			defaultDDMFormFieldParameterValues =
				GetterUtil.DEFAULT_STRING_VALUES;
		}
		else {
			defaultDDMFormFieldParameterValues =
				new String[] {defaultDDMFormFieldParameterValue};
		}

		parameterValues = ParamUtil.getParameterValues(
			httpServletRequest, ddmFormFieldParameterName,
			defaultDDMFormFieldParameterValues);

		return jsonFactory.serialize(parameterValues);
	}

	@Reference
	protected JSONFactory jsonFactory;

}