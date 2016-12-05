angular.module('broccoli', ['restangular', 'ui.bootstrap'])
  .controller('MainController', function(Restangular, AboutService, TemplateService, InstanceService ,$scope, $rootScope, $timeout) {
    var vm = this;
    vm.templates = {};
    $scope.isOpen = [];

    AboutService.refreshAbout();

    $rootScope.broccoliReachable = true;
    $rootScope.dismissRestangularError = function() {
        $rootScope.restangularError = null;
    };

    Restangular.setBaseUrl("/api/v1");

    Restangular.addResponseInterceptor(function(data, operation, what, url, response, deferred) {
      $rootScope.broccoliReachable = true;
      if (url != "/api/v1/status") {
        $rootScope._isLoggedIn = true;
      }
      return data;
    });

    Restangular.setErrorInterceptor(function(response, deferred, responseHandler) {
      if (response.status == -1) {
        $rootScope.broccoliReachable = false;
        $rootScope._isLoggedIn = false;
      } else if (response.config.url == "/api/v1/about") {
        $rootScope.broccoliReachable = true;
        if (response.status == 403) {
          $rootScope._isLoggedIn = false;
        }
      } else if (response.config.url == "/api/v1/auth/login") {
        $rootScope.restangularError = "Login failed!"
      } else if (response.config.url == "/api/v1/auth/logout") {
        $rootScope.restangularError = "Logout failed!"
      } else if ($scope._isLoggedIn) {
        $rootScope.restangularError = response.statusText + " (" + response.status + "): " + response.data;
      } else {
        // ignore error as it will anyway be logged by the browser in the console
      }
      return false;
    });

    $scope.allowedPollingFrequencies = [1000, 2000, 5000, 10000];

    $rootScope.$watch('_isLoggedIn', function(val) {
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
        $rootScope._about = {}
      }
    });

    function refreshTemplates() {
        TemplateService.getTemplates().then(function(templates) {
          $scope.templates = templates;
          InstanceService.refreshInstances();
        });
    }

    function copyValue(secret){
      var dummy = document.createElement("input");
      document.body.appendChild(dummy);
      dummy.setAttribute("id", "dummy_id");
      document.getElementById("dummy_id").value=secret;
      dummy.select();
      document.execCommand("copy");
      document.body.removeChild(dummy);
    }

    function isCollapsed(instance) {
      $scope.isOpen[instance.id] = !($scope.isOpen[instance.id]);
      }

    $scope.copyValue = copyValue;
    $scope.isCollapsed = isCollapsed;

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
  }).filter('toArray', function() { return function(obj) {
    if (!(obj instanceof Object)) return obj;
    return _.map(obj, function(val, key) {
      return Object.defineProperty(val, '$key', {__proto__: null, value: key});
    });
  }}).filter('belongsToTemplate', function() {
    return function(instances, template) {
      var filteredInstances = instances.filter(function(instance) {
        return instance.template.id == template.id;
      });
      return filteredInstances;
    }
  });
