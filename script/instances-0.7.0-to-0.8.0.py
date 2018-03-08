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


def update_parameter_infos(parameter_infos, template_variables, instance_path):
    updated_parameter_infos = deepcopy(parameter_infos)
    for variable in (template_variables - set(parameter_infos.keys())):
        print("Adding missing variable '%s' to the parameterInfos of instance %s" % (variable, instance_path))
        updated_parameter_infos[variable] = {"id": variable}
    return updated_parameter_infos


def convert_dashes_to_underscores(instance):
    result_instance = deepcopy(instance)

    # convert dashes in parameterValues
    assert "parameterValues" in instance
    result_parameter_values = result_instance["parameterValues"]
    for k, v in instance["parameterValues"].items():
        if "-" in k:
            new_k = k.replace("-", "_")
            print("Converting variable '%s' to %s in instance %s" % (k, new_k, instance_path))
            assert new_k not in result_instance
            del result_parameter_values[k]
            result_parameter_values[new_k] = v

    assert "template" in instance
    template = instance["template"]

    assert "template" in template
    assert "parameterInfos" in template

    # convert dashes in the template's parameterInfos and in the template text
    parameter_infos = template["parameterInfos"]
    result_parameter_infos = result_instance["template"]["parameterInfos"]
    for k, v in parameter_infos.items():
        if "-" in k:
            new_k = k.replace("-", "_")
            assert new_k not in result_parameter_infos
            new_v = result_parameter_infos.pop(k)
            new_v["id"] = new_k
            result_parameter_infos[new_k] = new_v
            result_instance["template"]["template"] = result_instance["template"]["template"].replace(
                    "{{" + k + "}}", "{{" + new_k + "}}"
            )

    return result_instance


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
        template_variables = get_template_variables(template["template"])
        parameter_infos = template["parameterInfos"]

        complete_parameter_infos = update_parameter_infos(parameter_infos, template_variables, instance_path)

        parameter_infos_changed = False
        if parameter_infos != complete_parameter_infos:
            parameter_infos_changed = True
            template["parameterInfos"] = complete_parameter_infos

        instance_without_dashes = convert_dashes_to_underscores(instance)

        parameter_values_changed = False
        if instance_without_dashes["parameterValues"] != instance["parameterValues"]:
            parameter_values_changed = True

        if instance_without_dashes["template"]["parameterInfos"] != instance["template"]["parameterInfos"]:
            parameter_infos_changed = True

        if parameter_infos_changed or parameter_values_changed:
            print("Overwritting instance: %s" % instance_path)
            open(instance_path, "w+").write(json.dumps(instance_without_dashes))
