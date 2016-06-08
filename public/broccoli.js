angular.module('broccoli', ['restangular', 'ui.bootstrap'])
    .controller('AppsCtrl', function(Restangular, $uibModal) {
        var vm = this;
        vm.apps = [];

        vm.openModal = openModal;

        activate();

        function activate() {
          Restangular.all("templates").getList().then(function(templates) {
            templates.forEach(function(template) {
              Restangular.one("templates", template).get().then(function(template){
                template.imageUrl = "/assets/" + template.id + ".svg"
                template.instances = [];
                Restangular.all("instances").getList({ "templateId" : template.id }).then(function(instances) {
                  instances.forEach(function(instance) {
                    template.instances.push(instance);
                  });
                });
                template.description = JSON.stringify(template.parameters);
                vm.apps.push(template);
              });
            });
          });
        }

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
            // todo post paramsToValue
          }, function () {
            console.log('Modal dismissed at: ' + new Date());
          });
        };
    });
