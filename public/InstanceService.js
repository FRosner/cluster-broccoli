angular.module('broccoli')
  .service('InstanceService', function(Restangular, $rootScope, $timeout) {

    function refreshInstances() {
      Restangular.all("instances").getList().then(function(instances) {
        $rootScope._instances = instances;
      });
      var pollingFrequency = ($rootScope._pollingFrequency > 1000) ? $rootScope._pollingFrequency : 1000;
      if ($rootScope._isLoggedIn) {
        $timeout(function () {
          refreshInstances();
        }, pollingFrequency);
      }
    }
    this.refreshInstances = refreshInstances;

    this.submitStatus=  function (instance, status) {
      return Restangular.all("instances")
        .customPOST({ "status": status }, instance.id, {}, {})
      };

    this.deleteInstance = function (template, instance) {
      instance.remove();
    };

    this.createInstance = function(template , paramsToValue) {
      return Restangular.all("instances")
      .post({
          templateId: template.id,
          parameters: paramsToValue
        })
    };

    this.editInstance = function(postData, newInstance) {
      return Restangular.all("instances")
      .customPOST(postData, newInstance.id, {}, {});
        };
  });