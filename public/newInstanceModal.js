angular.module('broccoli')
  .controller('NewInstanceCtrl', function ($scope, $rootScope, $uibModalInstance, template, instance) {
    var vm = this;
    vm.templateId = template.id;

    if (instance == null) {
      vm.panelTitle = "New " + template.id + " (" +  template.version.substring(0, 8) + ")";
      vm.okText = "Create instance";
      vm.paramsToValue = {};
      vm.parameterNames = template.parameterNames;
    } else {
      vm.panelTitle = "Edit " + instance.id + " (" + template.id + ", " + instance.template.version.substring(0, 8) + ")";
      vm.okText = "Edit instance";
      vm.paramsToValue = instance.parameterValues;
      vm.parameterNames = Object.keys(instance.parameterValues);
    }
    vm.ok = ok;
    vm.cancel = cancel;

    function ok() {
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
