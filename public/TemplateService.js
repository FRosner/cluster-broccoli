angular.module('broccoli')
.service('TemplateService', function(Restangular){

  this.getTemplates = function() {
     return Restangular.all("templates").getList();     
    }
    
});
