angular.module('broccoli')
  .service('InstanceService', function(Restangular, $rootScope, $timeout) {

    function refreshInstancesOnce() {
      Restangular.all("instances").getList().then(function(instances) {
        $rootScope._instances = instances;
      });
    }

    function refreshInstancesSoon() {
      $timeout(function () {
        refreshInstancesOnce();
      }, 500);
    }
    
    function refreshInstances() {
      refreshInstancesOnce();
      var pollingFrequency = ($rootScope._pollingFrequency > 1000) ? $rootScope._pollingFrequency : 1000;
      if ($rootScope._isLoggedIn) {
        $timeout(function () {
          refreshInstances();
        }, pollingFrequency);
      }
    }
    this.refreshInstances = refreshInstances;

    this.submitStatus=  function (instance, status) {
      refreshInstancesSoon();
      return Restangular.all("instances")
        .customPOST({ "status": status }, instance.id, {}, {});
    };
    

    this.deleteInstance = function (template, instance) {
      refreshInstancesSoon();
      instance.remove();
    };

    this.createInstance = function(template , paramsToValue) {
      refreshInstancesSoon();
      return Restangular.all("instances").post({
        templateId: template.id,
        parameters: paramsToValue
      });
    };

    this.editInstance = function(postData, newInstance) {
      refreshInstancesSoon();
      return Restangular.all("instances")
        .customPOST(postData, newInstance.id, {}, {});
    };
  });