angular.module('broccoli')
  .service('AuthenticationService', function(Restangular) {
      this.login = function(credentials) {
        return Restangular.all("auth/login")
        .post(credentials)
      };
    this.logout = function() {
      return Restangular.one("auth/logout")
      .post()     
    };
  })