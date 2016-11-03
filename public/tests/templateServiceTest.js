describe('Test Suite for TemplateService', function () {
  
  var Restangular, httpBackend, TemplateService;

  var mockResponse = [
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
          },
          {
            "id": "jupyter",
            "description": "Open source, interactive data science and scientific computing across over 40 programming languages.",
            "parameters": [
              "id"
            ],
            "parameterInfos": {},
            "version": "2c64126e09b72abd1a46f6db5b296221"
          },
          {
            "id": "zeppelin",
            "description": "A web-based notebook that enables interactive data analytics. You can make beautiful data-driven, interactive and collaborative documents with SQL, Scala and more.",
            "parameters": [
              "id"
            ],
            "parameterInfos": {},
            "version": "6f983b4ea4e12344e73f450fa9201243"
          }
        ];

  beforeEach(angular.mock.module('broccoli'));

  beforeEach(inject(function(_$httpBackend_, _Restangular_,_TemplateService_){
      httpBackend = _$httpBackend_;      
      Restangular = _Restangular_;
      TemplateService = _TemplateService_;   
      Restangular.setBaseUrl("/api/v1");    
    }));

  afterEach(function() {
    httpBackend.verifyNoOutstandingExpectation();
    httpBackend.verifyNoOutstandingRequest();
  });

  describe('TemplateService test', function(){

    it('Should Retrieve all Templates', inject(function(TemplateService){    

        httpBackend.whenGET('/api/v1/templates') .respond(mockResponse);

        TemplateService.getTemplates().then(function(data) {
            expect(Restangular.stripRestangular(data)).toEqual(mockResponse);
        });

        httpBackend.flush();
      }));
    });
  });