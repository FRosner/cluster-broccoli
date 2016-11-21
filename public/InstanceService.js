angular.module('broccoli')
    .service('InstanceService', function(Restangular){
      this.getInstances= function (template) {    
      	if(!template){
          return Restangular.all("instances").getList();
      	}
        else{
          return Restangular.all("instances")
          .getList({ "templateId" : template.id });
          }
        };

        this.submitStatus=  function (instance, status) {
          return Restangular.all("instances")
            .customPOST({ "status": status }, instance.id, {}, {})        
          };

        this.deleteInstance = function (template, instance) {
          instance.remove();
        };
        
        this.createInstance = function(template , paramsToValue) {
          return Restangular.all("instances")
          .post({
              templateId: template.id,
              parameters: paramsToValue
            })      
        };    

        this.editInstance = function(postData, newInstance) {
          return Restangular.all("instances")
          .customPOST(postData, newInstance.id, {}, {});          
          };
        });
