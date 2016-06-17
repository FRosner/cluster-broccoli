angular.module('broccoli', ['restangular', 'ui.bootstrap'])
    .controller('AppsCtrl', function(Restangular, $uibModal, $scope, $timeout) {
        var vm = this;
        vm.templates = {};

        vm.openModal = openModal;

        updateTemplates();

        function updateInstances(template) {
          console.log("Updating instance of template " + template.id)
          Restangular.all("instances").getList({ "templateId" : template.id }).then(function(instances) {
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
          Restangular.all("templates").getList().then(function(templateIds) {
            templateIds.forEach(function(templateId) {
              Restangular.one("templates", templateId).get().then(function(template){
                template.imageUrl = "/assets/" + template.id + ".svg"
                template.instances = {};
                vm.templates[template.id] = template;
                updateInstances(template);
              });
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
    });
