angular.module('broccoli')
  .controller('LoginModal',  function($scope, $cookies, Restangular, $q, $timeout,$uibModalInstance){
      var vm = this;
      vm.ok =ok; 
      vm.credentials = {};
     function ok() {  
      $uibModalInstance.close({
        "credentials": vm.credentials
      });
    };
  });