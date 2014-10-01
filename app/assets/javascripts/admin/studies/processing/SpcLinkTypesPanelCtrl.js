define(['../../module', 'underscore'], function(module, _) {
  'use strict';

  module.controller('SpcLinkTypesPanelCtrl', SpcLinkTypesPanelCtrl);

  SpcLinkTypesPanelCtrl.$inject = [
    '$scope',
    '$state',
    '$stateParams',
    'panelService',
    'StudyService',
    'spcLinkTypeModalService',
    'spcLinkTypeRemoveService',
    'processingTypeModalService',
    'specimenGroupModalService',
    'annotTypeModalService'
  ];

  /**
   *
   */
  function SpcLinkTypesPanelCtrl($scope,
                                 $state,
                                 $stateParams,
                                 panelService,
                                 StudyService,
                                 spcLinkTypeModalService,
                                 spcLinkTypeRemoveService,
                                 processingTypeModalService,
                                 specimenGroupModalService,
                                 annotTypeModalService) {
    var vm = this;

    var helper = panelService.panel(
      'study.panel.specimenLinkTypes',
      'admin.studies.study.processing.spcLinkTypeAdd');

    vm.tableData = [];
    vm.update      = update;
    vm.remove      = spcLinkTypeRemoveService.remove;
    vm.add         = helper.add;
    vm.information = information;
    vm.panelOpen   = helper.panelOpen;
    vm.panelToggle = helper.panelToggle;

    vm.processingTypesById = _.indexBy($scope.processingDto.processingTypes, 'id');
    vm.specimenGroupsById = _.indexBy($scope.processingDto.specimenGroups, 'id');
    vm.annotTypesById = _.indexBy($scope.processingDto.specimenLinkAnnotationTypes, 'id');

    vm.showProcessingType  = showProcessingType;
    vm.showSpecimenGroup   = showSpecimenGroup;
    vm.showAnnotationType  = showAnnotationType;

    init();

    vm.tableParams = helper.getTableParams(vm.tableData);
    vm.tableParams.settings().$scope = $scope;  // kludge: see https://github.com/esvit/ng-table/issues/297#issuecomment-55756473

    //--

    function init() {
      _.each($scope.processingDto.specimenLinkTypes, function (slt) {
        var annotationTypes = [];
        _.each(slt.annotationTypeData, function (annotTypeItem) {
          var at = vm.annotTypesById[annotTypeItem.annotationTypeId];
          annotationTypes.push({id: annotTypeItem.annotationTypeId, name: at.name });
        });

        vm.tableData.push({
          specimenLinkType:   slt,
          processingTypeName: vm.processingTypesById[slt.processingTypeId].name,
          inputGroupName:     vm.specimenGroupsById[slt.inputGroupId].name,
          outputGroupName:    vm.specimenGroupsById[slt.outputGroupId].name,
          annotationTypes:    annotationTypes
        });
      });
    }

    function information(spcLinkType) {
      spcLinkTypeModalService.show(spcLinkType, vm.processingTypesById, vm.specimenGroupsById);
    }

    function update(spcLinkType) {
      $state.go(
        'admin.studies.study.processing.spcLinkTypeUpdate',
        { procTypeId:spcLinkType.processingTypeId, spcLinkTypeId: spcLinkType.id });
    }

    function showProcessingType(processingTypeId) {
      processingTypeModalService.show(vm.processingTypesById[processingTypeId]);
    }

    function showSpecimenGroup(specimenGroupId) {
      specimenGroupModalService.show(vm.specimenGroupsById[specimenGroupId]);
    }

    function showAnnotationType(annotTypeId) {
      annotTypeModalService.show('Specimen Link Annotation Type', vm.annotTypesById[annotTypeId]);
    }

  }

});