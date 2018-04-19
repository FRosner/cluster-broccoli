import re
import json
import glob
import argparse
import os
from copy import deepcopy


def get_template_variables(template):
      variablePattern = re.compile("\\{\\{([A-Za-z][A-Za-z0-9\\-\\_\\_]*)\\}\\}")
      variables = set(variablePattern.findall(template))
      return variables


def update_parameter_infos(parameter_infos, template_variables, instance_path, param_type):
    updated_parameter_infos = deepcopy(parameter_infos)
    info_variables = set(parameter_infos.keys())
    for variable in info_variables.union(template_variables):
        if variable not in info_variables:
            print("Adding missing variable '%s' to the parameterInfos of instance %s" % (variable, instance_path))
            updated_parameter_infos[variable] = {"id": variable}
        if "type" not in updated_parameter_infos[variable]:
            print("Adding missing type for '%s' to the parameterInfos of instance %s" % (variable, instance_path))
            updated_parameter_infos[variable]["type"] = param_type
    return updated_parameter_infos


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("instance_directory")
    parser.add_argument("parameter_type")

    opts = parser.parse_args()

    for instance_path in glob.glob(os.path.join(opts.instance_directory, "*.json")):
        print("Processing %s" % instance_path)
        instance = json.loads(open(instance_path).read())

        assert "template" in instance
        template = instance["template"]
        assert "template" in template
        assert "parameterInfos" in template
        template_variables = get_template_variables(template["template"])
        parameter_infos = template["parameterInfos"]

        complete_parameter_infos = update_parameter_infos(parameter_infos, template_variables, instance_path, opts.parameter_type)

        parameter_infos_changed = False
        if parameter_infos != complete_parameter_infos:
            parameter_infos_changed = True
            template["parameterInfos"] = complete_parameter_infos

        if parameter_infos_changed:
            print("Overwritting instance: %s" % instance_path)
            open(instance_path, "w+").write(json.dumps(instance))
