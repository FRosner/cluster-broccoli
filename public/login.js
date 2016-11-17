angular.module('broccoli')
  .controller('LoginCntrl', function(Restangular, AuthenticationService, $uibModal, $scope, $rootScope) {
    var vm = this;     
    function loginUser(credentials) {
        var modalInstance = $uibModal.open({
            animation: true,
            templateUrl: '/assets/login.html',
            controller: 'LoginModal',
            controllerAs: 'loginmodal',
            scope: $scope,
            size: undefined,
            resolve: {
                credentials: function() {
                  return null;
                }            
            }
        });
        modalInstance.result.then(function(response) {
          $scope.user = response.credentials.username;     
          var credentials = response.credentials;
          AuthenticationService.login(credentials).then(function(response) {
            $rootScope.restangularError = null;        
            $rootScope.isLoggedIn = true;
          }); 
        });
      };

    function logoutUser() {     
       AuthenticationService.logout().then(function(response) {
        $rootScope.isLoggedIn = false;
        });        
      };

    vm.loginUser = loginUser;
    vm.logoutUser = logoutUser;          
  })
