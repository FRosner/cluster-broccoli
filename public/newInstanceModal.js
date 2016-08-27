angular.module('broccoli')
  .controller('NewInstanceCtrl', function ($scope, $uibModalInstance, template, instance) {
    var vm = this;
    vm.templateId = template.id;
    vm.parameters = template.parameters;

    if (instance == null) {
      vm.okText = "Create instance";
      vm.paramsToValue = {};
    } else {
      vm.okText = "Edit instance";
      vm.paramsToValue = instance.parameterValues;
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
});
