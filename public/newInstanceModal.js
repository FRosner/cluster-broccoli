angular.module('broccoli')
  .controller('NewInstanceCtrl', function ($scope, $rootScope, $uibModalInstance, template, instance) {
    var vm = this;
    vm.templateId = template.id;
    var realTemplate = null;
    if (instance == null) {
      vm.panelTitle = "New " + template.id + " (" +  template.version.substring(0, 8) + ")";
      vm.okText = "Create instance";
      vm.paramsToValue = {};
      realTemplate = template;
    } else {
      vm.panelTitle = "Edit " + instance.id + " (" + template.id + ", " + instance.template.version.substring(0, 8) + ")";
      vm.okText = "Edit instance";
      vm.paramsToValue = instance.parameterValues;
      realTemplate = instance.template;
    }
    vm.parameters = {};
    realTemplate.parameters.map(function(parameter) {
      var currentParameter = {
        "id": parameter,
        "name": parameter
      };
      if (template.parameterInfos[parameter] && template.parameterInfos[parameter].default) {
        currentParameter.default = template.parameterInfos[parameter].default;
      }
      vm.parameters[parameter] = currentParameter;
    });
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
        "instance": instance
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
