describe('Test Suite for AboutService', function() {

    var Restangular, httpBackend, AboutService;

    beforeEach(angular.mock.module('broccoli'));

    beforeEach(inject(function(_$httpBackend_, _Restangular_, _AboutService_) {
        httpBackend = _$httpBackend_;
        Restangular = _Restangular_;
        AboutService = _AboutService_;
        Restangular.setBaseUrl("/api/v1");
    }));

    afterEach(function() {
        httpBackend.verifyNoOutstandingExpectation();
        httpBackend.verifyNoOutstandingRequest();
    });

    describe('AboutService test', function() {

        it('getStatus', function() {
            var status = {
                "project": "Cluster Broccoli"
            };
            httpBackend.whenGET('/api/v1/about').respond([status]);
            AboutService.getStatus().then(function(data) {
                expect(Restangular.stripRestangular(data)).toEqual([status]);
            });
            httpBackend.flush();
        });
    });
});