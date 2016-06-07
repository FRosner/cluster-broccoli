angular.module('broccoli').controller('NewInstanceCtrl', function ($scope, $uibModalInstance) {

  $scope.ok = function () {
    $uibModalInstance.close('closing information');
  };

  $scope.cancel = function () {
    $uibModalInstance.dismiss('cancel');
  };
});
