AUI.add(
	'liferay-ddm-form-field-captcha',
	function(A) {
		var CaptchaField = A.Component.create(
			{
				ATTRS: {
					type: {
						value: 'captcha'
					}
				},

				EXTENDS: Liferay.DDM.Renderer.Field,

				NAME: 'liferay-ddm-form-field-captcha',

				prototype: {
					initializer: function() {
						var instance = this;

						instance._eventHandlers.push(
							instance.bindContainerEvent('click', instance._onClickRefresh, '.icon-refresh')
						);
					},

					getTemplate: function() {
						var instance = this;

						var container = instance.fetchContainer();

						if (!instance._formGroupNode) {
							instance._formGroupNode = container.one('.form-group');
						}

						var formGroupNode = instance._formGroupNode;

						if (formGroupNode.hasChildNodes()) {
							instance._formGroupNodeChildren = A.NodeList.create();

							var formGroupNodeChildren = formGroupNode.get('children');

							formGroupNodeChildren.each(
								function(item, index) {
									instance._formGroupNodeChildren.push(item.cloneNode(true));
								}
							);
						}

						var fieldName = formGroupNode.attr('data-fieldname');

						return '<div class="form-group" data-fieldname="' + fieldName + '"></div>';
					},

					getTemplateRenderer: function() {
						var instance = this;

						return A.bind('renderTemplate', instance);
					},

					getValue: function() {
						return '';
					},

					render: function() {
						var instance = this;

						var container = instance.get('container');

						container.empty();

						var formGroupNode = instance._formGroupNode;

						if (!formGroupNode.hasChildNodes()) {
							formGroupNode.setHTML(instance._formGroupNodeChildren);
						}

						formGroupNode.appendTo(container);

						return instance;
					},

					renderTemplate: function() {
						var instance = this;

						return instance._valueContainer().html();
					},

					_onClickRefresh: function() {
						var instance = this;

						var container = instance.get('container');

						var captchaNode = container.one('.captcha');

						var baseURL = captchaNode.attr('src');

						var url = Liferay.Util.addParams('t=' + Date.now(), baseURL);

						captchaNode.attr('src', url);
					}
				}
			}
		);

		Liferay.namespace('DDM.Field').Captcha = CaptchaField;
	},
	'',
	{
		requires: ['liferay-ddm-form-renderer-field']
	}
);