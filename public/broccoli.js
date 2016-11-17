angular.module('broccoli', ['restangular', 'ui.bootstrap'])
  .controller('MainController', function(Restangular, AboutService, TemplateService, InstanceService ,$scope, $rootScope, $timeout) {
      var vm = this;
      vm.templates = {};
      vm.about = {};

      $rootScope.broccoliReachable = true;
      $rootScope.dismissRestangularError = function() {
          $rootScope.restangularError = null;
      };

      Restangular.setBaseUrl("/api/v1");

      Restangular.addResponseInterceptor(function(data, operation, what, url, response, deferred) {
        $rootScope.broccoliReachable = true;
        if (url != "/api/v1/status") {
          $rootScope.isLoggedIn = true;
        }
        return data;
      });

      Restangular.setErrorInterceptor(function(response, deferred, responseHandler) {
        if (response.status == -1) {
          $rootScope.broccoliReachable = false;
          $rootScope.isLoggedIn = false;
        } else if (response.status == 403) {
          $rootScope.isLoggedIn = false;
        } else {
          $rootScope.restangularError = response.statusText + " (" + response.status + "): " + response.data;
        }
        return false;
      });

      function askStatus() {
        AboutService.getStatus().then(function(about) {
          vm.about = about;
        });
      }

      function refreshStatus() {
        Restangular.one("status").get();
        $timeout(function() {
          refreshStatus()
        }, 1000);
      }

      refreshStatus();

      $rootScope.$watch('isLoggedIn', function(val) {
        if(val) {
          AboutService.getStatus().then(function(about) {
            vm.about = about;
          });
          refreshTemplates();
        } else {
          vm.about = {};
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
          if ($scope.isLoggedIn) {
            $timeout(function () {
              refreshInstances(template);
            }, 1000);
          }
      }

      refreshTemplates();
  }).directive('focusField', function() {
    return {
      restrict: 'A',

      link: function(scope, element, attrs) {
        scope.$watch(function() {
          return scope.$eval(attrs.focusField);
        }, function (newValue) {
          if (newValue === true) {
            setTimeout(function() { element[0].focus(); }, 10);
          }
        });
      }
    }
  });
