angular.module('broccoli')
  .controller('NewInstanceCtrl', function ($scope, $rootScope, $uibModalInstance, template, instance, templates, deleteInstance) {
    var vm = this;
    vm.templateId = template.id;
    var realTemplate = null;
    vm.instance = instance;
    vm.dropdown = {};
    vm.dropdown.templates = templates;
    vm.dropdown.selectedTemplate = "unchanged";
    vm.deleteInstance =deleteInstance;

    if (deleteInstance == null){
      if (instance == null) {
        vm.panelTitle = "New " + template.id + " (" +  template.version.substring(0, 8) + ")";
        vm.okText = "Create instance";
        vm.paramsToValue = {};
        realTemplate = template;
      } else {
        vm.panelTitle = "Edit " + instance.id + " (" + instance.template.id + ", " + instance.template.version.substring(0, 8) + ")";
        vm.okText = "Edit instance";
        vm.paramsToValue = instance.parameterValues;
        realTemplate = instance.template;
      }
    }
    else{
      vm.panelTitle = "Delete " + instance.id + " (" + instance.template.id + ", " + instance.template.version.substring(0, 8) + ")";
      vm.okText = "Delete instance";
      vm.paramsToValue = instance.parameterValues;
      realTemplate = instance.template;
    }

    $scope.$watch('instCtrl.dropdown.selectedTemplate', function(newTemplateId, oldTemplateId) {
      if (newTemplateId == "unchanged") {
        updateParameterForm(realTemplate)
      } else {
        var newTemplate = templates[newTemplateId];
        updateParameterForm(newTemplate);
      }
    });


    function updateParameterForm(currentTemplate) {
      vm.parameters = {};
      currentTemplate.parameters.map(function(parameter) {
        var currentParameter = {
          "id": parameter,
          "name": parameter
        };
        if (currentTemplate.parameterInfos[parameter] && currentTemplate.parameterInfos[parameter].default) {
          currentParameter.default = currentTemplate.parameterInfos[parameter].default;
        }
        vm.parameters[parameter] = currentParameter;
      });
    }
    vm.ok = ok;
    vm.cancel = cancel;

    function ok() {
        Object.keys(vm.paramsToValue).forEach(function(param) {
        if (vm.paramsToValue[param] == "") {
          delete vm.paramsToValue[param];
        }
      }); 
      $uibModalInstance.close({
        "paramsToValue": vm.paramsToValue,
        "instance": instance,
        "selectedTemplate": vm.dropdown.selectedTemplate
      });
    };

    function cancel() {
      $uibModalInstance.dismiss();
    };
})
  .directive('focusField', function() {
  return{
         restrict: 'A',

         link: function(scope, element, attrs){
           scope.$watch(function(){
             return scope.$eval(attrs.focusField);
             },function (newValue){
               if (newValue === true){
                   element[0].focus();
               }
           });
         }
     };
});
