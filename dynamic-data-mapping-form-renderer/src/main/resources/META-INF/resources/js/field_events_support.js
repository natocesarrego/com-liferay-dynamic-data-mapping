AUI.add(
	'liferay-ddm-form-renderer-field-events',
	function(A) {
		var FieldEventsSupport = function() {
		};

		FieldEventsSupport.prototype = {
			initializer: function() {
				var instance = this;

				instance._eventHandlers.push(
					instance.after(instance._afterEventsRender, instance, 'render')
				);

				instance._domEvents = [];

				instance._bindEvents();
			},

			bindContainerEvent: function(eventName, callback, selector) {
				var instance = this;

				var container = instance.get('container');

				var query = selector;

				if (query.call) {
					query = query.call(instance);
				}

				var handler = container.delegate(eventName, A.bind(callback, instance), query);

				instance._domEvents.push(
					{
						callback: callback,
						handler: handler,
						name: eventName,
						selector: selector
					}
				);

				return handler;
			},

			bindInputEvent: function(eventName, callback) {
				var instance = this;

				return instance.bindContainerEvent(eventName, callback, instance.getInputSelector);
			},

			_afterEventsRender: function() {
				var instance = this;

				var events = instance._domEvents;

				instance._domEvents = [];

				var length = events.length;

				while (length--) {
					var event = events[length];

					event.handler.detach();

					instance.bindContainerEvent(event.name, event.callback, event.selector);
				}
			},

			_bindEvents: function() {
				var instance = this;

				instance.bindInputEvent('blur', instance._onInputBlur, true);
				instance.bindInputEvent('focus', instance._onInputFocus, true);
				instance.bindInputEvent(['input', 'change'], instance._onValueChange);
			},

			_fireBlurEvent: function() {
				var instance = this;

				var root = instance.getRoot();

				if (root) {
					var now = new Date();

					Liferay.fire(
						'ddmFieldBlur',
						{
							fieldName: instance.get('name'),
							focusDuration: (now - (instance.get('fieldFocusDate') || now)),
							formId: root.getFormId(),
							page: root.getCurrentPage() - 1
						}
					);
				}
			},

			_fireFocusEvent: function() {
				var instance = this;

				var root = instance.getRoot();

				if (root) {
					instance.set('fieldFocusDate', new Date());

					Liferay.fire(
						'ddmFieldFocus',
						{
							fieldName: instance.get('name'),
							formId: root.getFormId(),
							page: root.getCurrentPage() - 1
						}
					);
				}
			},

			_fireStartedFillingEvent: function() {
				var instance = this;

				if (!instance.get('startedFilling')) {
					instance.set('startedFilling', true);

					var root = instance.getRoot();

					if (root) {
						Liferay.fire(
							'ddmFieldStartedFilling',
							{
								fieldName: instance.get('fieldName'),
								formId: root.getFormId(),
								page: root.getCurrentPage() || 1
							}
						);
					}
				}
			},

			_onInputBlur: function(event) {
				var instance = this;

				instance.fire(
					'blur',
					{
						domEvent: event,
						field: instance
					}
				);

				instance._fireBlurEvent();
			},

			_onInputFocus: function(event) {
				var instance = this;

				instance.fire(
					'focus',
					{
						domEvent: event,
						field: instance
					}
				);

				instance._fireFocusEvent();
			},

			_onValueChange: function(event) {
				var instance = this;

				instance.fire(
					'valueChanged',
					{
						domEvent: event,
						field: instance,
						value: instance.getValue()
					}
				);
			}
		};

		Liferay.namespace('DDM.Renderer').FieldEventsSupport = FieldEventsSupport;
	},
	'',
	{
		requires: []
	}
);