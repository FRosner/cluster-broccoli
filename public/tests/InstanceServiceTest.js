describe('Test Suite for InstanceService', function() {

    var Restangular, httpBackend, InstanceService;

    beforeEach(angular.mock.module('broccoli'));

    beforeEach(inject(function(_$httpBackend_, _Restangular_, _InstanceService_) {
        httpBackend = _$httpBackend_;
        Restangular = _Restangular_;
        InstanceService = _InstanceService_;
        Restangular.setBaseUrl("/api/v1");
    }));

    afterEach(function() {
        httpBackend.verifyNoOutstandingExpectation();
        httpBackend.verifyNoOutstandingRequest();
    });

    describe('InstanceService test', function() {

        it('should create instances', function() {
            var instance = {
                "id": "id"
            };
            var template = [{
                "id": "http-server"
            }];
            var params_posted = {
                "params": "params"
            };
            httpBackend.whenPOST('/api/v1/instances').respond(instance);
            InstanceService.createInstance(template, params_posted).then(function(data) {
                expect(Restangular.stripRestangular(data)).toEqual(instance);
            });
            httpBackend.flush();
        });

        it('should submit status', function() {
            var instance = {
                "id": "id"
            };
            var status = "running";
            httpBackend.whenPOST('/api/v1/instances/id').respond(instance);
            InstanceService.submitStatus(instance, status).then(function(data) {
                expect(Restangular.stripRestangular(data)).toEqual(instance);
            });
            httpBackend.flush();
        });

        it('should edit Instance', function() {
            var instance = {
                "id": "id"
            };
            var params_posted = {
                "params": "params"
            };
            httpBackend.whenPOST('/api/v1/instances/id').respond(instance);
            InstanceService.editInstance(params_posted, instance).then(function(data) {
                expect(Restangular.stripRestangular(data)).toEqual(instance);
            });
            httpBackend.flush();
        });
    });
});