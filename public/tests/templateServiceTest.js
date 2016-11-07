describe('Test Suite for TemplateService', function() {

    var Restangular, httpBackend, TemplateService;

    beforeEach(angular.mock.module('broccoli'));

    beforeEach(inject(function(_$httpBackend_, _Restangular_, _TemplateService_) {
        httpBackend = _$httpBackend_;
        Restangular = _Restangular_;
        TemplateService = _TemplateService_;
        Restangular.setBaseUrl("/api/v1");
    }));

    afterEach(function() {
        httpBackend.verifyNoOutstandingExpectation();
        httpBackend.verifyNoOutstandingRequest();
    });

    describe('TemplateService test', function() {
        it('Should Retrieve all Templates', inject(function(TemplateService) {
            var templates = [{
                "id": "http-server"
            }];
            httpBackend.whenGET('/api/v1/templates').respond(templates);
            TemplateService.getTemplates().then(function(data) {
                expect(Restangular.stripRestangular(data)).toEqual(templates);
            });
            httpBackend.flush();
        }));
    });
});