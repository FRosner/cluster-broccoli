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
                resolve: {
                    template: function() {
                      return template;
                    },
                    instance: function() {
                      return null;
                    },
                    templates: function() {
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
                resolve: {
                    template: function() {
                      return template;
                    },
                    instance: function() {
                      return instance;
                    },
                    templates: function() {
                      return templates;
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
                    templates[result.selectedTemplate].parameters.forEach(function(parameter) {
                        parameterValuesForSelectedTemplate[parameter] = newInstance.parameterValues[parameter];
                    });
                    newInstance.parameterValues = parameterValuesForSelectedTemplate;
                }
                postData.parameterValues = newInstance.parameterValues;
                InstanceService.editInstance(postData, newInstance)
                    .then(function(result) {
                        console.log(result);
                        $rootScope.restangularError = null;
                    }, function(error) {
                        console.log("There was an error creating");
                        console.log(error);
                    });
            });
        };

        function submitStatus(instance, status) {
            InstanceService.submitStatus(instance, status)
                .then(function(updatedInstance) {
                    $rootScope.restangularError = null;
                    for (i in updatedInstance) {
                        instance[i] = updatedInstance[i];
                    };
                });
        }

        function deleteInstance(template, instance) {
            $rootScope.restangularError = null;
            InstanceService.deleteInstance(template, instance);
        }


        vm.deleteInstance = InstanceService.deleteInstance;

        $scope.submitStatus = submitStatus;

        vm.editInstance = editInstance;

        vm.createInstance = createInstance;
    });
