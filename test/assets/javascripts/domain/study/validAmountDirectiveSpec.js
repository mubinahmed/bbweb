/**
 * Jasmine test suite
 */
define(['angular', 'angularMocks', 'underscore', 'biobankApp'], function(angular, mocks, _) {
  'use strict';

  describe('Directive: validAmount', function() {
    var scope, form;

    beforeEach(mocks.module('biobankApp'));
    beforeEach(mocks.module('biobank.test'));

    beforeEach(inject(function ($rootScope, $compile) {
      var element;

      scope = $rootScope;

      element = angular.element(
        '<form name="testForm">' +
          '  <input type="number"' +
          '         name="theNumber"' +
          '         ng-model="theNumber"' +
          '         valid-amount' +
          '         required />' +
          '</form>');

      scope.theNumber = null;
      $compile(element)(scope);
      scope.$digest();
      form = scope.testForm;
    }));

    it('should allow a positive integer', function() {
      var anInteger = 1;
      form.theNumber.$setViewValue(anInteger.toString());
      expect(scope.theNumber).toEqual(anInteger);
      expect(form.theNumber.$valid).toBe(true);
    });

    it('should not allow a negative integer', function() {
      var anInteger = -1;
      form.theNumber.$setViewValue(anInteger.toString());
      expect(scope.theNumber).toBeUndefined();
      expect(form.theNumber.$valid).toBe(false);
    });

    it('should allow a positive floating point', function() {
      var aFloat = 1.10;
      form.theNumber.$setViewValue(aFloat.toString());
      expect(scope.theNumber).toEqual(aFloat);
      expect(form.theNumber.$valid).toBe(true);
    });

    it('should not allow a negative floating point', function() {
      var aFloat = -1.10;
      form.theNumber.$setViewValue(aFloat.toString());
      expect(scope.theNumber).toBeUndefined();
      expect(form.theNumber.$valid).toBe(false);
    });

    it('should not allow an alphanumeric value', function() {
      var aString = 'x1.10';
      form.theNumber.$setViewValue(aString);
      expect(scope.theNumber).toBeUndefined();
      expect(form.theNumber.$valid).toBe(false);
    });

  });

});
