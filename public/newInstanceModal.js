angular.module('broccoli')
  .controller('NewInstanceCtrl', function ($scope, $uibModalInstance, templateId, parameters) {
    var vm = this;
    vm.templateId = templateId;
    vm.parameters = parameters;
    vm.paramsToValue = {};

    vm.click = click;
    vm.ok = ok;
    vm.cancel = cancel;

    activate();

    function activate() {
      console.log('modal');
    }

    function click() {
      console.log(vm.paramsToValue);
    }

    function ok() {
      $uibModalInstance.close(paramsToValue);
    };

    function cancel() {
      $uibModalInstance.dismiss();
    };
});
