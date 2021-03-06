define(['faker', 'underscore'], function(faker, _) {
  'use strict';

  /**
   * Taken from fixer version 2.1.2. When karma-fixer uses the same version this
   * function can be removed.
   */
  function uuid() {
    var RFC4122_TEMPLATE = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx';
    var replacePlaceholders = function (placeholder) {
      var random = Math.random()*16|0;
      var value = placeholder == 'x' ? random : (random &0x3 | 0x8);
      return value.toString(16);
    };
    return RFC4122_TEMPLATE.replace(/[xy]/g, replacePlaceholders);
  }

  function randomBoolean() {
    return faker.random.number() === 1;
  }

  function fakeModal() {
    return {
      result: {
        then: function(confirmCallback, cancelCallback) {
          //Store the callbacks for later when the user clicks on the OK or Cancel button of the dialog
          this.confirmCallBack = confirmCallback;
          this.cancelCallback = cancelCallback;
        }
      },
      close: function( item ) {
        //The user clicked OK on the modal dialog, call the stored confirm callback with the selected item
        this.result.confirmCallBack( item );
      },
      dismiss: function( type ) {
        //The user clicked cancel on the modal dialog, call the stored cancel callback
        this.result.cancelCallback( type );
      }
    };
  }

  function addCustomMatchers() {
    jasmine.addMatchers({
      toContainAll: function(util, customEqualityTesters) {
        return {
          compare: function(actual, expected) {
            return { pass: _.each(expected, function (item) {
              return util.contains(actual, item, customEqualityTesters); })
              };
          }
        };
      }
    });
  }

  function putHtmlTemplate($templateCache, template) {
    $templateCache.put(template,
      jasmine.getFixtures().getFixtureHtml_('../../../base/app' + template));
  }

  function camelCaseToUnderscore(str) {
    var result;
    if (_.isUndefined(str)) {
      throw new Error('string is undefined');
    }
    result = str.charAt(0).toUpperCase() + str.slice(1)
      .replace(/([A-Z])/g, '_$1').toUpperCase()
      .replace(' ', '');
    return result;
  };

  return {
    uuid:                  uuid,
    randomBoolean:         randomBoolean,
    fakeModal:             fakeModal,
    addCustomMatchers:     addCustomMatchers,
    putHtmlTemplate:       putHtmlTemplate,
    camelCaseToUnderscore: camelCaseToUnderscore
  };
});
