describe('Test Suite for AboutService', function () {
  
  var Restangular, httpBackend, AboutService;  
  var mockResponse = {
    "project": {
      "name": "Cluster Broccoli",
      "version": "0.5.0"
    },
    "scala": {
      "version": "2.10.6"
    },
    "sbt": {
      "version": "0.13.11"
    },
    "permissions": {
      "mode": "operator"
    },
    "nomad": {
      "jobPrefix": "dev"
    }
  }

  beforeEach(angular.mock.module('broccoli'));

  beforeEach(inject(function(_$httpBackend_, _Restangular_,_AboutService_){
      httpBackend = _$httpBackend_;      
      Restangular = _Restangular_;
      AboutService = _AboutService_;   
      Restangular.setBaseUrl("/api/v1");    
    }));

  afterEach(function() {
    httpBackend.verifyNoOutstandingExpectation();
    httpBackend.verifyNoOutstandingRequest();
  });

  describe('AboutService test', function(){

    it('getStatus', function(){
        httpBackend.whenGET('/api/v1/about').respond([mockResponse]);
        AboutService.getStatus().then(function(data) {
          expect(Restangular.stripRestangular(data)).toEqual([mockResponse]);
        });
        httpBackend.flush();
      });
    });
  });

