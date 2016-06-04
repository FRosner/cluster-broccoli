angular.module('broccoli', ['restangular'])
    .controller('AppsCtrl', function(Restangular) {
        var vm = this;
        vm.apps = [
          {
            name: "jupyter",
            description: "super\nfast",
            imageUrl: "assets/jupyter.svg",
            instances: [
              {
                id: 1,
                url: "localhost:9000/templates/zeppelin",
                status: "running"
              }
            ]
          }
        ];
        Restangular.all('templates').getList().then(function(templates) {
          templates.forEach(function(template) {
            Restangular.one('templates', template).get().then(function(template){
              template.description = JSON.stringify(template.parameters);
              vm.apps.push(template);
            });
          })
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
