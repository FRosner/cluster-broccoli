angular.module('broccoli', ['restangular', 'ui.bootstrap'])
  .controller('MainController', function(Restangular, $uibModal, $scope, $rootScope, $timeout) {
    var vm = this;
    vm.templates = {};
    $rootScope.broccoliReachable = true;

    Restangular.setBaseUrl("/api/v1");

    vm.about = {}
    Restangular.one("about").get().then(function(about) {
      vm.about = about;
    });

    Restangular.setErrorInterceptor(function(response, deferred, responseHandler) {
      if (response.status == -1) {
        $rootScope.broccoliReachable = false;
        return false;
      }
      return true;
    });

    function updateInstances(template) {
      Restangular.all("instances").getList({ "templateId" : template.id }).then(function(instances) {
        $rootScope.broccoliReachable = true;
        template.instances = {};
        instances.forEach(function(instance) {
          template.instances[instance.id] = instance;
        });
      });
      $timeout(function(){
        updateInstances(template);
      }, 1000);
    }

    function updateTemplates() {
      console.log("Updating templates")
      Restangular.all("templates").getList().then(function(templates) {
        templates.forEach(function(template) {
          template.instances = {};
          vm.templates[template.id] = template;
          updateInstances(template);
        });
      });
    }

    function submitStatus(instance, status) {
      Restangular.all("instances")
        .customPOST({ "status": status }, instance.id, {}, {})
        .then(function(updatedInstance) {
          for (i in updatedInstance) {
            instance[i] = updatedInstance[i];
          };
        });
    }

    function deleteInstance(template, instance) {
      delete template.instances[instance.id];
      instance.remove();
    }
    vm.deleteInstance = deleteInstance;

    $scope.submitStatus = submitStatus;

    function createInstance(template) {
      var modalInstance = $uibModal.open({
        animation: true,
        templateUrl: '/assets/newInstanceModal.html',
        controller: 'NewInstanceCtrl',
        controllerAs: 'instCtrl',
        size: undefined,
        resolve: {
          template: function () {
            return template;
          },
          instance: function () {
            return null;
          }
        }
      });

      modalInstance.result.then(function (paramsToValueAndInstance) {
        var paramsToValue = paramsToValueAndInstance.paramsToValue;
        Restangular.all("instances").post({
          templateId: template.id,
          parameters: paramsToValue
        }).then(function(result) {
        }, function(error) {
          console.log("There was an error creating");
          console.log(error);
        });
      });
    };
    vm.createInstance = createInstance;

    function editInstance(template, instance) {
      var modalInstance = $uibModal.open({
        animation: true,
        templateUrl: '/assets/newInstanceModal.html',
        controller: 'NewInstanceCtrl',
        controllerAs: 'instCtrl',
        size: undefined,
        resolve: {
          template: function () {
            return template;
          },
          instance: function () {
            return instance;
          }
        }
      });

      modalInstance.result.then(function (paramsToValueAndInstance) {
        var paramsToValue = paramsToValueAndInstance.paramsToValue;
        var newInstance = paramsToValueAndInstance.instance;
        Restangular.all("instances")
          .customPOST({ "parameterValues": newInstance.parameterValues}, newInstance.id, {}, {})
          .then(function(result) {
        }, function(error) {
          console.log("There was an error creating");
          console.log(error);
        });
      });
    };
    vm.editInstance = editInstance;

    updateTemplates();
  });
