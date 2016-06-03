angular.module('broccoli', [])
    .controller('AppsCtrl', [function() {
        var vm = this;
        vm.apps = [
          {
            name: "jupyter",
            description: "super\nfast",
            imageUrl: "https://en.wikipedia.org/wiki/Jupiter#/media/File:Jupiter_and_its_shrunken_Great_Red_Spot.jpg",
            instances: [
              {
                id: 1,
                url: "localhost:9000/templates/zeppelin",
                status: "running"
              }
            ]
          }
        ];

        var separateCharacters = function(line) {
            var result = [];
            for (var i = 0; i < line.length; i++) {
                result.push(line[i]);
            }
            return result;
        };

        vm.findWords = function() {

        };
    }]);

/*.controller('AppsCtrl', function(Restangular) {
      var vm = this;
      vm.apps = ['abc'];
    });
*/
