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
      } else if (response.config.url == "/api/v1/about") {
        $rootScope.broccoliReachable = true;
        if (response.status == 403) {
          $rootScope.isLoggedIn = false;
        }
      } else if (response.config.url == "/api/v1/auth/login") {
        $rootScope.restangularError = "Login failed!"
      } else if (response.config.url == "/api/v1/auth/logout") {
        $rootScope.restangularError = "Logout failed!"
      } else if ($scope.isLoggedIn) {
        $rootScope.restangularError = response.statusText + " (" + response.status + "): " + response.data;
      } else {
        // ignore error as it will anyway be logged by the browser in the console
      }
      return false;
    });

    function refreshAbout() {
      AboutService.getStatus().then(function(about) {
        vm.about = about;
      });
      $timeout(function () {
        refreshAbout()
      }, 1000);
    }

    refreshAbout();

    $rootScope.$watch('isLoggedIn', function(val) {
      if(val) {
        $scope.pageLoading = true;
        function progressTo(i) {
          return function() {
            $scope.pageLoadingProgress = i;
          };
        }
        for (i = 0; i <= 100; i = i + 25) {
          $timeout(progressTo(i), i * 5);
        }

        $timeout(function() {
          $scope.pageLoading = false;
          progressTo(0);
        }, 1100);
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
