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
});
