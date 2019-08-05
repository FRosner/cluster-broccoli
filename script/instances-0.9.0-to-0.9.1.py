import json
import glob
import argparse
import os
from copy import deepcopy


def update_parameter_infos(parameter_infos):
    updated_parameters = deepcopy(parameter_infos)
    info_variables = set(parameter_infos.keys())
    for variable in info_variables:
        if type(updated_parameters[variable]['type']) == str:
            old_type = updated_parameters[variable]['type']
            updated_parameters[variable]['type'] = {"name": old_type}
        else:
            print("not changing type for " + variable)
    return updated_parameters


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("instance_directory")

    opts = parser.parse_args()

    for instance_path in glob.glob(os.path.join(opts.instance_directory, "*.json")):
        print("Processing %s" % instance_path)
        instance = json.loads(open(instance_path).read())

        assert "template" in instance
        template = instance["template"]
        assert "template" in template
        assert "parameterInfos" in template
        parameter_infos = template["parameterInfos"]

        complete_parameter_infos = update_parameter_infos(parameter_infos)

        parameter_infos_changed = False
        if parameter_infos != complete_parameter_infos:
            parameter_infos_changed = True
            template["parameterInfos"] = complete_parameter_infos

        if parameter_infos_changed:
            print("Overwritting instance: %s" % instance_path)
            open(instance_path, "w+").write(json.dumps(instance))
