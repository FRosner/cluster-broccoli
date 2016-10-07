angular.module('broccoli', ['restangular', 'ui.bootstrap'])
.controller('MainController',['Restangular','crud_service','$uibModal','$scope','$rootScope','$timeout', function(Restangular, crud_service, $uibModal, $scope, $rootScope, $timeout) {

    var vm = this;
    vm.templates = crud_service.updateTemplates();
    
    $rootScope.dismissRestangularError = function() {
      console.log("dismiss");
      $rootScope.restangularError = null;
    };

    Restangular.setBaseUrl("/api/v1");

    vm.about = {}
    Restangular.one("about").get().then(function(about) {
      vm.about = about;
    });

    Restangular.setErrorInterceptor(function(response, deferred, responseHandler) {
      if (response.status == -1) {
        $rootScope.broccoliReachable = false;
      } else {
        $rootScope.restangularError = response.statusText + " (" + response.status + "): " + response.data;
      }
      return false;
    });
    
    vm.deleteInstance = crud_service.deleteInstance;

    $scope.submitStatus = crud_service.submitStatus;    

    vm.createInstance = crud_service.createInstance;
    
    vm.editInstance = crud_service.editInstance;

    crud_service.updateTemplates();
   
  }]);