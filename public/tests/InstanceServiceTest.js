describe('Test Suite for InstanceService', function () {
  
  var Restangular, httpBackend, InstanceService;

  var template = [
    {
      "id": "http-server",
      "description": "A simple Python HTTP request handler. This class serves files from the current directory and below, directly mapping the directory structure to HTTP requests.",
      "parameters": [
        "id",
        "cpu"
      ],
      "parameterInfos": {
        "cpu": {
          "name": "cpu",
          "default": "100"
        }
      },
      "version": "f88dbbdc8249b8e5075598e165aec527"
    }
  ]

  var created_instance = {
    "id": "my-http",
    "parameterValues": {
      "id": "my-http",
      "cpu": "250"
    },
    "status": "stopped",
    "services": {},
    "template": {
      "id": "http-server",
      "description": "A simple Python HTTP request handler. This class serves files from the current directory and below, directly mapping the directory structure to HTTP requests.",
      "parameters": [
        "id",
        "cpu"
      ],
      "parameterInfos": {
        "cpu": {
          "name": "cpu",
          "default": "100"
        }
      },
      "version": "f88dbbdc8249b8e5075598e165aec527"
    }
  };

  var params_posted = {
    "parameters":
      {
       "id": "my-http", 
       "cpu": "250" 
      }
    };

  beforeEach(angular.mock.module('broccoli'));

  beforeEach(inject(function(_$httpBackend_, _Restangular_,_InstanceService_){
      httpBackend = _$httpBackend_;      
      Restangular = _Restangular_;
      InstanceService = _InstanceService_;   
      Restangular.setBaseUrl("/api/v1");    
    }));

  afterEach(function() {
    httpBackend.verifyNoOutstandingExpectation();
    httpBackend.verifyNoOutstandingRequest();
  });

  describe('InstanceService test', function(){

    it('should retrieve all created instances', function(){
        httpBackend.whenGET('/api/v1/instances').respond([created_instance]);
        InstanceService.getInstances().then(function(data) {
          expect(Restangular.stripRestangular(data)).toEqual([created_instance]);
        });
        httpBackend.flush();
      });

    it('should create instances', function(){
      httpBackend.whenPOST('/api/v1/instances').respond(created_instance);
      InstanceService.createInstance(template,params_posted).then(function(data) {
        expect(Restangular.stripRestangular(data)).toEqual(created_instance);
      });        
        httpBackend.flush();
      });

    it('should submit status', function(){
        httpBackend.whenPOST('/api/v1/instances/my-http').respond(created_instance);
        InstanceService.submitStatus(created_instance,params_posted).then(function(data) {
          expect(Restangular.stripRestangular(data)).toEqual(created_instance);
      });
         httpBackend.flush();
      });

    it('should edit Instance', function(){
        httpBackend.whenPOST('/api/v1/instances/my-http').respond(created_instance);
        InstanceService.editInstance(params_posted,created_instance ).then(function(data){
          expect(Restangular.stripRestangular(data)).toEqual(created_instance);
        });
        httpBackend.flush();
      });
    });
  });

