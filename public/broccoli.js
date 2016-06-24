angular.module('broccoli', ['restangular', 'ui.bootstrap'])
  .controller('AppsCtrl', function(Restangular, $uibModal, $scope, $timeout) {
    var vm = this;
    vm.templates = {};

    Restangular.setBaseUrl("/api/v1");

    function updateInstances(template) {
      Restangular.all("instances").getList({ "templateId" : template.id }).then(function(instances) {
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
        .customPOST('"' + status + '"', instance.id, {}, {})
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

    function openModal(templateApp) {
      var modalInstance = $uibModal.open({
        animation: true,
        templateUrl: '/assets/newInstanceModal.html',
        controller: 'NewInstanceCtrl',
        controllerAs: 'instCtrl',
        size: undefined,
        resolve: {
          templateId: function () {
            return templateApp.id;
          },
          parameters: function () {
            return templateApp.parameters;
          }
        }
      });

      modalInstance.result.then(function (paramsToValue) {
        Restangular.all("instances").post({
          templateId: templateApp.id,
          parameters: paramsToValue
        }).then(function(newInstance) {
          templateApp.instances.push(newInstance);
        }, function(error) {
          console.log("There was an error creating");
          console.log(error);
        });
      });
    };
    vm.openModal = openModal;

    updateTemplates();
  });
