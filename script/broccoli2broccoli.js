#!/usr/bin/env node --harmony

var program = require('commander');
var co = require('co');
var prompt = require('co-prompt');
var request = require('superagent');

program
  .arguments('<from-broccoli>')
  .arguments('<to-broccoli>')
  .action(function(fromBroccoli, toBroccoli) {
    co(function *() {
      var yn = yield prompt('Migrating instances from ' + fromBroccoli + ' to ' + toBroccoli + '? [y/n]: ');
      if (yn == "y") {
        request
          .get(fromBroccoli + '/api/v1/instances')
          .end(function (err, res) {
            if (err) {
              console.error('Error retrieving the instances from %s: %s', fromBroccoli, err);
            } else {
              var instances = res.body;
              instances.forEach(function (instance){
                console.log('Migrating: %s', instance.id);
                var instanceCreation = {
                  "templateId": instance.template.id,
                  "parameters": instance.parameterValues
                };
                request
                  .post(toBroccoli + '/api/v1/instances')
                  .send(instanceCreation)
                  .end(function (err, res) {
                    if (err) {
                      console.error('Error putting the instance %s to %s: %s', instance.id, toBroccoli, err);
                    }
                  });
              });
            }
          });
      } else {
        console.log('Exiting.');
      }
      process.stdin.pause();
    }).catch(function(err) {
      console.error(err.stack);
    });
  })
  .parse(process.argv);
