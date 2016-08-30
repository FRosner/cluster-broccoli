angular.module('broccoli', ['restangular', 'ui.bootstrap'])
  .controller('MainController', function(Restangular, $uibModal, $scope, $rootScope, $timeout) {
    var vm = this;
    vm.templates = {};
    $rootScope.broccoliReachable = true;
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
          $rootScope.restangularError = null;
          for (i in updatedInstance) {
            instance[i] = updatedInstance[i];
          };
        });
    }

    function deleteInstance(template, instance) {
      $rootScope.restangularError = null;
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
          },
          templates: function () {
            return null;
          }
        }
      });

      modalInstance.result.then(function (result) {
        var paramsToValue = result.paramsToValue;
        Restangular.all("instances").post({
          templateId: template.id,
          parameters: paramsToValue
        }).then(function(result) {
          $rootScope.restangularError = null;
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
          },
          templates: function () {
            return vm.templates;
          }
        }
      });

      modalInstance.result.then(function (result) {
        var paramsToValue = result.paramsToValue;
        var newInstance = result.instance;
        var postData = { "parameterValues": newInstance.parameterValues };
        if (result.selectedTemplate != null && result.selectedTemplate != "unchanged") {
          postData['selectedTemplate'] = result.selectedTemplate;
        }
        Restangular.all("instances")
          .customPOST(postData, newInstance.id, {}, {})
          .then(function(result) {
            $rootScope.restangularError = null;
          }, function(error) {
            console.log("There was an error creating");
            console.log(error);
          });
      });
    };
    vm.editInstance = editInstance;

    updateTemplates();
  });
