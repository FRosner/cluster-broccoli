angular.module('broccoli')
  .controller('NewInstanceCtrl', function ($scope, $uibModalInstance, templateId, parameters) {
    var vm = this;
    vm.templateId = templateId;
    vm.parameters = parameters;
    vm.paramsToValue = {};

    vm.ok = ok;
    vm.cancel = cancel;

    function ok() {
      $uibModalInstance.close(vm.paramsToValue);
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
