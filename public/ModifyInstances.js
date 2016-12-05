angular.module('broccoli')
    .controller('ModifyInstanceCntrl', function(Restangular, InstanceService, $uibModal, $scope, $rootScope) {
        var vm = this;
        function createInstance(template) {
            var modalInstance = $uibModal.open({
                animation: true,
                templateUrl: '/assets/newInstanceModal.html',
                controller: 'NewInstanceCtrl',
                controllerAs: 'instCtrl',
                size: undefined,
                scope: $scope,
                resolve: {
                    template: function() {
                      return template;
                    },
                    instance: function() {
                      return null;
                    },
                    templates: function() {
                      return null;
                    },
                    deleteInstance:function(){
                      return null;
                    }
                }
            });
            modalInstance.result.then(function(result) {
                var paramsToValue = result.paramsToValue;
                InstanceService.createInstance(template, paramsToValue).then(function(result) {
                    $rootScope.restangularError = null;
                }, function(error) {
                    console.log("There was an error creating");
                    console.log(error);
                });
            });
        }

        function editInstance(template, instance, templates) {
            var modalInstance = $uibModal.open({
                animation: true,
                templateUrl: '/assets/newInstanceModal.html',
                controller: 'NewInstanceCtrl',
                controllerAs: 'instCtrl',
                size: undefined,
                scope: $scope,
                resolve: {
                    template: function() {
                      return template;
                    },
                    instance: function() {
                      return instance;
                    },
                    templates: function() {
                      return templates;
                    },
                    deleteInstance: function() {
                      return null;
                    }
                }
            });

            modalInstance.result.then(function(result) {
                var paramsToValue = result.paramsToValue;
                var newInstance = result.instance;
                var parameterValuesForSelectedTemplate = {};
                var postData = {};
                if (result.selectedTemplate != null && result.selectedTemplate != "unchanged") {
                    postData['selectedTemplate'] = result.selectedTemplate;
                    templates.find(function(t) {return t.id === result.selectedTemplate}).parameters.forEach(function(parameter) {
                        parameterValuesForSelectedTemplate[parameter] = newInstance.parameterValues[parameter];
                    });
                    newInstance.parameterValues = parameterValuesForSelectedTemplate;
                }
                postData.parameterValues = newInstance.parameterValues;
                InstanceService.editInstance(postData, newInstance)
                    .then(function(result) {
                        $rootScope.restangularError = null;
                    }, function(error) {
                        console.log("There was an error creating");
                        console.log(error);
                    });
            });
        };

        function submitStatus(instance, status) {
            InstanceService.submitStatus(instance, status);
        }

        function deleteInstance(template, instance) {
          $scope.deleteInstance = true;
          var modalInstance = $uibModal.open({
                animation: true,
                templateUrl:'/assets/newInstanceModal.html',
                controller: 'NewInstanceCtrl',
                controllerAs: 'instCtrl',
                size: undefined,
                scope: $scope,
                resolve: {
                    template: function() {
                      return template;
                    },
                    instance: function() {
                      return instance;
                    },
                    templates: function() {
                     return null;
                    },
                    deleteInstance: function() {
                      return $scope.deleteInstance;
                    }
                }
            });
          modalInstance.result.then(function(result) {
                $rootScope.restangularError = null;
                InstanceService.deleteInstance(template, instance)
            });
        }


        vm.deleteInstance = deleteInstance;

        $scope.submitStatus = submitStatus;

        vm.editInstance = editInstance;

        vm.createInstance = createInstance;
    });
