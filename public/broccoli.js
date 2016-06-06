angular.module('broccoli', ['restangular'])
    .controller('AppsCtrl', function(Restangular) {
        var vm = this;
        vm.apps = [];
        Restangular.all("templates").getList().then(function(templates) {
          templates.forEach(function(template) {
            Restangular.one("templates", template).get().then(function(template){
              template.imageUrl = "assets/" + template.id + ".svg"
              template.description = "Amazing notebook!"
              console.log(template);
              template.instances = [];
              Restangular.all("instances").getList({ "templateId" : template.id }).then(function(instances) {
                instances.forEach(function(instance) {
                  console.log(instance);
                  template.instances.push(instance);
                  console.log(template);
                });
              });
              template.description = JSON.stringify(template.parameters);
              vm.apps.push(template);
            });
          });
        });

        var separateCharacters = function(line) {
            var result = [];
            for (var i = 0; i < line.length; i++) {
                result.push(line[i]);
            }
            return result;
        };

        vm.findWords = function() {

        };
    });
