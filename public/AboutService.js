angular.module('broccoli')
  .service('AboutService', function(Restangular, $rootScope, $timeout) {
    Restangular.setBaseUrl("/api/v1");

    function refreshAbout() {
      Restangular.one("about").get().then(function(newAbout) {
        $rootScope._about = newAbout;
      });
      $timeout(function () {
        refreshAbout()
      }, 1000);
    }
    this.refreshAbout = refreshAbout;
  });