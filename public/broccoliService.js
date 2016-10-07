angular.module('broccoli')
.factory('crud_service', ['Restangular','$uibModal','$rootScope','$timeout', function(Restangular,$uibModal,$rootScope,$timeout ){
  var vm = this;
  vm.templates = {};
  
  Restangular.setBaseUrl("/api/v1");
    
  var instance_service = {
      createInstance: createInstance,
      deleteInstance:deleteInstance,
      editInstance:editInstance,
      updateInstances:updateInstances,
      updateTemplates:updateTemplates,
      submitStatus:submitStatus     
  };

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
      //console.log("Updating templates")      
      Restangular.all("templates").getList().then(function(templates) {
        templates.forEach(function(template) {
          template.instances = {};
          vm.templates[template.id] = template;
          updateInstances(template);          
        });     
      });      
      return vm.templates;
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
        var parameterValuesForSelectedTemplate = {};
        var postData = {};
        if (result.selectedTemplate != null && result.selectedTemplate != "unchanged") {
          postData['selectedTemplate'] = result.selectedTemplate;
          vm.templates[result.selectedTemplate].parameters.forEach(function(parameter) {
            parameterValuesForSelectedTemplate[parameter] = newInstance.parameterValues[parameter];
          });
          newInstance.parameterValues = parameterValuesForSelectedTemplate;
        }
        postData.parameterValues = newInstance.parameterValues;
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
  
  return instance_service;

}])  