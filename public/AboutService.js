angular.module('broccoli')
    .service('AboutService', function(Restangular) {
        this.getStatus = function() {
            return Restangular.one("about").get();
        }
    });