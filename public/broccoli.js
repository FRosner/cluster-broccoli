angular.module('broccoli', ['restangular', 'ui.bootstrap'])
    .controller('MainController', function(Restangular, AboutService, TemplateService, InstanceService ,$scope, $rootScope, $timeout) {
        var vm = this;
        vm.templates = {};
        vm.about = {}

        $rootScope.broccoliReachable = true;
        $rootScope.dismissRestangularError = function() {
            $rootScope.restangularError = null;
        };

        Restangular.setBaseUrl("/api/v1");

        Restangular.addResponseInterceptor(function(data, operation, what, url, response, deferred) {
          $rootScope.broccoliReachable = true;
          $rootScope.isLoggedIn = true;
          return data;
        });

        Restangular.setErrorInterceptor(function(response, deferred, responseHandler) {
          if (response.status == -1) {
            $rootScope.broccoliReachable = false;
          } else if (response.status == 403) {
            $rootScope.isLoggedIn = false;
          } else {
              $rootScope.restangularError = response.statusText + " (" + response.status + "): " + response.data;
          }
          return false;
        });


        $rootScope.$watch('isLoggedIn', function(val) {
          if(val) {
            AboutService.getStatus().then(function(about) {
            vm.about = about;
            });
            refreshTemplates();
          }
        });

        function refreshTemplates() {
            TemplateService.getTemplates().then(function(templates) {
                var templates = templates;
                templates.forEach(function(template) {
                    template.instances = {};
                    vm.templates[template.id] = template;
                    refreshInstances(template);
                    $scope.templates = templates;
                });
            });
        }

        function refreshInstances(template) {
            InstanceService.getInstances(template).then(function(instances) {
                $scope.instances = instances;
                template.instances = {};
                instances.forEach(function(instance) {
                    template.instances[instance.id] = instance;
                    return template.instances;
                });
            });
            $timeout(function() {
                refreshInstances(template);
            }, 1000);
        }

        refreshTemplates();
    });
